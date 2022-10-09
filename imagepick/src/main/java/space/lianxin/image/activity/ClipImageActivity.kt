package space.lianxin.image.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import space.lianxin.image.ImagePick
import space.lianxin.image.R
import space.lianxin.image.databinding.ActivityClipImageBinding
import space.lianxin.image.entity.RequestConfig
import space.lianxin.image.utils.ImageUtil
import java.util.*

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/8 20:58
 */
class ClipImageActivity : AppCompatActivity() {

    companion object {

        /** 图片选择的配置类key */
        private const val KEY_CONFIG = "key_config"

        /** requestCode Key */
        private const val KEY_CODE = "key_code"

        fun startActivity(activity: Activity, requestCode: Int, config: RequestConfig) {
            val intent = Intent(activity, ClipImageActivity::class.java)
            intent.putExtra(KEY_CONFIG, config)
            intent.putExtra(KEY_CODE, requestCode)
            activity.startActivityForResult(intent, requestCode)
        }

    }

    /** 视图ViewBinding */
    private lateinit var binding: ActivityClipImageBinding

    private var mRequestCode = 0
    private var isCameraImage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClipImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        val config: RequestConfig = intent.getParcelableExtra(KEY_CONFIG) ?: return finish()
        mRequestCode = intent.getIntExtra(KEY_CODE, 0)
        config.isSingle = true
        config.maxSelectCount = 0
        setStatusBarColor()
        ImagePickActivity.startActivity(this, mRequestCode, config)

        binding.btnConfirm.setOnClickListener {
            if (binding.processImg.drawable != null) {
                binding.btnConfirm.isEnabled = false
                confirm(binding.processImg.clipImage())
            }
        }
        binding.btnBack.setOnClickListener { onBackPressed() }
        binding.processImg.setRatio(config.cropRatio)
    }

    private fun confirm(bitmap: Bitmap) {
        val bitmap1: Bitmap = bitmap
        val name = DateFormat.format(
            "yyyyMMdd_hhmmss",
            Calendar.getInstance(Locale.getDefault())
        ).toString()
        val path: String = ImageUtil.getImageCacheDir(this)
        val imagePath = ImageUtil.saveImage(bitmap1, path, name)
        bitmap1.recycle()
        if (imagePath.isNotEmpty()) {
            val selectImages = ArrayList<String?>()
            selectImages.add(imagePath)
            val intent = Intent()
            intent.putStringArrayListExtra(ImagePick.PICK_RESULT, selectImages)
            intent.putExtra(ImagePick.IS_CAMERA_IMAGE, isCameraImage)
            setResult(RESULT_OK, intent)
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && requestCode == mRequestCode) {
            val images = data.getStringArrayListExtra(ImagePick.PICK_RESULT)
            isCameraImage = data.getBooleanExtra(ImagePick.IS_CAMERA_IMAGE, false)
            val bitmap = ImageUtil.decodeSampledBitmapFromFile(this, images?.get(0), 720, 1080)
            if (bitmap != null) {
                binding.processImg.setBitmapData(bitmap)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * 修改状态栏颜色
     */
    private fun setStatusBarColor() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.c_373c3d)
    }

}