package space.lianxin.image.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.*
import kotlin.math.roundToInt

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 19:37
 */
object ImageUtil {

    /**
     * 获取缓存图片的文件夹
     *
     * @param context
     * @return
     */
    fun getImageCacheDir(context: Context): String {
        var file: File? = null
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
            file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            } else {
                context.externalCacheDir
            }
        }
        if (file == null) {
            file = context.cacheDir
        }
        return "${file?.path}${File.separator}image_select"
    }

    /**
     * 是否是剪切返回的图片
     */
    fun isCutImage(dir: String, path: String?): Boolean {
        return if (!path.isNullOrEmpty()) {
            path.startsWith(dir)
        } else false
    }

    /**
     * 保存拍照的图片
     *
     * @param context
     * @param uri
     * @param takeTime 调起相机拍照的时间
     */
    fun savePicture(context: Context, uri: Uri?, takeTime: Long) {
        Thread {
            if (isNeedSavePicture(context, takeTime)) {
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            }
        }.start()
    }

    /**
     * 是否需要保存拍照的图片
     *
     * @param context
     * @return
     */
    @SuppressLint("Range")
    private fun isNeedSavePicture(context: Context, takeTime: Long): Boolean {
        //扫描图片
        val mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            .buildUpon().appendQueryParameter("limit", "1").build()
        val mContentResolver = context.contentResolver
        val mCursor = mContentResolver.query(
            mImageUri, arrayOf(
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.SIZE
            ),
            MediaStore.MediaColumns.SIZE + ">0",
            null,
            MediaStore.Files.FileColumns._ID + " DESC"
        )

        //读取扫描到的图片
        if (mCursor != null && mCursor.count > 0 && mCursor.moveToFirst()) {
            //获取图片时间
            var time = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
            mCursor.getInt(mCursor.getColumnIndex(MediaStore.Images.Media._ID))
            if (time.toString().length < 13) {
                time *= 1000
            }
            mCursor.close()

            // 如果照片的插入时间大于相机的拍照时间，就认为是拍照图片已插入
            return time + 1000 < takeTime
        }
        return true
    }

    fun zoomBitmap(bm: Bitmap, reqWidth: Int, reqHeight: Int): Bitmap {
        // 获得图片的宽高
        val width = bm.width
        val height = bm.height
        // 计算缩放比例
        val scaleWidth = reqWidth.toFloat() / width
        val scaleHeight = reqHeight.toFloat() / height
        val scale = scaleWidth.coerceAtMost(scaleHeight)
        // 取得想要缩放的matrix参数
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        // 得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true)
    }

    /**
     * 保存图片
     */
    fun saveImage(bitmap: Bitmap, path: String, name: String): String {
        var b: FileOutputStream? = null
        val file = File(path)
        if (!file.exists()) {
            // 创建文件夹
            file.mkdirs()
        }
        val fileName = path + File.separator + name + ".jpg"
        try {
            b = FileOutputStream(fileName)
            // 把数据写入文件
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, b)
            return fileName
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                if (b != null) {
                    b.flush()
                    b.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ""
    }

    /**
     * 根据计算的inSampleSize，得到压缩后图片
     */
    fun decodeSampledBitmapFromFile(
        context: Context,
        pathName: String?,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var degree = 0
        val uri = UriUtils.getImageContentUri(context, pathName)
        val parcelFileDescriptor: ParcelFileDescriptor?
        val fileDescriptor: FileDescriptor?
        try {
            parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor = parcelFileDescriptor?.fileDescriptor ?: return null
            val exif: ExifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ExifInterface(fileDescriptor)
            } else {
                ExifInterface(pathName.orEmpty())
            }
            val result = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when (result) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        try {
            // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
            } else {
                BitmapFactory.decodeFile(pathName, options)
            }
            // 调用上面定义的方法计算inSampleSize值
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 使用获取到的inSampleSize值再次解析图片
            options.inJustDecodeBounds = false
            //            options.inPreferredConfig = Bitmap.Config.RGB_565;
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getBitmapFromUri(context, uri, options)
            } else {
                BitmapFactory.decodeFile(pathName, options)
            }
            parcelFileDescriptor.close()
            if (degree != 0 && bitmap != null) {
                val newBitmap: Bitmap = rotateImageView(bitmap, degree)
                bitmap.recycle()
                return newBitmap
            }
            return bitmap
        } catch (error: OutOfMemoryError) {
            Log.e("eee", "内存 --> OutOfMemoryError !!！")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 计算inSampleSize，用于压缩图片
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int, reqHeight: Int
    ): Int {
        // 源图片的宽度
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        if (width > reqWidth && height > reqHeight) {
            // 计算出实际宽度和目标宽度的比率
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToInt()
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            inSampleSize = widthRatio.coerceAtLeast(heightRatio)
        }
        return inSampleSize
    }

    /**
     * 获取Bitmap
     */
    private fun getBitmapFromUri(
        context: Context,
        uri: Uri,
        options: BitmapFactory.Options?
    ): Bitmap? {
        try {
            val parcelFileDescriptor = context.contentResolver
                .openFileDescriptor(uri, "r")
            val fileDescriptor = parcelFileDescriptor?.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
            parcelFileDescriptor?.close()
            return image
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 旋转图片
     */
    private fun rotateImageView(bitmap: Bitmap, angle: Int): Bitmap {
        // 旋转图片 动作
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}