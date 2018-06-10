package one.xcorp.widget.swipepicker

import android.content.Context
import android.graphics.*
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

internal class HoverView : View {

    private val paint = Paint()
    private val contentRect = Rect()
    private val backgroundRect = RectF()
    private val backgroundPath = Path()

    var textSize
        get() = paint.textSize
        set(value) {
            paint.textSize = value
            requestLayout()
        }
    var textColor = fetchColor(android.R.attr.textColorPrimaryInverse)
        set(value) {
            field = value
            invalidate()
        }
    var color = fetchColor(R.attr.colorPrimary)
        set(value) {
            field = value
            invalidate()
        }
    var text = ""
        set(value) {
            field = value
            requestLayout()
        }

    private val radiusCorner: Float

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs, R.style.XcoRp_Style_SwipePicker_HoverView)

    constructor(context: Context, attrs: AttributeSet?, styleRes: Int) :
            super(context, attrs, 0) {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER

        val typedArray = context.obtainStyledAttributes(styleRes, R.styleable.HoverView)
        minimumWidth = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_minWidth,
                resources.getDimensionPixelSize(R.dimen.hoverView_minWidth))
        val padding = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_padding,
                resources.getDimensionPixelSize(R.dimen.hoverView_padding))
        setPadding(padding, padding, padding, padding)
        textSize = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_textSize,
                resources.getDimensionPixelSize(R.dimen.hoverView_textSize)).toFloat()
        textColor = typedArray.getColor(
                R.styleable.HoverView_android_textColor, textColor)
        color = typedArray.getColor(
                R.styleable.HoverView_android_color, color)
        typedArray.recycle()

        radiusCorner = resources.getDimensionPixelSize(R.dimen.hoverView_radiusCorner).toFloat()
    }

    private fun fetchColor(attr: Int): Int {
        val typedArray = context.obtainStyledAttributes(0, intArrayOf(attr))

        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        return color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        paint.getTextBounds(text, 0, text.length, contentRect)

        val width = Math.max(ViewCompat.getMinimumWidth(this),
                (paint.measureText(text) + 0.5f).toInt())
        val height = Math.max(textSize.toInt(), contentRect.height())

        contentRect.set(0, 0,
                paddingLeft + width + paddingRight,
                paddingTop + height + paddingBottom)

        val arrowSize = (contentRect.height() / 2.5f + 0.5f)

        backgroundRect.set(contentRect)
        backgroundRect.bottom += arrowSize

        setMeasuredDimension(backgroundRect.width().toInt(), backgroundRect.height().toInt())
        defineBackgroundPath(backgroundRect, arrowSize)
    }

    private fun defineBackgroundPath(rect: RectF, arrowSize: Float) = with(backgroundPath) {
        reset()
        moveTo(rect.left + radiusCorner, rect.top)
        lineTo(rect.width() - radiusCorner, rect.top)
        arcTo(RectF(rect.right - radiusCorner, rect.top, rect.right,
                radiusCorner + rect.top), 270f, 90f)

        lineTo(rect.right, rect.bottom - arrowSize - radiusCorner)
        arcTo(RectF(rect.right - radiusCorner,
                rect.bottom - radiusCorner - arrowSize, rect.right,
                rect.bottom - arrowSize), 0f, 90f)

        lineTo(rect.left + rect.width() / 2 + arrowSize / 2, rect.bottom - arrowSize)
        lineTo(rect.left + rect.width() / 2, rect.bottom)
        lineTo(rect.left + rect.width() / 2 - arrowSize / 2, rect.bottom - arrowSize)
        lineTo(rect.left + Math.min(radiusCorner, rect.width() / 2 - arrowSize / 2),
                rect.bottom - arrowSize)

        arcTo(RectF(rect.left, rect.bottom - radiusCorner - arrowSize, radiusCorner
                + rect.left, rect.bottom - arrowSize), 90f, 90f)
        lineTo(rect.left, rect.top + radiusCorner)
        arcTo(RectF(rect.left, rect.top, radiusCorner + rect.left,
                radiusCorner + rect.top), 180f, 90f)
        close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = color
        canvas.drawPath(backgroundPath, paint)

        paint.color = textColor
        val fm = paint.fontMetrics

        val baseline = contentRect.height() / 2 + (fm.descent - fm.ascent) / 2f - fm.descent
        canvas.drawText(text, (contentRect.width() / 2).toFloat(), baseline, paint)
    }
}