package space.lianxin.image.entity

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 17:51
 */
@Parcelize
data class Image @JvmOverloads constructor(
    val path: String? = null,
    val time: Long = 0,
    val name: String? = null,
    val mimeType: String? = null,
    val uri: Uri
) : Parcelable {

    fun isGif(): Boolean = "image/gif" == mimeType

}