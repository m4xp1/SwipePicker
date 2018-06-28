package one.xcorp.widget.swipepicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.*
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_UP
import android.view.SoundEffectConstants.CLICK
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager.LayoutParams.*
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.util.*
import kotlin.properties.Delegates

class SwipePicker : LinearLayout {

    companion object {
        private const val ANIMATION_DURATION = 200L
    }

    // <editor-fold desc="Properties">
    var hint: CharSequence
        get() = hintTextView.text
        set(value) {
            hintTextView.text = value
        }
    var inputBackground: Drawable
        get() = backgroundView.background
        set(value) = ViewCompat.setBackground(backgroundView, value)
    var deactivateGesture = true
    var manualInput = true
        set(enable) {
            if (!enable) {
                isSelected = false
            }
            field = enable
        }
    var inputType: Int
        get() = inputEditText.inputType
        set(value) {
            if (value !in arrayOf(2, 4098, 8194, 12290)) {
                throw IllegalArgumentException("Only TYPE_CLASS_NUMBER with" +
                        "TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL are allowed")
            }
            inputEditText.inputType = value
        }
    var scaleValues: FloatArray? = null
        set(value) {
            val oldValue = field
            val newValue = value

            if (minValue == oldValue?.first()) {
                minValue = newValue?.first() ?: minValue
            }
            if (maxValue == oldValue?.last()) {
                maxValue = newValue?.last() ?: maxValue
            }

            field = value
            invalidateValue()
        }
    var minValue by Delegates.observable(1f) { _, _, _ -> invalidateValue() }
    var maxValue by Delegates.observable(10f) { _, _, _ -> invalidateValue() }
    var stepSize = 1f
    var defaultValue = minValue
    var value by Delegates.observable(defaultValue) { _, _, _ -> invalidateValue() }
    var restriction = Restriction.LOWER
        set(value) {
            if (value !in arrayOf(Restriction.NONE, Restriction.LOWER,
                            Restriction.UPPER, Restriction.LOWER or Restriction.UPPER)) {
                throw IllegalArgumentException("Unknown restriction type")
            }
            field = value
            invalidateValue()
        }
    // </editor-fold>

    private val windowManager: WindowManager
    private val gestureDetector: GestureDetectorCompat

    private val hoverView by lazy { createHoverView() }
    private val hintTextView by lazy { findViewById<AppCompatTextView>(android.R.id.hint) }
    private val backgroundView by lazy { findViewById<View>(android.R.id.background) }
    private val inputEditText by lazy { findViewById<EditText>(android.R.id.input) }

    private var activated = false
    private var hoverViewStyle = R.style.XcoRp_Style_SwipePicker_HoverView

    private val fontScale: Float
    private val backgroundViewPosition = IntArray(2)
    private val hoverViewMargin = resources.getDimensionPixelSize(R.dimen.hoverView_margin).toFloat()
    private val hoverViewLayoutParams by lazy { createHoverViewLayoutParams() }
    private val numberFormat = NumberFormat.getInstance(Locale.US).apply { isGroupingUsed = false }
    private var hintAnimation: HintAnimation? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.swipePicker)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, R.style.XcoRp_Widget_SwipePicker)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        gestureDetector = GestureDetectorCompat(context, GestureListener())
        gestureDetector.setIsLongpressEnabled(false)

        orientation = VERTICAL
        isChildrenDrawingOrderEnabled = true

        inflate(context, R.layout.swipe_picker, this)
        obtainStyledAttributes(context, attrs, defStyleAttr, defStyleRes)

        backgroundView.setOnTouchListener(::onTouch)
        inputEditText.setOnBackPressedListener(::onInputCancel)
        inputEditText.setOnEditorActionListener(::onInputDone)

        fontScale = inputEditText.textSize / hintTextView.textSize
    }

    /**
     * It is required that the background does not hide a hint.
     */
    override fun getChildDrawingOrder(childCount: Int, i: Int) =
            if (i == 0) 1 else if (i == 1) 0 else i

    private fun createHoverView(): HoverView {
        val hoverView = HoverView(context, defStyleRes = hoverViewStyle)
        hoverView.alpha = 0f

        return hoverView
    }

    private fun createHoverViewLayoutParams() = WindowManager.LayoutParams().apply {
        width = MATCH_PARENT
        height = MATCH_PARENT
        format = PixelFormat.TRANSLUCENT
        flags = (FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE)
        type = TYPE_APPLICATION_PANEL
    }

    private fun obtainStyledAttributes(
            context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.SwipePicker, defStyleAttr, defStyleRes)

        minimumWidth = typedArray.getDimensionPixelSize(
                R.styleable.SwipePicker_android_minWidth,
                resources.getDimensionPixelSize(R.dimen.swipePicker_minWidth))
        val padding = typedArray.getDimensionPixelSize(
                R.styleable.SwipePicker_android_padding,
                resources.getDimensionPixelSize(R.dimen.swipePicker_padding))
        hintTextView.setPadding(padding, padding, padding, padding)
        inputEditText.setPadding(padding, padding, padding, padding)
        setPadding(0, 0, 0, 0)
        hint = typedArray.getString(R.styleable.SwipePicker_android_hint)
        setMaxLength(typedArray.getInt(R.styleable.SwipePicker_android_maxLength,
                resources.getInteger(R.integer.swipePicker_maxLength)))
        activated = typedArray.getBoolean(
                R.styleable.SwipePicker_android_state_activated, activated)
        setHintTextAppearance(typedArray.getResourceId(
                R.styleable.SwipePicker_hintTextAppearance,
                R.style.XcoRp_TextAppearance_SwipePicker_Hint))
        setInputTextAppearance(typedArray.getResourceId(
                R.styleable.SwipePicker_inputTextAppearance,
                R.style.XcoRp_TextAppearance_SwipePicker_Input))
        inputBackground = typedArray.getDrawable(R.styleable.SwipePicker_inputBackground)
        deactivateGesture = typedArray
                .getBoolean(R.styleable.SwipePicker_deactivateGesture, deactivateGesture)
        manualInput = typedArray.getBoolean(R.styleable.SwipePicker_manualInput, manualInput)
        inputType = typedArray.getInt(
                R.styleable.SwipePicker_inputType, InputType.TYPE_CLASS_NUMBER)
        if (typedArray.hasValue(R.styleable.SwipePicker_scaleValues)) {
            scaleValues = obtainScaleValues(
                    typedArray.getResourceId(R.styleable.SwipePicker_scaleValues, 0))
        }
        minValue = typedArray.getFloat(
                R.styleable.SwipePicker_minValue, scaleValues?.first() ?: minValue)
        maxValue = typedArray.getFloat(
                R.styleable.SwipePicker_maxValue, scaleValues?.last() ?: maxValue)
        if (minValue >= maxValue) {
            throw IllegalArgumentException("The minimum value must be less than the maximum.")
        }
        stepSize = typedArray.getFloat(R.styleable.SwipePicker_stepSize, stepSize)
        defaultValue = typedArray.getFloat(R.styleable.SwipePicker_value, minValue)
        value = defaultValue
        restriction = typedArray.getInt(R.styleable.SwipePicker_restriction, restriction)
        hoverViewStyle = typedArray.getResourceId(
                R.styleable.SwipePicker_hoverViewStyle, hoverViewStyle)

        typedArray.recycle()
    }

    /**
     * Read floating array from resources xml and check it.
     * Array must have more than two elements in ascending order and floating type.
     *
     * @param arrayResId xml resource identifier
     * @return read array
     * @throws IllegalArgumentException if validation fail
     */
    private fun obtainScaleValues(arrayResId: Int): FloatArray {
        val typedArray = resources.obtainTypedArray(arrayResId)
        try {
            val result = FloatArray(typedArray.length())

            var isValid = result.size >= 2
            var i = 0
            while (isValid && ++i < result.size) {
                result[i - 1] = typedArray.getFloat(i - 1, 0f)
                result[i] = typedArray.getFloat(i, 0f)
                if (result[i - 1] >= result[i]) {
                    isValid = false
                }
            }

            if (!isValid) {
                throw IllegalArgumentException("Invalid array format for the value scale. " +
                        "An array must have more than two elements in ascending order.")
            }

            return result
        } finally {
            typedArray.recycle()
        }
    }

    fun setMaxLength(max: Int) {
        val filters = inputEditText.filters
                .filter { f -> f !is InputFilter.LengthFilter }
                .toMutableList()
        filters.add(InputFilter.LengthFilter(max))
        inputEditText.filters = filters.toTypedArray()
    }

    fun setHintTextAppearance(resId: Int) = TextViewCompat.setTextAppearance(hintTextView, resId)

    fun setInputTextAppearance(resId: Int) = TextViewCompat.setTextAppearance(inputEditText, resId)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            isActivated = activated
            hintAnimation?.end()

            if (isPressed) {
                invalidateHoverViewPosition()
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        isSelected = true
        playSoundEffect(CLICK)
        return true
    }

    private fun onTouch(view: View, event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        if (event.action == ACTION_UP || event.action == ACTION_CANCEL) {
            isPressed = false
            view.playSoundEffect(CLICK)
        }
        return result
    }

    override fun isActivated() = activated

    override fun setActivated(activated: Boolean) {
        hintAnimation?.cancel()
        hintAnimation = HintAnimation(hintTextView).setDuration(ANIMATION_DURATION)

        hintAnimation?.let { animation ->
            if (activated) {
                animation.to(0f, 1f)
                animation.addEndListener {
                    inputEditText.visibility = View.VISIBLE
                    super.setActivated(activated)
                }.start()
            } else {
                isSelected = false

                val scaledHeight = hintTextView.height *
                        inputEditText.height / hintTextView.height.toFloat()
                val verticalOffset = (hintTextView.height - scaledHeight) / 2

                animation.to(scaledHeight + verticalOffset + inputEditText.top, fontScale)
                animation.addStartListener {
                    inputEditText.visibility = View.INVISIBLE
                    super.setActivated(activated)
                }.start()
            }
        }

        if (this.activated != activated) {
            value = defaultValue
        }
        this.activated = activated
    }

    override fun setSelected(selected: Boolean) {
        if (!manualInput || isSelected == selected) {
            return
        }

        super.setSelected(selected)

        if (!selected) {
            onInputCancel()
        } else if (!isActivated) {
            isActivated = true
        }

        if (hintAnimation?.isRunning != true) {
            setInputEnabled(selected)
        } else {
            hintAnimation?.addEndListener { setInputEnabled(isSelected) }
        }
    }

    override fun setPressed(pressed: Boolean) {
        if (pressed == isPressed) {
            return
        }

        if (pressed && !isActivated) {
            hintTextView.alpha = 0f

            isActivated = true
            hintAnimation?.end()
        }

        super.setPressed(pressed)
        if (pressed) {
            showHoverView()
        } else {
            hideHoverView()
        }
    }

    private fun setInputEnabled(enabled: Boolean) {
        if (inputEditText.isEnabled == enabled) {
            return
        }

        inputEditText.isEnabled = enabled
        if (enabled) {
            inputEditText.requestFocus()
            inputEditText.selectAll()
            inputEditText.showKeyboard()
        } else {
            inputEditText.setSelection(0)
            inputEditText.clearFocus()
            inputEditText.hideKeyBoard()
        }
    }

    private fun onInputCancel(): Boolean {
        inputEditText.setText(numberFormat.format(value))
        isSelected = false
        return true
    }

    private fun onInputDone(view: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER) {
            value = view.text.toString().toFloatOrNull() ?: value
            isSelected = false
            playSoundEffect(CLICK)
            return true
        }
        return false
    }

    private fun showHoverView() {
        hoverView.text = inputEditText.text
        invalidateHoverViewPosition()

        hintTextView.animate().alpha(0f).setDuration(ANIMATION_DURATION).start()
        hoverView.animate().alpha(1f).setDuration(ANIMATION_DURATION)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        if (hoverView.parent == null) {
                            windowManager.addView(hoverView, hoverViewLayoutParams)
                        }
                    }
                }).start()
    }

    private fun hideHoverView() {
        hintTextView.animate().alpha(1f).setDuration(0).start()
        hoverView.animate().alpha(0f).setDuration(0)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (hoverView.parent != null) {
                            windowManager.removeViewImmediate(hoverView)
                        }
                    }
                }).start()
    }

    private fun invalidateHoverViewPosition() {
        hoverView.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        backgroundView.getLocationOnScreen(backgroundViewPosition)

        hoverView.x = backgroundViewPosition[0] +
                backgroundView.width / 2f - hoverView.measuredWidth / 2f
        hoverView.y = backgroundViewPosition[1] -
                hoverView.measuredHeight - hoverViewMargin
    }

    private fun invalidateValue() {
        inputEditText.setText(numberFormat.format(value))
        if (isPressed) {
            hoverView.text = inputEditText.text
            invalidateHoverViewPosition()
        }
    }

    object Restriction {
        const val NONE = 0x00000000
        const val LOWER = 0x00000001
        const val UPPER = 0x00000002
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private var isShowPress = false // used to eliminate flicker

        override fun onDown(event: MotionEvent): Boolean {
            isShowPress = false
            return true
        }

        override fun onShowPress(e: MotionEvent) {
            isPressed = true
            isShowPress = true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isShowPress) {
                isSelected = true
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (deactivateGesture) {
                isActivated = false
                return true
            }
            return false
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            isPressed = true
            return false
        }
    }
}
