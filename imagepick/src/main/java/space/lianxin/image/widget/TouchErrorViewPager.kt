package space.lianxin.image.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * description : 继承ViewPager并在onInterceptTouchEvent捕捉异常。
 * 因为ViewPager嵌套PhotoView使用，有时候会发生IllegalArgumentException异常。
 *
 * Create by LianXin on 2022/10/7 21:02
 */
class TouchErrorViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

}