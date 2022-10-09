package space.lianxin.image.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import space.lianxin.image.R
import space.lianxin.image.entity.Folder
import space.lianxin.image.entity.Image
import space.lianxin.image.utils.ImageUtil
import java.io.File

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 19:24
 */
object ImageModel {

    /**
     * 缓存图片
     */
    private var cacheImageList: ArrayList<Folder>? = null
    private var isNeedCache = false

    @SuppressLint("StaticFieldLeak")
    private var observer: PhotoContentObserver? = null

    /**
     * 预加载图片
     */
    fun preloadAndRegisterContentObserver(context: Context) {
        isNeedCache = true
        if (observer == null) {
            val temp = PhotoContentObserver(context.applicationContext)
            observer = temp
            observer = PhotoContentObserver(context.applicationContext)
            context.applicationContext.contentResolver
                .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, temp)
        }
        preload(context)
    }

    private fun preload(context: Context) {
        val write = ContextCompat
            .checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (write == PackageManager.PERMISSION_GRANTED) {
            // 有权限，加载图片。
            loadImageForSdCard(context, true, null)
        }
    }

    /**
     * 清空缓存
     */
    fun clearCache(context: Context) {
        isNeedCache = false
        observer?.let {
            context.applicationContext.contentResolver.unregisterContentObserver(it)
            observer = null
        }
        Thread {
            synchronized(ImageModel::class.java) {
                cacheImageList?.clear()
                cacheImageList = null
            }
        }.start()
    }

    /**
     * 从SDCard加载图片 (由于扫描图片是耗时的操作，所以要在子线程处理。)
     */
    fun loadImageForSdCard(
        context: Context,
        isPreload: Boolean = false,
        onSuccess: ((ArrayList<Folder>?) -> Unit)? = null
    ) {
        Thread {
            synchronized(ImageModel::class.java) {
                val imageCacheDir: String = ImageUtil.getImageCacheDir(context) ?: return@Thread
                val folders: ArrayList<Folder>?
                if (cacheImageList == null || isPreload) {
                    val imageList = loadImage(context)
                    imageList.sortWith { image, t1 ->
                        when {
                            image.time > t1.time -> 1
                            image.time < t1.time -> -1
                            else -> 0
                        }
                    }
                    val images = ArrayList<Image>()
                    for (image in imageList) {
                        // 过滤不存在或未下载完成的图片
                        val exists = "downloading" != getExtensionName(image.path)
                                && checkImgExists(image.path)
                        // 过滤剪切保存的图片；
                        val isCutImage: Boolean =
                            ImageUtil.isCutImage(imageCacheDir, image.path)
                        if (!isCutImage && exists) {
                            images.add(image)
                        }
                    }
                    images.reverse()
                    folders = splitFolder(context, images)
                    if (isNeedCache) {
                        cacheImageList = folders
                    }
                } else {
                    folders = cacheImageList
                }
                onSuccess?.invoke(folders)
            }
        }.start()
    }

    /**
     * 从SDCard加载图片
     */
    @SuppressLint("Range")
    @Synchronized
    private fun loadImage(context: Context): ArrayList<Image> {
        // 扫描图片
        val mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val mContentResolver = context.contentResolver
        val mCursor = mContentResolver.query(
            mImageUri, arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE
            ),
            MediaStore.MediaColumns.SIZE + ">0",
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )
        val images = ArrayList<Image>()
        // 读取扫描到的图片
        mCursor?.let {
            while (it.moveToNext()) {
                // 获取图片的路径
                val id = it.getLong(it.getColumnIndex(MediaStore.Images.Media._ID))
                val path = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
                // 获取图片名称
                val name = it.getString(it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                // 获取图片时间
                var time = it.getLong(it.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                if (time.toString().length < 13) time *= 1000
                // 获取图片类型
                val mimeType = it.getString(it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE))
                // 获取图片uri
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    .buildUpon()
                    .appendPath(id.toString())
                    .build()
                images.add(Image(path, time, name, mimeType, uri))
            }
            mCursor.close()
        }
        return images
    }

    /**
     * 获取文件扩展名
     */
    private fun getExtensionName(filename: String?): String {
        if (!filename.isNullOrEmpty()) {
            val dot = filename.lastIndexOf('.')
            if (dot > -1 && dot < filename.length - 1) {
                return filename.substring(dot + 1)
            }
        }
        return ""
    }

    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     */
    private fun checkImgExists(filePath: String?): Boolean {
        if (filePath.isNullOrEmpty()) return false
        return File(filePath).exists()
    }

    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     */
    private fun splitFolder(context: Context, images: ArrayList<Image>?): ArrayList<Folder> {
        val folders = ArrayList<Folder>()
        folders.add(Folder(false, context.getString(R.string.selector_all_image), images))
        if (images != null && images.isNotEmpty()) {
            val size = images.size
            for (i in 0 until size) {
                val path = images[i].path
                val name: String = getFolderName(path)
                if (name.isNotEmpty()) {
                    val folder: Folder = getFolder(name, folders)
                    folder.addImage(images[i])
                }
            }
        }
        return folders
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     */
    private fun getFolderName(path: String?): String {
        if (!path.isNullOrEmpty()) {
            val strings = path.split(File.separator).toTypedArray()
            if (strings.size >= 2) {
                return strings[strings.size - 2]
            }
        }
        return ""
    }

    private fun getFolder(name: String, folders: MutableList<Folder>): Folder {
        if (folders.isNotEmpty()) {
            val size = folders.size
            for (i in 0 until size) {
                val folder = folders[i]
                if (name == folder.name) {
                    return folder
                }
            }
        }
        val newFolder = Folder(false, name)
        folders.add(newFolder)
        return newFolder
    }

    class PhotoContentObserver constructor(
        private val context: Context
    ) : ContentObserver(null) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            preload(context)
        }

    }

}