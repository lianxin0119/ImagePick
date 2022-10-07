package space.lianxin.image.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import space.lianxin.image.R
import space.lianxin.image.entity.Folder

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 20:05
 */
@SuppressLint("NotifyDataSetChanged")
class FolderAdapter constructor(
    private val mContext: Context,
    private val mFolders: ArrayList<Folder>?
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    /** 文件夹被选中 */
    var onFolderSelect: ((Folder) -> Unit)? = null

    private val mInflater = LayoutInflater.from(mContext)
    private var mSelectItem = 0

    private val isAndroidQ = isAndroidQ()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.folder_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = mFolders?.get(position) ?: return
        val images = folder.images
        holder.tvFolderName.text = folder.name
        holder.ivSelect.visibility = if (mSelectItem == position) View.VISIBLE else View.GONE
        if (!images.isNullOrEmpty()) {
            holder.tvFolderSize.text = mContext.getString(R.string.selector_image_num, images.size)
            Glide.with(mContext)
                .load(if (isAndroidQ) images[0].uri else images[0].path)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(holder.ivImage)
        } else {
            holder.tvFolderSize.text = mContext.getString(R.string.selector_image_num, 0)
            holder.ivImage.setImageBitmap(null)
        }

        holder.itemView.setOnClickListener {
            mSelectItem = holder.bindingAdapterPosition
            notifyDataSetChanged()
            onFolderSelect?.invoke(folder)
        }
    }

    override fun getItemCount(): Int {
        return mFolders?.size ?: 0
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    private fun isAndroidQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        var ivSelect: ImageView = itemView.findViewById(R.id.iv_select)
        var tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        var tvFolderSize: TextView = itemView.findViewById(R.id.tv_folder_size)

    }

}