package space.lianxin.image.entity

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/7 19:06
 */
data class Folder @JvmOverloads constructor(
    /** 是否可以调用相机拍照。只有“全部”文件夹才可以拍照 */
    var useCamera: Boolean = false,
    val name: String? = null,
    var images: ArrayList<Image>? = null
) {

    fun addImage(image: Image?) {
        if (image != null && !image.path.isNullOrBlank()) {
            if (images == null) {
                images = ArrayList()
            }
            images?.add(image)
        }
    }

}