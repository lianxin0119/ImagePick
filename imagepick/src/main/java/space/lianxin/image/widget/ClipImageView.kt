package space.lianxin.image.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.sqrt

/**
 * description : This person is too lazy to leave anything.
 *
 * Create by LianXin on 2022/10/8 21:01
 */
class ClipImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    def: Int = 0
) : AppCompatImageView(context, attrs, def) {

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_DRAG = 1
        private const val MODE_ZOOM = 2
        private const val MODE_POINTER_UP = 3
    }

    private var mDownPoint: PointF? = null
    private var mMiddlePoint: PointF? = null
    private var mMatrix: Matrix? = null
    private var mTempMatrix: Matrix? = null

    private var mBitmapWidth = 0
    private var mBitmapHeight = 0

    private var mLastDistance = 0f

    private var currMode = MODE_NONE

    private val mFrontGroundPaint = Paint()
    private var mTargetWidth = 0
    private var mTargetHeight = 0
    private var xFerMode: Xfermode? = null

    //    private var r: Rect = Rect(0, 0, 0, 0)
//    private var rf: RectF = RectF(r)
    private var r: Rect? = null
    private var rf: RectF? = null

    private var mCircleCenterX = 0f
    private var mCircleCenterY: Float = 0f
    private var mCircleX = 0f
    private var mCircleY: Float = 0f
    private var isCutImage = false
    private var mRatio = 1.0f

    init {
        setRadius()
    }

    private fun setRadius() {
        mTargetWidth = getScreenWidth(context)
        mTargetHeight = (mTargetWidth * mRatio).toInt()
        mCircleCenterX = (width / 2).toFloat()
        mCircleCenterY = (height / 2).toFloat()
        mCircleX = mCircleCenterX - mTargetWidth / 2
        mCircleY = mCircleCenterY - mTargetHeight / 2
    }

    /**
     * 设置要剪裁的图片
     */
    fun setBitmapData(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        mBitmapHeight = bitmap.height
        mBitmapWidth = bitmap.width
        setImageBitmap(bitmap)
        init()
    }

    /**
     * 截取Bitmap
     *
     * @return
     */
    fun clipImage(): Bitmap {
        isCutImage = true
        val paint = Paint()
        isDrawingCacheEnabled = true
        var bitmap = drawingCache
        val targetBitmap = Bitmap.createBitmap(
            mTargetWidth, mTargetHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(targetBitmap)
        val left = -bitmap!!.width / 2 + mTargetWidth / 2
        val top = -height / 2 + mTargetHeight / 2
        val right = bitmap.width / 2 + mTargetWidth / 2
        val bottom = height / 2 + mTargetHeight / 2
        val dst = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        canvas.drawBitmap(bitmap, null, dst, paint)
        isDrawingCacheEnabled = false
        bitmap.recycle()
        bitmap = null
        isCutImage = false
        // 返回方形图片
        return targetBitmap
    }

    fun setRatio(ratio: Float) {
        if (mRatio != ratio) {
            mRatio = ratio
            setRadius()
            invalidate()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        setRadius()
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        if (isCutImage) {
//            return
//        }
//        if (rf.isEmpty) {
//            r.set(0, 0, width, height)
//            rf.set(r)
//        }
//        // 画入前景圆形蒙板层
//        val sc = canvas.saveLayer(rf, null, Canvas.ALL_SAVE_FLAG)
//        // 画入矩形黑色半透明蒙板层
//        canvas.drawRect(r, mFrontGroundPaint)
//        // 设置Xfermode，目的是为了去除矩形黑色半透明蒙板层和圆形的相交部分
//        mFrontGroundPaint.xfermode = xFerMode
//        // 画入正方形
//        val left = mCircleCenterX - mTargetWidth / 2
//        val top = mCircleCenterY - mTargetHeight / 2
//        val right = mCircleCenterX + mTargetWidth / 2
//        val bottom = mCircleCenterY + mTargetHeight / 2
//        canvas.drawRect(left, top, right, bottom, mFrontGroundPaint)
//
//        canvas.restoreToCount(sc)
//        // 清除Xfermode，防止影响下次画图
//        mFrontGroundPaint.xfermode = null
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isCutImage) {
            return
        }
        if (rf == null || rf?.isEmpty == true) {
            r = Rect(0, 0, width, height)
            rf = RectF(r)
        }
        // 画入前景圆形蒙板层
        val sc = canvas.saveLayer(rf, null, Canvas.ALL_SAVE_FLAG)
        //画入矩形黑色半透明蒙板层
        canvas.drawRect(r!!, mFrontGroundPaint)
        //设置Xfermode，目的是为了去除矩形黑色半透明蒙板层和圆形的相交部分
        mFrontGroundPaint.xfermode = xFerMode
        //画入正方形
        //画入正方形
        val left = mCircleCenterX - mTargetWidth / 2
        val top = mCircleCenterY - mTargetHeight / 2
        val right = mCircleCenterX + mTargetWidth / 2
        val bottom = mCircleCenterY + mTargetHeight / 2
        canvas.drawRect(left, top, right, bottom, mFrontGroundPaint)

        canvas.restoreToCount(sc)
        //清除Xfermode，防止影响下次画图
        mFrontGroundPaint.xfermode = null
    }

    private fun init() {
        mDownPoint = PointF()
        mMiddlePoint = PointF()
        mMatrix = Matrix()
        mTempMatrix = Matrix()
        mFrontGroundPaint.color = Color.parseColor("#ac000000")
        mFrontGroundPaint.isAntiAlias = true
        xFerMode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        scaleType = ScaleType.MATRIX
        post { center() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mMatrix == null) return super.onTouchEvent(event)

        val values = FloatArray(9)
        mMatrix?.getValues(values)
        val left = values[Matrix.MTRANS_X]
        val top = values[Matrix.MTRANS_Y]
        val right = left + mBitmapWidth * values[Matrix.MSCALE_X]
        val bottom = top + mBitmapHeight * values[Matrix.MSCALE_Y]

        var x: Float
        var y: Float
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                currMode = MODE_DRAG
                mDownPoint!![event.x] = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (getDistance(event) > 10f) {
                    currMode = MODE_ZOOM
                    midPoint(mMiddlePoint!!, event)
                    mLastDistance = getDistance(event)
                }
            }
            // 如果当前模式为拖曳（单指触屏）
            MotionEvent.ACTION_MOVE -> {
                if (currMode == MODE_DRAG) {
                    x = event.x - mDownPoint!!.x
                    y = event.y - mDownPoint!!.y
                    //left靠边
                    if (x + left > mCircleX) {
                        x = 0f
                    }
                    //right靠边
                    if (x + right < mCircleX + mTargetWidth) {
                        x = 0f
                    }
                    //top靠边
                    if (y + top > mCircleY) {
                        y = 0f
                    }
                    //bottom靠边
                    if (y + bottom < mCircleY + mTargetHeight) {
                        y = 0f
                    }
                    mMatrix!!.postTranslate(x, y)
                    mDownPoint!![event.x] = event.y
                } else if (currMode == MODE_POINTER_UP) {
                    currMode = MODE_DRAG
                    mDownPoint!![event.x] = event.y
                } else {
                    // 否则当前模式为缩放（双指触屏）
                    val distance: Float = getDistance(event)
                    if (distance > 10f) {
                        val scale = distance / mLastDistance

                        //left靠边
                        if (left >= mCircleX) {
                            mMiddlePoint!!.x = 0f
                        }
                        //right靠边
                        if (right <= mCircleX + mTargetWidth) {
                            mMiddlePoint!!.x = right
                        }
                        //top靠边
                        if (top >= mCircleY) {
                            mMiddlePoint!!.y = 0f
                        }
                        //bottom靠边
                        if (bottom <= mCircleY + mTargetHeight) {
                            mMiddlePoint!!.y = bottom
                        }
                        mTempMatrix!!.set(mMatrix)
                        mTempMatrix!!.postScale(scale, scale, mMiddlePoint!!.x, mMiddlePoint!!.y)
                        val tempValues = FloatArray(9)
                        mTempMatrix!!.getValues(tempValues)
                        val tempLeft = tempValues[Matrix.MTRANS_X]
                        val tempTop = tempValues[Matrix.MTRANS_Y]
                        val tempRight = tempLeft + mBitmapWidth * tempValues[Matrix.MSCALE_X]
                        val tempBottom = tempTop + mBitmapHeight * tempValues[Matrix.MSCALE_Y]
                        // 靠边预判断
                        if (tempLeft > mCircleX || tempRight < mCircleX + mTargetWidth || tempTop > mCircleY || tempBottom < mCircleY + mTargetHeight) {
                            return true
                        }
                        mMatrix!!.postScale(scale, scale, mMiddlePoint!!.x, mMiddlePoint!!.y)
                        mLastDistance = getDistance(event)
                    }
                }
            }
            MotionEvent.ACTION_UP -> currMode = MODE_NONE
            MotionEvent.ACTION_POINTER_UP -> currMode = MODE_POINTER_UP
        }
        imageMatrix = mMatrix
        return true
    }

    /**
     * 两点的距离
     */
    private fun getDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    /**
     * 两点的中点
     */
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    /**
     * 横向、纵向居中
     */
    private fun center() {
        val height = mBitmapHeight.toFloat()
        val width = mBitmapWidth.toFloat()
        val scale = (mTargetWidth / width).coerceAtLeast(mTargetHeight / height)
        val deltaX = -(width * scale - getWidth()) / 2.0f
        val deltaY = -(height * scale - getHeight()) / 2.0f
        mMatrix?.postScale(scale, scale)
        mMatrix?.postTranslate(deltaX, deltaY)
        imageMatrix = mMatrix
    }

    /**
     * 获得屏幕宽度
     */
    private fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

}