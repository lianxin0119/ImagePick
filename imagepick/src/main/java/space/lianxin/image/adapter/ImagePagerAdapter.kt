package space.lianxin.image.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import space.lianxin.image.entity.Image
import space.lianxin.image.utils.ImageUtil

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 21:28
 */
class ImagePagerAdapter(
    private val mContext: Context,
    private val mImgList: List<Image>
) : PagerAdapter() {

    var onItemClick: ((position: Int, image: Image) -> Unit)? = null

    private val viewList: ArrayList<PhotoView> = ArrayList(4)

    init {
        createImageViews()
    }

    private fun createImageViews() {
        for (i in 0..3) {
            val imageView = PhotoView(mContext)
            imageView.adjustViewBounds = true
            viewList.add(imageView)
        }
    }

    override fun getCount(): Int {
        return mImgList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        if (`object` is PhotoView) {
            `object`.setImageDrawable(null)
            viewList.add(`object`)
            container.removeView(`object`)
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val currentView = viewList.removeAt(0)
        val image = mImgList[position]
        container.addView(currentView)
        if (image.isGif()) {
            currentView.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(mContext)
                .load(if (isAndroidQ()) image.uri else image.path)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .override(720, 1080)
                .into(currentView)
        } else {
            Glide.with(mContext).asBitmap()
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .load(if (isAndroidQ()) image.uri else image.path)
                .into(object : SimpleTarget<Bitmap?>(720, 1080) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        val bw = resource.width
                        val bh = resource.height
                        if (bw > 4096 || bh > 4096) {
                            val bitmap: Bitmap = ImageUtil.zoomBitmap(resource, 4096, 4096)
                            setBitmap(currentView, bitmap)
                        } else {
                            setBitmap(currentView, resource)
                        }
                    }
                })
        }
        currentView.setOnClickListener {
            onItemClick?.invoke(position, image)
        }
        return currentView
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    private fun isAndroidQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun setBitmap(imageView: PhotoView, bitmap: Bitmap?) {
        imageView.setImageBitmap(bitmap)
        if (bitmap != null) {
            val bw = bitmap.width
            val bh = bitmap.height
            val vw = imageView.width
            val vh = imageView.height
            if (bw != 0 && bh != 0 && vw != 0 && vh != 0) {
                if (1.0f * bh / bw > 1.0f * vh / vw) {
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    val offset = (1.0f * bh * vw / bw - vh) / 2
                    adjustOffset(imageView, offset)
                } else {
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }
        }
    }

    private fun adjustOffset(view: PhotoView, offset: Float) {
        val cher = view.attacher
        try {
            val field = PhotoViewAttacher::class.java.getDeclaredField("mBaseMatrix")
            field.isAccessible = true
            val matrix = field[cher] as Matrix
            matrix.postTranslate(0f, offset)
            val method = PhotoViewAttacher::class.java.getDeclaredMethod("resetMatrix")
            method.isAccessible = true
            method.invoke(cher)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}