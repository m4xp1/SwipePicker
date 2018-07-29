package one.xcorp.widget.swipepicker

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

class HoverView : View {

    // <editor-fold desc="Properties">
    var textSize
        get() = paint.textSize
        set(value) {
            paint.textSize = value
            requestLayout()
        }
    var textColor: Int
        @ColorInt get
        set(@ColorInt value) {
            field = value
            invalidate()
        }
    var color: Int
        @ColorInt get
        set(@ColorInt value) {
            field = value
            invalidate()
        }
    var colorTint: Int
        @ColorInt get
        set(@ColorInt value) {
            field = value
            invalidate()
        }
    var text: CharSequence = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
    // </editor-fold>

    private val paint = Paint()
    private val contentRect = Rect()
    private val backgroundRect = RectF()
    private val arcRect = RectF()
    private val backgroundPath = Path()
    private val radiusCorner: Float

    private val digits = Regex("\\d")
    private val wideDigit = "4"

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, R.style.XcoRp_Style_SwipePicker_HoverView)

    constructor(context: Context,
                attrs: AttributeSet? = null,
                defStyleAttr: Int = 0,
                defStyleRes: Int = R.style.XcoRp_Style_SwipePicker_HoverView)
            : super(context, attrs, defStyleAttr) {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER

        val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.HoverView, defStyleAttr, defStyleRes)
        minimumWidth = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_minWidth,
                resources.getDimensionPixelSize(R.dimen.hoverView_minWidth))
        val padding = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_padding,
                resources.getDimensionPixelSize(R.dimen.hoverView_padding))
        setPadding(padding, padding, padding, padding)
        textSize = typedArray.getDimensionPixelSize(R.styleable.HoverView_android_textSize,
                resources.getDimensionPixelSize(R.dimen.hoverView_textSize)).toFloat()
        textColor = typedArray.getColor(R.styleable.HoverView_android_textColor,
                ContextCompat.getColor(context, R.color.swipePicker_textInverse))
        color = typedArray.getColor(R.styleable.HoverView_android_color,
                ContextCompat.getColor(context, R.color.swipePicker_primary))
        colorTint = typedArray.getColor(R.styleable.HoverView_colorTint, Color.TRANSPARENT)
        typedArray.recycle()

        radiusCorner = resources.getDimensionPixelSize(R.dimen.hoverView_radiusCorner).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ems = text.toString().replace(digits, wideDigit)
        paint.getTextBounds(ems, 0, ems.length, contentRect)

        val right = Math.max(ViewCompat.getMinimumWidth(this),
                paddingLeft + contentRect.width() + paddingRight)
        val bottom = paddingTop + textSize.toInt() + paddingBottom
        contentRect.set(0, 0, right, bottom)

        val arrowSize = contentRect.height() / 2.5f

        backgroundRect.set(contentRect)
        backgroundRect.bottom += arrowSize

        defineBackgroundPath(backgroundRect, radiusCorner, arrowSize)
        setMeasuredDimension(backgroundRect.width().toInt(), backgroundRect.height().toInt())
    }

    private fun defineBackgroundPath(rect: RectF, radiusCorner: Float, arrowSize: Float) = with(backgroundPath) {
        reset()
        moveTo(rect.left + radiusCorner, rect.top)

        lineTo(rect.width() - radiusCorner, rect.top)
        arcRect.set(rect.right - radiusCorner, rect.top,
                rect.right, radiusCorner + rect.top)
        arcTo(arcRect, 270f, 90f)

        lineTo(rect.right, rect.bottom - arrowSize - radiusCorner)
        arcRect.set(rect.right - radiusCorner, rect.bottom - radiusCorner - arrowSize,
                rect.right, rect.bottom - arrowSize)
        arcTo(arcRect, 0f, 90f)

        lineTo(rect.left + rect.width() / 2 + arrowSize / 2, rect.bottom - arrowSize)
        lineTo(rect.left + rect.width() / 2, rect.bottom)
        lineTo(rect.left + rect.width() / 2 - arrowSize / 2, rect.bottom - arrowSize)
        lineTo(rect.left + Math.min(radiusCorner, rect.width() / 2 - arrowSize / 2),
                rect.bottom - arrowSize)

        arcRect.set(rect.left, rect.bottom - radiusCorner - arrowSize,
                radiusCorner + rect.left, rect.bottom - arrowSize)
        arcTo(arcRect, 90f, 90f)
        lineTo(rect.left, rect.top + radiusCorner)

        arcRect.set(rect.left, rect.top,
                radiusCorner + rect.left, radiusCorner + rect.top)
        arcTo(arcRect, 180f, 90f)
        close()
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = if (colorTint != Color.TRANSPARENT) colorTint else color
        canvas.drawPath(backgroundPath, paint)

        paint.color = textColor
        val fm = paint.fontMetrics

        val baseline = contentRect.height() / 2 + (fm.descent - fm.ascent) / 2 - fm.descent
        canvas.drawText(text, 0, text.length, contentRect.width() / 2f, baseline, paint)
    }
}
