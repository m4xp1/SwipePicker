package one.xcorp.widget.swipepicker

import android.animation.*
import android.content.Context
import android.content.res.TypedArray
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.text.InputFilter
import android.text.InputType
import android.text.InputType.*
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

class SwipePicker : LinearLayout {

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val DELAY_SHOW_PRESS = 200
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
    var allowDeactivate = true
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
            when (value) {
                TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL,
                TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL,
                TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_SIGNED,
                TYPE_CLASS_NUMBER -> inputEditText.inputType = value
                else -> throw IllegalArgumentException("Only TYPE_CLASS_NUMBER with " +
                        "TYPE_NUMBER_FLAG_SIGNED or TYPE_NUMBER_FLAG_DECIMAL are allowed here.")
            }
        }
    var scale: List<Float>? = null
        set(value) {
            value?.let {
                val isAscending = it.windowed(2).all { (a, b) -> a < b }
                if (it.isEmpty() || !isAscending)
                    throw IllegalArgumentException("Invalid values scale format. " +
                            "An array must have one or more elements in ascending order.")
            }
            field = value?.toList()
        }
    var minValue = -Float.MAX_VALUE
        set(_value) {
            field = _value
            value = value // check value restriction
        }
    var maxValue = Float.MAX_VALUE
        set(_value) {
            field = _value
            value = value // check value restriction
        }
    var step = 1f
    var value = 1f
        set(value) {
            val oldValue = field
            var newValue = value

            if (newValue < minValue) {
                newValue = minValue
            } else if (newValue > maxValue) {
                newValue = maxValue
            }

            field = newValue
            invalidateValue()
            if (newValue != oldValue) {
                valueChangeListener?.onValueChanged(this, oldValue, newValue)
            }
        }
    val hoverView by lazy { createHoverView() }
    // </editor-fold>

    private val windowManager: WindowManager
    private val gestureDetector: GestureDetectorCompat

    private val hintTextView by lazy { findViewById<AppCompatTextView>(android.R.id.hint) }
    private val backgroundView by lazy { findViewById<View>(android.R.id.background) }
    private val inputEditText by lazy { findViewById<EditText>(android.R.id.input) }

    private var activated = false
    private var hoverViewStyle = R.style.XcoRp_Style_SwipePicker_HoverView

    private val fontScale: Float
    private val hoverViewMargin: Float
    private val backgroundViewPosition = IntArray(2)
    private val hoverViewLayoutParams by lazy { createHoverViewLayoutParams() }
    private var hintAnimation: AnimatorSet? = null

    private val numberFormat = NumberFormat.getInstance(Locale.US).apply { isGroupingUsed = false }
    private var stateChangeListener: OnStateChangeListener? = null
    private var valueChangeListener: OnValueChangeListener? = null
    private var swipeHandler: OnSwipeHandler? = object : OnSwipeHandler {}

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
        hoverViewMargin = resources.getDimensionPixelSize(R.dimen.hoverView_margin).toFloat()
    }

    // It is required that the background does not hide a hint.
    override fun getChildDrawingOrder(childCount: Int, i: Int) =
            if (i == 0) 1 else if (i == 1) 0 else i

    private fun createHoverView() = HoverView(context,
            defStyleRes = hoverViewStyle).apply { alpha = 0f }

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
        allowDeactivate = typedArray
                .getBoolean(R.styleable.SwipePicker_allowDeactivate, allowDeactivate)
        manualInput = typedArray.getBoolean(R.styleable.SwipePicker_manualInput, manualInput)
        inputType = typedArray.getInt(
                R.styleable.SwipePicker_inputType, InputType.TYPE_CLASS_NUMBER)
        scale = typedArray.getFloatArray(R.styleable.SwipePicker_scale)?.toList()
        minValue = typedArray.getFloat(R.styleable.SwipePicker_minValue, minValue)
        maxValue = typedArray.getFloat(R.styleable.SwipePicker_maxValue, maxValue)
        if (minValue >= maxValue) {
            throw IllegalArgumentException("The minimum value is greater than the maximum.")
        }
        step = typedArray.getFloat(R.styleable.SwipePicker_step, step)
        value = typedArray.getFloat(R.styleable.SwipePicker_value, value)
        hoverViewStyle = typedArray.getResourceId(
                R.styleable.SwipePicker_hoverViewStyle, hoverViewStyle)

        typedArray.recycle()
    }

    /**
     * Read floating array from resources xml.
     *
     * @param index Index of attribute to retrieve.
     * @return Read floating array. Returns {@code null} if the given resId does not exist.
     */
    private fun TypedArray.getFloatArray(index: Int): FloatArray? {
        var typedArray: TypedArray? = null
        return try {
            typedArray = resources.obtainTypedArray(getResourceId(index, 0))

            val result = FloatArray(typedArray.length())
            for (i in 0 until typedArray.length()) {
                result[i] = typedArray.getFloat(i, 0f)
            }
            result
        } catch (e: Throwable) {
            null
        } finally {
            typedArray?.recycle()
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

    fun setOnStateChangeListener(listener: (isActivated: Boolean) -> Unit) =
            setOnStateChangeListener(object : OnStateChangeListener {
                override fun onStateChanged(view: SwipePicker, isActivated: Boolean) {
                    listener(isActivated)
                }
            })

    fun setOnStateChangeListener(listener: OnStateChangeListener?) {
        stateChangeListener = listener
    }

    fun setOnValueChangeListener(listener: (value: Float) -> Unit) =
            setOnValueChangeListener(object : OnValueChangeListener {
                override fun onValueChanged(view: SwipePicker, oldValue: Float, newValue: Float) {
                    listener(newValue)
                }
            })

    fun setOnValueChangeListener(listener: OnValueChangeListener?) {
        valueChangeListener = listener
    }

    fun setOnSwipeHandler(handler: (value: Float, division: Int) -> Float) =
            setOnSwipeHandler(object : OnSwipeHandler {
                override fun onSwipe(view: SwipePicker, value: Float, division: Int): Float {
                    return handler(value, division)
                }
            })

    fun setOnSwipeHandler(handler: OnSwipeHandler?) {
        swipeHandler = handler
    }

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
        if (!super.performClick()) {
            playSoundEffect(CLICK)
        }
        isSelected = true
        return true
    }

    private fun onTouch(view: View, event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        if (event.action == ACTION_UP || event.action == ACTION_CANCEL) {
            handler.removeCallbacksAndMessages(gestureDetector)
            isPressed = false
            view.playSoundEffect(CLICK)
        }
        return result
    }

    override fun isActivated() = activated

    override fun setActivated(activated: Boolean) {
        hintAnimation?.removeAllListeners()
        hintAnimation?.cancel()

        hintAnimation = if (activated) {
            animateHintTo(0f, 1f).addEndListener {
                inputEditText.visibility = View.VISIBLE
                super.setActivated(activated)
            }
        } else {
            isSelected = false

            val scaledHeight = hintTextView.height *
                    inputEditText.height / hintTextView.height.toFloat()
            val scaledVerticalOffset = (hintTextView.height - scaledHeight) / 2
            val y = scaledHeight + scaledVerticalOffset + inputEditText.top

            animateHintTo(y, fontScale).addStartListener {
                inputEditText.visibility = View.INVISIBLE
                super.setActivated(activated)
            }
        }
        hintAnimation?.start()

        if (this.activated != activated) {
            this.activated = activated
            stateChangeListener?.onStateChanged(this, activated)
        }
    }

    private fun animateHintTo(y: Float, scale: Float): AnimatorSet {
        val animatorSet = AnimatorSet()
        val animMove = ObjectAnimator.ofFloat(hintTextView, "translationY", y)
        val animScale = ObjectAnimator.ofPropertyValuesHolder(hintTextView,
                PropertyValuesHolder.ofFloat("scaleX", scale),
                PropertyValuesHolder.ofFloat("scaleY", scale))
        animatorSet.playTogether(animMove, animScale)
        animatorSet.duration = ANIMATION_DURATION
        return animatorSet
    }

    override fun setSelected(selected: Boolean) {
        if (!manualInput || isSelected == selected) return

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
        if (isSelected) {
            invalidateValue()
            isSelected = false
            return true
        }
        return false
    }

    private fun onInputDone(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            value = view.text.toString().toFloatOrNull() ?: value
            isSelected = false
            playSoundEffect(CLICK)
            return true
        }
        return false
    }

    override fun setPressed(pressed: Boolean) {
        if (pressed == isPressed) return

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

    interface OnStateChangeListener {

        /**
         * The listener is called every time the activate state changes.
         *
         * @param view SwipePicker of initiating event.
         * @param isActivated Current activated state.
         */
        fun onStateChanged(view: SwipePicker, isActivated: Boolean)
    }

    interface OnValueChangeListener {

        /**
         * The listener is called every time the value changes.
         *
         * @param view SwipePicker of initiating event.
         * @param oldValue Old value.
         * @param newValue New value.
         */
        fun onValueChanged(view: SwipePicker, oldValue: Float, newValue: Float)
    }

    interface OnSwipeHandler {

        /**
         * The handler is called when the gesture occurs and
         * is intended to change the algorithm for calculating the value.
         *
         * @param view SwipePicker of initiating event.
         * @param value The value from which need moved.
         * @param division The number of divisions that have moved.
         * @return The calculated value after the gesture processing which must be set to the view.
         */
        fun onSwipe(view: SwipePicker, value: Float, division: Int): Float = with(view) {
            val scale = view.scale

            if (division == 0) return value
            if (scale == null
                    || (value < scale.first() && division < 0)
                    || (value > scale.last() && division > 0)) {
                return value + (division * step)
            }



            return value
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = resources.displayMetrics.density * 15f

        private var isShowPress = false

        private var initialValue = 0f
        private var previousDivision = 0

        override fun onDown(event: MotionEvent): Boolean {
            isShowPress = false

            initialValue = value
            previousDivision = 0

            handler.postAtTime(::onShowPress,
                    gestureDetector, event.downTime + DELAY_SHOW_PRESS)

            return true
        }

        // Use it because SimpleOnGestureListener#onShowPress(MotionEvent e)
        // causes flicker and wrong behavior.
        fun onShowPress() {
            isPressed = true
            isShowPress = true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isShowPress) {
                isSelected = true
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (allowDeactivate) {
                isActivated = false
                return true
            }
            return false
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val division = Math.round((e2.x - e1.x) / swipeThreshold)

            if (previousDivision != division) {
                if (!isPressed) onShowPress()
                swipeHandler?.let {
                    value = it.onSwipe(this@SwipePicker, initialValue, division)
                }
            }

            previousDivision = division
            return isPressed
        }
    }

    private fun AnimatorSet.addStartListener(listener: () -> Unit): AnimatorSet {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                listener()
            }
        })
        return this
    }

    private fun AnimatorSet.addEndListener(listener: () -> Unit): AnimatorSet {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                listener()
            }
        })
        return this
    }
}
