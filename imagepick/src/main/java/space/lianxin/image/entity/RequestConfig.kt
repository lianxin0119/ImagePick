package space.lianxin.image.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 16:16
 */
@Parcelize
data class RequestConfig @JvmOverloads constructor(
    /** 是否要剪切 */
    var isCrop: Boolean = false,
    /** 是否要支持拍照 */
    var useCamera: Boolean = true,
    /** 仅拍照,不打开相册。true时, [useCamera]不生效 */
    var onlyTakePhoto: Boolean = false,
    /** 是否单选 */
    var isSingle: Boolean = false,
    /** 是否可以点击图片预览 */
    var canPreview: Boolean = true,
    /** 图片的最大选择数量，小于等于0时，不限数量，[isSingle]为false时生效 */
    var maxSelectCount: Int = 0,
    /** 接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，重新打开选择器，允许把先前选过的图片传进来，并把这些图片默认为选中状态 */
    var selected: ArrayList<String>? = null,
    /** 图片剪切的宽高比，宽固定为手机屏幕的宽。 */
    var cropRatio: Float = 1f
) : Parcelable