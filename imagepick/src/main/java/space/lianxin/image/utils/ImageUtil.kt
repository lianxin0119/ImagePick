package space.lianxin.image.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

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

}