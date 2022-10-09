package space.lianxin.image

import android.app.Activity
import space.lianxin.image.activity.ClipImageActivity
import space.lianxin.image.activity.ImagePickActivity
import space.lianxin.image.entity.RequestConfig

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/6 16:11
 */
class ImagePick {

    companion object {

        /** 图片选择的结果 */
        const val PICK_RESULT = "pick_result"

        /**
         * 是否是来自于相机拍照的图片，
         * 只有本次调用相机拍出来的照片，返回时才为true。
         * 当为true时，图片返回当结果有且只有一张图片。
         */
        const val IS_CAMERA_IMAGE = "is_camera_image"

        const val IS_CONFIRM = "is_confirm"
        const val RESULT_CODE = 0x12

        fun build(): Builder {
            return Builder()
        }
    }

    class Builder internal constructor() {

        /**
         * 图片选择配置文件
         */
        private val config: RequestConfig = RequestConfig()

        /**
         * 是否使用图片剪切功能。默认false。如果使用了图片剪切功能，相册只能单选。
         */
        fun setCrop(isCrop: Boolean): Builder {
            config.isCrop = isCrop
            return this
        }

        /**
         * 是否使用拍照功能。
         */
        fun useCamera(useCamera: Boolean): Builder {
            config.useCamera = useCamera
            return this
        }

        /**
         * 仅拍照, 不打开相册。为true时[useCamera]不生效
         */
        fun onlyTakePhoto(onlyTakePhoto: Boolean): Builder {
            config.onlyTakePhoto = onlyTakePhoto
            return this
        }

        /**
         * 是否单选。为true时[setMaxSelectCount]无效
         */
        fun setSingle(isSingle: Boolean): Builder {
            config.isSingle = isSingle
            return this
        }

        /**
         * 是否可以点击预览，默认为true
         */
        fun canPreview(canPreview: Boolean): Builder {
            config.canPreview = canPreview
            return this
        }

        /**
         * 图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
         */
        fun setMaxSelectCount(maxSelectCount: Int): Builder {
            config.maxSelectCount = maxSelectCount
            return this
        }

        /**
         * 接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开
         * 选择器，允许用户把先前选过的图片传进来，并把这些图片默认为选中状态。
         */
        fun setSelected(selected: ArrayList<String>?): Builder {
            config.selected = selected
            return this
        }

        /**
         * 图片剪切的宽高比，宽固定为手机屏幕的宽。默认为1:1
         */
        fun setCropRatio(ratio: Float): Builder {
            config.cropRatio = ratio
            return this
        }

        /**
         * 设置完成，打开相册
         *
         * @param activity 启动选择界面，会通过[Activity.onActivityResult]把选择结果返回
         * @param requestCode 请求code
         */
        fun start(activity: Activity, requestCode: Int) {
            // 仅拍照，useCamera必须为true
            if (config.onlyTakePhoto) {
                config.useCamera = true
            }
            if (config.isCrop) {
                ClipImageActivity.startActivity(activity, requestCode, config)
            } else {
                ImagePickActivity.startActivity(activity, requestCode, config)
            }
        }

    }

}