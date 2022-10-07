package space.lianxin.image.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import space.lianxin.image.ImagePick
import space.lianxin.image.R
import space.lianxin.image.adapter.ImagePagerAdapter
import space.lianxin.image.databinding.ActivityPreviewBinding
import space.lianxin.image.entity.Image

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 18:12
 */
class PreviewActivity : AppCompatActivity() {

    companion object {

        // tempImages和tempSelectImages用于图片列表数据的页面传输。
        // 之所以不要Intent传输这两个图片列表
        // 因为要保证两位页面操作的是同一个列表数据，同时可以避免数据量大时，
        // 用Intent传输发生的错误问题。

        private var tempImages: ArrayList<Image>? = null
        private var tempSelectImages: ArrayList<Image>? = null

        /** 最大的图片选择数 */
        const val MAX_SELECT_COUNT = "max_select_count"

        /** 是否单选 */
        const val IS_SINGLE = "is_single"

        /** 初始位置 */
        const val POSITION = "position"

        fun startActivity(
            activity: Activity,
            images: ArrayList<Image>,
            selectImages: ArrayList<Image>,
            isSingle: Boolean,
            maxSelectCount: Int,
            position: Int
        ) {
            tempImages = images
            tempSelectImages = selectImages
            val intent = Intent(activity, PreviewActivity::class.java)
            intent.putExtra(MAX_SELECT_COUNT, maxSelectCount)
            intent.putExtra(IS_SINGLE, isSingle)
            intent.putExtra(POSITION, position)
            activity.startActivityForResult(intent, ImagePick.RESULT_CODE)
        }

    }

    /** 视图ViewBinding */
    private lateinit var binding: ActivityPreviewBinding

    private lateinit var mImages: ArrayList<Image>
    private lateinit var mSelectImages: ArrayList<Image>
    private var isShowBar = true
    private var isConfirm = false
    private var isSingle = false
    private var mMaxCount = 0

    private var mSelectDrawable: BitmapDrawable? = null
    private var mUnSelectDrawable: BitmapDrawable? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 设置页面全屏显示
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            // 设置页面延伸到刘海区显示
            window.attributes = lp
        }

        mImages = tempImages ?: ArrayList()
        tempImages = null
        mSelectImages = tempSelectImages ?: ArrayList()
        tempSelectImages = null

        val intent = intent
        mMaxCount = intent.getIntExtra(MAX_SELECT_COUNT, 0)
        isSingle = intent.getBooleanExtra(IS_SINGLE, false)

        val resources = resources
        val selectBitmap = BitmapFactory.decodeResource(resources, R.mipmap.icon_image_select)
        mSelectDrawable = BitmapDrawable(resources, selectBitmap)
        mSelectDrawable?.setBounds(0, 0, selectBitmap.width, selectBitmap.height)

        val unSelectBitmap = BitmapFactory.decodeResource(resources, R.mipmap.icon_image_un_select)
        mUnSelectDrawable = BitmapDrawable(resources, unSelectBitmap)
        mUnSelectDrawable?.setBounds(0, 0, unSelectBitmap.width, unSelectBitmap.height)

        setStatusBarColor()

        val lp = binding.rlTopBar.layoutParams as RelativeLayout.LayoutParams
        lp.topMargin = getStatusBarHeight()
        binding.rlTopBar.layoutParams = lp

        initListener()
        initViewPager()

        binding.tvIndicator.text = "1/${mImages.size}"
        changeSelect(mImages[0])
        binding.vpImage.currentItem = intent.getIntExtra(POSITION, 0)

    }

    /**
     * 修改状态栏颜色
     */
    private fun setStatusBarColor() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#373c3d")
    }

    private fun initListener() {
        binding.btnBack.setOnClickListener { onBackPressed() }
        binding.btnConfirm.setOnClickListener {
            isConfirm = true
            finish()
        }
        binding.tvSelect.setOnClickListener { clickSelect() }
    }

    private fun clickSelect() {
        val position: Int = binding.vpImage.currentItem
        if (mImages.size > position) {
            val image = mImages[position]
            if (mSelectImages.contains(image)) {
                mSelectImages.remove(image)
            } else if (isSingle) {
                mSelectImages.clear()
                mSelectImages.add(image)
            } else if (mMaxCount <= 0 || mSelectImages.size < mMaxCount) {
                mSelectImages.add(image)
            }
            changeSelect(image)
        }
    }

    /**
     * 初始化ViewPager
     */
    private fun initViewPager() {
        val adapter = ImagePagerAdapter(this, mImages)
        binding.vpImage.adapter = adapter
        adapter.onItemClick = { _, _ ->
            if (isShowBar) {
                hideBar()
            } else {
                showBar()
            }
        }
        binding.vpImage.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                binding.tvIndicator.text = "${position + 1}/${mImages.size}"
                changeSelect(mImages[position])
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    /**
     * 显示头部和尾部栏
     */
    private fun showBar() {
        isShowBar = true
        // 添加延时，保证StatusBar完全显示后再进行动画。
        binding.rlTopBar.postDelayed({
            val animator = ObjectAnimator.ofFloat(
                binding.rlTopBar,
                "translationY",
                binding.rlTopBar.translationY,
                0f
            ).setDuration(300)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    binding.rlTopBar.visibility = View.VISIBLE
                }
            })
            animator.start()
            ObjectAnimator.ofFloat(
                binding.rlBottomBar,
                "translationY",
                binding.rlBottomBar.translationY,
                0f
            ).setDuration(300).start()
        }, 100)
    }

    /**
     * 隐藏头部和尾部栏
     */
    private fun hideBar() {
        isShowBar = false
        val animator = ObjectAnimator.ofFloat(
            binding.rlTopBar,
            "translationY",
            0f,
            -binding.rlTopBar.height.toFloat()
        ).setDuration(300)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.rlTopBar.visibility = View.GONE
                // 添加延时，保证rlTopBar完全隐藏后再隐藏StatusBar。
                binding.rlTopBar.postDelayed({ }, 5)
            }
        })
        animator.start()
        ObjectAnimator.ofFloat(
            binding.rlBottomBar,
            "translationY",
            0f,
            binding.rlBottomBar.height.toFloat()
        ).setDuration(300).start()
    }

    private fun changeSelect(image: Image) {
        binding.tvSelect.setCompoundDrawables(
            if (mSelectImages.contains(image)) mSelectDrawable else mUnSelectDrawable,
            null,
            null,
            null
        )
        setSelectImageCount(mSelectImages.size)
    }

    private fun setSelectImageCount(count: Int) {
        if (count == 0) {
            binding.btnConfirm.isEnabled = false
            binding.tvConfirm.setText(R.string.selector_send)
        } else {
            binding.btnConfirm.isEnabled = true
            val sure = getString(R.string.selector_send)
            binding.tvConfirm.text = when {
                isSingle -> sure
                mMaxCount > 0 -> "${sure}(${count}/${mMaxCount})"
                else -> "${sure}(${count})"
            }
        }
    }

    override fun finish() {
        super.finish()
        // Activity关闭时，通过Intent把用户的操作(确定/返回)传给ImageSelectActivity。
        val intent = Intent()
        intent.putExtra(ImagePick.IS_CONFIRM, isConfirm)
        setResult(ImagePick.RESULT_CODE, intent)
        super.finish()
    }

    /**
     * 获取状态栏高度
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

}