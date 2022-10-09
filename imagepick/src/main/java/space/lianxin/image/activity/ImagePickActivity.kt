package space.lianxin.image.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.EnvironmentCompat
import androidx.recyclerview.widget.*
import space.lianxin.image.ImagePick
import space.lianxin.image.R
import space.lianxin.image.adapter.FolderAdapter
import space.lianxin.image.adapter.ImageAdapter
import space.lianxin.image.databinding.ActivityImagePickBinding
import space.lianxin.image.entity.Folder
import space.lianxin.image.entity.Image
import space.lianxin.image.entity.RequestConfig
import space.lianxin.image.model.ImageModel
import space.lianxin.image.utils.ImageUtil
import space.lianxin.image.utils.UriUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 16:47
 */
class ImagePickActivity : AppCompatActivity() {

    companion object {

        /** 图片选择的配置类key */
        private const val KEY_CONFIG = "key_config"

        private const val CAMERA_REQUEST_CODE = 0x10
        private const val PERMISSION_WRITE_EXTERNAL_REQUEST_CODE = 0x11
        private const val PERMISSION_CAMERA_REQUEST_CODE = 0x12

        fun startActivity(activity: Activity, requestCode: Int, config: RequestConfig) {
            val intent = Intent(activity, ImagePickActivity::class.java)
            intent.putExtra(KEY_CONFIG, config)
            activity.startActivityForResult(intent, requestCode)
        }

    }

    // region 成员参数

    /** 视图ViewBinding */
    private lateinit var binding: ActivityImagePickBinding

    /** 图片选择配置参数 */
    private lateinit var config: RequestConfig

    /** 图片显示的适配器 */
    private lateinit var mAdapter: ImageAdapter

    private lateinit var mLayoutManager: GridLayoutManager

    /** 是否初始化了图片文件夹 */
    private var isInitFolder = false

    /** 图片文件夹状态是否打开 */
    private var isOpenFolder = false

    /** 文件夹集合 */
    private var mFolders: ArrayList<Folder>? = null

    /** 当前文件夹 */
    private var mFolder: Folder? = null

    /** 拍照 */
    private var mCameraImagePath: String? = null
    private var mCameraUri: Uri? = null
    private var mTakeTime: Long = 0

    private var isShowTime = false
    // endregion

    private val mHideHandler = Handler(Looper.getMainLooper())
    private val mHide = Runnable { hideTime() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 检查是否缺少配置类
        val temp = intent.getParcelableExtra<RequestConfig>(KEY_CONFIG)
        if (temp == null) {
            Toast.makeText(this, "缺少配置选择", Toast.LENGTH_SHORT).show()
            return finish()
        }
        config = temp
        // 检查是否是缺少仅拍照，
        if (config.onlyTakePhoto) {
            // 仅拍照
            checkPermissionAndCamera()
        } else {
            binding = ActivityImagePickBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setStatusBarColor()
            initImageList()
            initListener()
            checkPermissionAndLoadImages()
            hideFolderList()
            setSelectImageCount(0)
        }
    }

    /**
     * 刚开始的时候文件夹列表默认是隐藏的
     */
    private fun hideFolderList() {
        binding.rvFolder.post {
            binding.rvFolder.translationY = binding.rvFolder.height.toFloat()
            binding.rvFolder.visibility = View.GONE
            binding.rvFolder.setBackgroundColor(Color.WHITE)
        }
    }

    /**
     * 检查权限并加载SD卡里的图片。
     */
    private fun checkPermissionAndLoadImages() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            // 没有图片
            return
        }
        // 检查是否有读的权限
        val write = ContextCompat
            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (write == PackageManager.PERMISSION_GRANTED) {
            // 有权限，加载图片。
            loadImageForSdCard()
        } else {
            // 没有权限，申请权限。
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0x11
            )
        }
    }

    /**
     * 从SDCard加载图片。
     */
    private fun loadImageForSdCard() {
        ImageModel.loadImageForSdCard(this) { folders ->
            mFolders = folders
            runOnUiThread {
                if (!mFolders.isNullOrEmpty()) {
                    initFolderList()
                    mFolders?.get(0)?.useCamera = config.useCamera
                    setFolder(mFolders?.get(0))
                    if (config.selected != null) {
                        mAdapter.setSelectedImages(config.selected)
                        config.selected = null
                        setSelectImageCount(mAdapter.getSelectImages().size)
                    }
                }
            }
        }
    }

    /**
     * 初始化图片列表
     */
    private fun initImageList() {
        // 判断屏幕方向
        val configuration = resources.configuration
        mLayoutManager = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            GridLayoutManager(this, 3)
        } else {
            GridLayoutManager(this, 5)
        }
        binding.rvImage.layoutManager = mLayoutManager
        mAdapter = ImageAdapter(this, config.maxSelectCount, config.isSingle, config.canPreview)
        binding.rvImage.adapter = mAdapter
        (binding.rvImage.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        if (!mFolders.isNullOrEmpty()) {
            setFolder(mFolders?.get(0))
        }
        mAdapter.onImageSelect = { _, _, selectCount ->
            setSelectImageCount(selectCount)
        }
        mAdapter.setOnItemClickListener(object : ImageAdapter.OnItemClickListener {
            override fun onItemClick(image: Image, position: Int) {
                toPreviewActivity(mAdapter.getData(), position)
            }

            override fun onCameraClick() {
                checkPermissionAndCamera()
            }
        })
    }

    /**
     * 初始化图片文件夹列表
     */
    private fun initFolderList() {
        if (!mFolders.isNullOrEmpty()) {
            isInitFolder = true
            binding.rvFolder.layoutManager = LinearLayoutManager(this)
            val adapter = FolderAdapter(this, mFolders)
            adapter.onFolderSelect = { folder ->
                setFolder(folder)
                closeFolder()
            }
            binding.rvFolder.adapter = adapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSelectImageCount(count: Int) {
        if (count == 0) {
            binding.btnConfirm.isEnabled = false
            binding.btnPreview.isEnabled = false
            binding.tvConfirm.setText(R.string.selector_send)
            binding.tvPreview.setText(R.string.selector_preview)
        } else {
            binding.btnConfirm.isEnabled = true
            binding.btnPreview.isEnabled = true
            binding.tvPreview.text = getString(R.string.selector_preview) + "(" + count + ")"
            val sure = getString(R.string.selector_send)
            binding.tvConfirm.text = when {
                config.isSingle -> sure
                config.maxSelectCount > 0 -> "${sure}(${count}/${config.maxSelectCount})"
                else -> "${sure}(${count})"
            }
        }
    }

    /**
     * 设置选中的文件夹，同时刷新图片列表
     */
    private fun setFolder(folder: Folder?) {
        if (folder != null && folder != mFolder) {
            mFolder = folder
            binding.tvFolderName.text = folder.name
            binding.rvImage.scrollToPosition(0)
            mAdapter.refresh(folder.images, folder.useCamera)
        }
    }

    /**
     * 设置状态栏颜色
     */
    private fun setStatusBarColor() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = getColor(R.color.c_373c3d)
    }

    private fun initListener() {
        // 返回键
        binding.btnBack.setOnClickListener { onBackPressed() }
        // 预览
        binding.btnPreview.setOnClickListener {
            val images: ArrayList<Image> = ArrayList()
            images.addAll(mAdapter.getSelectImages())
            toPreviewActivity(images, 0)
        }
        // 确定
        binding.btnConfirm.setOnClickListener { confirm() }
        // 图片文件夹
        binding.btnFolder.setOnClickListener {
            if (isInitFolder) {
                if (isOpenFolder) {
                    closeFolder()
                } else {
                    openFolder()
                }
            }
        }
        binding.masking.setOnClickListener { closeFolder() }
        binding.rvImage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                changeTime()
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                changeTime()
            }
        })
    }

    /**
     * 改变时间条显示的时间（显示图片列表中的第一个可见图片的时间）
     */
    private fun changeTime() {
        val firstVisibleItem: Int = mLayoutManager.findFirstVisibleItemPosition()
        val image = mAdapter.getFirstVisibleImage(firstVisibleItem)
        if (image != null) {
            binding.tvTime.text = getImageDataFormat(image.time)
            showTime()
            mHideHandler.removeCallbacks(mHide)
            mHideHandler.postDelayed(mHide, 1500)
        }
    }

    /**
     * 显示时间条
     */
    private fun showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(binding.tvTime, "alpha", 0f, 1f).setDuration(300).start()
            isShowTime = true
        }
    }

    /**
     * 隐藏时间条
     */
    private fun hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(binding.tvTime, "alpha", 1f, 0f).setDuration(300).start()
            isShowTime = false
        }
    }

    /**
     * 格式化当前图片的时间
     */
    private fun getImageDataFormat(time: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM", Locale.CHINA)
        return sdf.format(Date(time))
    }

    /**
     * 收起文件夹列表
     */
    private fun closeFolder() {
        if (isOpenFolder) {
            binding.masking.visibility = View.GONE
            val animator = ObjectAnimator.ofFloat(
                binding.rvFolder,
                "translationY",
                0f,
                binding.rvFolder.height.toFloat()
            ).setDuration(300)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.rvFolder.visibility = View.GONE
                }
            })
            animator.start()
            isOpenFolder = false
        }
    }

    /**
     * 弹出文件夹列表
     */
    private fun openFolder() {
        if (!isOpenFolder) {
            binding.masking.visibility = View.VISIBLE
            val animator = ObjectAnimator.ofFloat(
                binding.rvFolder,
                "translationY",
                binding.rvFolder.height.toFloat(),
                0f
            ).setDuration(300)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    binding.rvFolder.visibility = View.VISIBLE
                }
            })
            animator.start()
            isOpenFolder = true
        }
    }

    /**
     * 去预览的界面
     */
    private fun toPreviewActivity(images: ArrayList<Image>?, position: Int) {
        if (images.isNullOrEmpty()) return
        PreviewActivity.startActivity(
            this,
            images,
            mAdapter.getSelectImages(),
            config.isSingle,
            config.maxSelectCount,
            position
        )
    }

    /**
     * 提交
     */
    private fun confirm() {
        // 因为图片的实体类是Image，而我们返回的是String数组，所以要进行转换。
        val selectImages = mAdapter.getSelectImages()
        val images = ArrayList<String>()
        for (image in selectImages) {
            image.path?.let { images.add(image.path) }
        }
        saveImageAndFinish(images, false)
    }

    private fun saveImageAndFinish(images: ArrayList<String>, isCameraImage: Boolean) {
        // 点击确定，把选中的图片通过Intent传给上一个Activity。
        setResult(images, isCameraImage)
        finish()
    }

    private fun setResult(images: ArrayList<String>, isCameraImage: Boolean) {
        val intent = Intent()
        intent.putStringArrayListExtra(ImagePick.PICK_RESULT, images)
        intent.putExtra(ImagePick.IS_CAMERA_IMAGE, isCameraImage)
        setResult(RESULT_OK, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 允许权限，加载图片。
                loadImageForSdCard()
            } else {
                // TODO: 拒绝权限，弹出提示框。
            }
        } else if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.size > 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                // 允许权限，有调起相机拍照。
                openCamera()
            } else {
                // TODO: 拒绝权限，弹出提示框。
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ImagePick.RESULT_CODE) {
            if (data != null && data.getBooleanExtra(ImagePick.IS_CONFIRM, false)) {
                // 如果用户在预览页点击了确定，就直接把用户选中的图片返回给用户。
                confirm()
            } else {
                // 否则，就刷新当前页面。
                mAdapter.notifyDataSetChanged()
                setSelectImageCount(mAdapter.getSelectImages().size)
            }
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val images = ArrayList<String>()
                var savePictureUri: Uri? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    savePictureUri = mCameraUri
                    images.add(UriUtils.getPathForUri(this, mCameraUri))
                } else {
                    mCameraImagePath?.let {
                        savePictureUri = Uri.fromFile(File(it))
                        images.add(it)
                    }
                }
                ImageUtil.savePicture(this, savePictureUri, mTakeTime)
                saveImageAndFinish(images, true)
            } else {
                if (config.onlyTakePhoto) {
                    finish()
                }
            }
        }
    }

    /**
     * 检查权限并拍照。
     */
    private fun checkPermissionAndCamera() {
        val hasCameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val hasWriteExternalPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED
            && hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED
        ) {
            //有调起相机拍照。
            openCamera()
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_CAMERA_REQUEST_CODE
            )
        }
    }

    /**
     * 调起相机拍照
     */
    private fun openCamera() {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            var photoUri: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                photoUri = createImagePathUri()
            } else {
                try {
                    photoFile = createImageFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (photoFile != null) {
                    mCameraImagePath = photoFile.absolutePath
                    photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // 通过FileProvider创建一个content类型的Uri
                        FileProvider.getUriForFile(
                            this,
                            "$packageName.imageSelectorProvider",
                            photoFile
                        )
                    } else {
                        Uri.fromFile(photoFile)
                    }
                }
            }
            mCameraUri = photoUri
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE)
                mTakeTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * 创建一条图片地址uri,用于保存拍照后的照片
     *
     * @return 图片的uri
     */
    private fun createImagePathUri(): Uri? {
        val status = Environment.getExternalStorageState()
        val timeFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val time = System.currentTimeMillis()
        val imageName = timeFormatter.format(Date(time))
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        val values = ContentValues(2)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        return if (status == Environment.MEDIA_MOUNTED) {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            contentResolver.insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)
        val storageDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) {
            storageDir.mkdir()
        }
        val tempFile = File(storageDir, imageFileName)
        return if (Environment.MEDIA_MOUNTED != EnvironmentCompat.getStorageState(tempFile)) {
            null
        } else tempFile
    }

}