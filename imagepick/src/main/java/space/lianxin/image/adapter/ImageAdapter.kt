package space.lianxin.image.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import space.lianxin.image.R
import space.lianxin.image.entity.Image

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 17:26
 */
@SuppressLint("NotifyDataSetChanged")
class ImageAdapter constructor(
    private val context: Context,
    /** 图片的最大选择数量, [isSingle]为false有效 */
    private val maxCount: Int = 0,
    /** 是否单选 */
    private val isSingle: Boolean = false,
    /** 是否点击放大图片查看 */
    private val isViewImage: Boolean = true
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    /**
     * 当图片选中状态改变
     * image - 图片信息
     * isSelect - 选中状态，true is selected
     * selectCount - 选中数量
     */
    var onImageSelect: ((image: Image, isSelect: Boolean, selectCount: Int) -> Unit)? = null

    /** 布局加载器 */
    private val mInflater = LayoutInflater.from(context)

    /** 图片集合 */
    private var mImages: ArrayList<Image>? = null

    /** 已经选中的图片图片集合 */
    private val mSelectImages: ArrayList<Image> = ArrayList()

    /** 添加相机 */
    private var useCamera = false

    /** 是否是android 10(android 10获取图片不一样了) */
    private var isAndroidQ: Boolean? = null

    /** item的点击事件 */
    private var mItemClickListener: OnItemClickListener? = null

    companion object {

        /** item类型 相机 */
        private const val TYPE_CAMERA = 1

        /** item类型 图片 */
        private const val TYPE_IMAGE = 2

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = if (viewType == TYPE_IMAGE) {
            mInflater.inflate(R.layout.image_item_images, parent, false)
        } else {
            mInflater.inflate(R.layout.image_item_camera, parent, false)
        }
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_IMAGE -> {
                // 没有信息直接结束
                val image = getImage(position) ?: return
                val req = if (isAndroidQ()) {
                    // android 10使用uri获取图片
                    Glide.with(context).load(image.uri)
                } else {
                    Glide.with(context).load(image.path)
                }
                // 加载图片
                req.apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.ivImage!!)
                // 设置选中的样式
                setItemSelect(holder, mSelectImages.contains(image))
                // 是否是gif
                holder.ivGif?.visibility = if (image.isGif()) View.VISIBLE else View.GONE
                // 选中状态点击
                holder.ivSelectIcon?.setOnClickListener {
                    checkedImage(holder, image)
                }
                // 有预览就预览，没有预览就改变选中状态
                holder.itemView.setOnClickListener {
                    if (isViewImage) {
                        val pos = holder.bindingAdapterPosition
                        val p = if (useCamera) pos - 1 else pos
                        mItemClickListener?.onItemClick(image, p)
                    } else {
                        checkedImage(holder, image)
                    }
                }
            }
            TYPE_CAMERA -> {
                holder.itemView.setOnClickListener {
                    mItemClickListener?.onCameraClick()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (useCamera && position == 0) TYPE_CAMERA else TYPE_IMAGE
    }

    override fun getItemCount(): Int {
        val size = mImages?.size ?: 0
        return if (useCamera) size + 1 else size
    }

    /**
     * 设置回调
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mItemClickListener = listener
    }

    /**
     * 设置选中的Image
     */
    fun setSelectedImages(selected: List<String>?) {
        if (selected.isNullOrEmpty()) return
        if (mImages == null) {
            mImages = ArrayList()
        }
        // 循环添加选中的图片
        for (path in selected) {
            if (isOverMaxCount()) {
                // 超过最大限制
                return notifyDataSetChanged()
            }
            mImages?.forEach { image ->
                if (path == image.path) {
                    if (!mSelectImages.contains(image)) {
                        mSelectImages.add(image)
                    }
                    return@forEach
                }
            }
        }
        notifyDataSetChanged()
    }

    /**
     * 获取选中的Image
     */
    fun getSelectImages(): ArrayList<Image> {
        return mSelectImages
    }

    /**
     * 获取全部图片
     */
    fun getData(): ArrayList<Image>? {
        return mImages
    }

    /**
     * 获取第一个可见item图片信息
     */
    fun getFirstVisibleImage(firstVisibleItem: Int): Image? {
        return if (!mImages.isNullOrEmpty()) {
            if (useCamera) {
                mImages?.get(if (firstVisibleItem > 0) firstVisibleItem - 1 else 0)
            } else {
                mImages?.get(if (firstVisibleItem < 0) 0 else firstVisibleItem)
            }
        } else null
    }

    /** 刷新 */
    fun refresh(data: ArrayList<Image>?, useCamera: Boolean) {
        mImages = data
        this.useCamera = useCamera
        notifyDataSetChanged()
    }

    /**
     * 获取图片信息
     */
    private fun getImage(position: Int): Image? {
        if (mImages == null) return null
        return mImages?.get(if (useCamera) position - 1 else position)
    }

    /** 判断当前的android版本 */
    private fun isAndroidQ(): Boolean {
        isAndroidQ?.let {
            return it
        }
        val androidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        isAndroidQ = androidQ
        return androidQ
    }

    /**
     * 设置图片选中和未选中的效果
     */
    private fun setItemSelect(holder: ViewHolder, isSelect: Boolean) {
        if (isSelect) {
            holder.ivSelectIcon?.setImageResource(R.mipmap.icon_image_select)
            holder.ivMasking?.alpha = 0.5f
        } else {
            holder.ivSelectIcon?.setImageResource(R.mipmap.icon_image_un_select)
            holder.ivMasking?.alpha = 0.2f
        }
    }

    /**
     * 是否超过最大数量限制
     */
    private fun isOverMaxCount(): Boolean {
        return if (isSingle && mSelectImages.size == 1) {
            true
        } else {
            maxCount > 0 && mSelectImages.size == maxCount
        }
    }

    /**
     * 取消选中图片
     */
    private fun unSelectImage(image: Image) {
        mSelectImages.remove(image)
        onImageSelect?.invoke(image, false, mSelectImages.size)
    }

    /**
     * 选中图片
     */
    private fun selectImage(image: Image) {
        mSelectImages.add(image)
        onImageSelect?.invoke(image, true, mSelectImages.size)
    }

    /**
     * 清除之前的选状态
     */
    private fun clearImageSelect() {
        if (mImages != null && mSelectImages.size == 1) {
            val index = mImages?.indexOf(mSelectImages[0]) ?: -1
            mSelectImages.clear()
            if (index != -1) {
                notifyItemChanged(if (useCamera) index + 1 else index)
            }
        }
    }

    /**
     * 检查图片的选中状态，并进行切换
     */
    private fun checkedImage(holder: ViewHolder, image: Image) {
        if (mSelectImages.contains(image)) {
            // 如果图片已经选中，就取消选中
            unSelectImage(image)
            setItemSelect(holder, false)
        } else if (isSingle) {
            // 如果是单选，就先清空已经选中的图片，再选中当前图片
            clearImageSelect()
            selectImage(image)
            setItemSelect(holder, true)
        } else if (maxCount <= 0 || mSelectImages.size < maxCount) {
            // 如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectImage(image)
            setItemSelect(holder, true)
        }
    }

    class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {

        var ivImage: ImageView? = null
        var ivSelectIcon: ImageView? = null
        var ivMasking: ImageView? = null
        var ivGif: ImageView? = null

        init {
            if (viewType == TYPE_IMAGE) {
                ivImage = itemView.findViewById(R.id.iv_image)
                ivSelectIcon = itemView.findViewById(R.id.iv_select)
                ivMasking = itemView.findViewById(R.id.iv_masking)
                ivGif = itemView.findViewById(R.id.iv_gif)
            }
        }

    }

    /**
     * 每个item的点击事件
     */
    interface OnItemClickListener {
        /**
         * Image Item点击
         */
        fun onItemClick(image: Image, position: Int)

        /**
         * 相机 Item点击(必然是第0个)
         */
        fun onCameraClick()
    }

}