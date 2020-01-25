package one.xcorp.widget.swipepicker

import android.animation.*
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.support.annotation.ColorInt
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.AppCompatTextView
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_UP
import android.view.SoundEffectConstants.CLICK
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager.LayoutParams.*
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Scroller
import java.io.Serializable
import java.text.NumberFormat
import java.util.*
import kotlin.math.sign

class SwipePicker : LinearLayout {

    companion object {
        private const val ANIMATION_DURATION = 200L

        private const val VELOCITY_DOWNSCALE = 2
        private const val VELOCITY_THRESHOLD = 300

        private const val PRESS_DELAY_SHOW = 200
        private const val PRESS_DELAY_HIDE = 450
    }

    // <editor-fold desc="Properties">
    var hint: CharSequence?
        get() = hintTextView.text
        set(value) {
            hintTextView.text = value
        }
    var allowDeactivate = true
    var allowFling = true
    var hintTextSize: Float
        get() = hintTextView.textSize
        set(value) = hintTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
    var hintTextColor: ColorStateList
        get() = hintTextView.textColors
        set(value) = hintTextView.setTextColor(value)
    var inputTextSize: Float
        get() = inputEditText.textSize
        set(value) = inputEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
    var inputTextColor: ColorStateList
        get() = inputEditText.textColors
        set(value) = inputEditText.setTextColor(value)
    var backgroundInput: Drawable?
        get() = inputAreaView.background
        set(value) {
            // clear tint on previously background
            val background = backgroundInput
            background?.let {
                DrawableCompat.setTintList(it, null)
                DrawableCompat.setTintMode(it, PorterDuff.Mode.SRC_IN)
            }
            // set new background and invalidate tint
            ViewCompat.setBackground(inputAreaView, value)
            backgroundInputTint = backgroundInputTint
            backgroundInputTintMode = backgroundInputTintMode
        }
    var backgroundInputTint: ColorStateList? = null
        set(value) {
            field = value
            backgroundInput?.let { DrawableCompat.setTintList(it, value) }
        }
    var backgroundInputTintMode = PorterDuff.Mode.SRC_IN
        set(value) {
            field = value
            backgroundInput?.let { DrawableCompat.setTintMode(it, value) }
        }
    var manualInput = true
        set(enable) {
            if (!enable) {
                isSelected = false
            }
            field = enable
        }
    var inputFilters: Array<InputFilter>
        get() = inputEditText.filters
        set(value) {
            inputEditText.filters = value
            invalidateValue()
        }
    var inputType: Int
        get() = inputEditText.inputType
        set(value) {
            inputEditText.inputType = value
        }
    var scale: List<Float>? = null
        set(scale) {
            if (scale == null) {
                field = null
                return
            }

            require(!scale.isEmpty() && scale.windowed(2).all { (a, b) -> a < b }) {
                "Invalid values scale format. An array must have one or more elements in ascending order."
            }

            field = scale.toList()
            if (sticky) {
                value = value // stick value to scale
            }
        }
    var minValue = -Float.MAX_VALUE
        set(min) {
            field = min
            value = value // check value restriction
        }
    var maxValue = Float.MAX_VALUE
        set(max) {
            field = max
            value = value // check value restriction
        }
    var step = 1f
        set(step) {
            field = step
            if (sticky && scale != null) {
                value = value // stick value to scale
            }
        }
    var value = 1f
        set(value) {
            val oldValue = field
            var newValue = value

            if (sticky && scale != null) {
                newValue = scaleHandler.onStick(this, newValue)
            }

            when {
                newValue < minValue -> newValue = minValue
                newValue > maxValue -> newValue = maxValue
            }

            if (newValue != oldValue) {
                field = newValue
                invalidateValue()

                valueChangeListener?.onValueChanged(this, oldValue, newValue)
            }
        }
    var sticky = false
        set(enable) {
            field = enable
            if (enable && scale != null) {
                value = value // stick value to scale
            }
        }
    var looped = false
    val hoverView by lazy { createHoverView() }
    // </editor-fold>

    private val windowManager: WindowManager
    private val gestureDetector: GestureDetectorCompat
    private val scrollHandler = ScrollHandler()
    private val scaleHelper = ScaleHelper()

    private val hintTextView by lazy { findViewById<AppCompatTextView>(android.R.id.hint) }
    private val inputAreaView by lazy { findViewById<View>(android.R.id.inputArea) }
    private val inputEditText by lazy { findViewById<EditText>(android.R.id.input) }

    private val numberFormat = NumberFormat.getInstance(Locale.US).apply { isGroupingUsed = false }
    private var activated = false
    private val inputAreaPosition = IntArray(2)
    private var hintAnimator: AnimatorSet? = null
    private val hoverViewMargin = resources.getDimensionPixelSize(R.dimen.hoverView_margin)
    private var hoverViewStyle = R.style.XcoRp_Style_SwipePicker_HoverView
    private val hoverViewLayoutParams by lazy { createHoverViewLayoutParams() }

    private var valueTransformer: ValueTransformer = object : ValueTransformer {}
    private var scaleHandler: ScaleHandler = object : ScaleHandler {}
    private var stateChangeListener: OnStateChangeListener? = null
    private var valueChangeListener: OnValueChangeListener? = null

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
        // android still save text selection when set saveEnabled=false
        // for symmetry we will make unique id for all
        hintTextView.id = id + hintTextView.id
        inputAreaView.id = id + inputAreaView.id
        inputEditText.id = id + inputEditText.id

        obtainStyledAttributes(context, attrs, defStyleAttr, defStyleRes)

        inputAreaView.setOnTouchListener { _, e -> onTouch(e) }
        inputEditText.apply {
            setOnBackPressedListener { onInputCancel() }
            setOnEditorActionListener { _, a, e -> onInputDone(a, e) }
            setOnFocusChangeListener { _, h -> onFocusChange(h) }
        }

        invalidateValue()
    }

    // It is required that the input area does not hide a hint.
    override fun getChildDrawingOrder(childCount: Int, i: Int) =
            when (i) {
                0 -> 1
                1 -> 0
                else -> i
            }

    private fun calculateHintPosition(activated: Boolean): Float {
        if (activated) return 0f

        val scaledHeight = hintTextView.height *
                inputEditText.height / hintTextView.height.toFloat()
        val scaledVerticalOffset = (hintTextView.height - scaledHeight) / 2

        return scaledHeight + scaledVerticalOffset + inputEditText.top
    }

    private fun calculateFontScale(activated: Boolean) =
            if (activated) 1f else inputEditText.textSize / hintTextView.textSize

    private fun createHoverView() = HoverView(context,
            defStyleRes = hoverViewStyle).apply { alpha = 0f }

    private fun createHoverViewLayoutParams() = WindowManager.LayoutParams().apply {
        width = MATCH_PARENT
        height = MATCH_PARENT
        format = PixelFormat.TRANSLUCENT
        flags = (FLAG_LAYOUT_NO_LIMITS or FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE)
        type = TYPE_APPLICATION_PANEL
    }

    private fun obtainStyledAttributes(
            context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) =
            with(context.obtainStyledAttributes(
                    attrs, R.styleable.SwipePicker, defStyleAttr, defStyleRes)) {
                try {
                    minimumWidth = getDimensionPixelSize(
                            R.styleable.SwipePicker_android_minWidth,
                            resources.getDimensionPixelSize(R.dimen.swipePicker_minWidth))
                    hint = getString(R.styleable.SwipePicker_android_hint)
                    activated = getBoolean(
                            R.styleable.SwipePicker_android_state_activated, activated)
                    allowDeactivate = getBoolean(R.styleable.SwipePicker_allowDeactivate, allowDeactivate)
                    allowFling = getBoolean(R.styleable.SwipePicker_allowFling, allowFling)
                    setHintTextAppearance(getResourceId(
                            R.styleable.SwipePicker_hintTextAppearance,
                            R.style.XcoRp_TextAppearance_SwipePicker_Hint))
                    setInputTextAppearance(getResourceId(
                            R.styleable.SwipePicker_inputTextAppearance,
                            R.style.XcoRp_TextAppearance_SwipePicker_Input))
                    backgroundInput = getDrawable(R.styleable.SwipePicker_backgroundInput)
                    if (hasValue(R.styleable.SwipePicker_backgroundInputTint)) {
                        backgroundInputTint = getColorStateList(R.styleable.SwipePicker_backgroundInputTint)
                    }
                    if (hasValue(R.styleable.SwipePicker_backgroundInputTintMode)) {
                        backgroundInputTintMode = getTintMode(R.styleable.SwipePicker_backgroundInputTintMode, backgroundInputTintMode)
                    }
                    manualInput = getBoolean(R.styleable.SwipePicker_manualInput, manualInput)
                    setMaxLength(getInt(R.styleable.SwipePicker_android_maxLength,
                            resources.getInteger(R.integer.swipePicker_maxLength)))
                    inputType = getInt(
                            R.styleable.SwipePicker_android_inputType, InputType.TYPE_CLASS_NUMBER)
                    if (hasValue(R.styleable.SwipePicker_anchor)) {
                        scale = listOf(getFloat(R.styleable.SwipePicker_anchor, 0f))
                    }
                    if (!isInEditMode && hasValue(R.styleable.SwipePicker_scale)) {
                        scale = getFloatArray(R.styleable.SwipePicker_scale).toList()
                    }
                    minValue = getFloat(R.styleable.SwipePicker_minValue, minValue)
                    maxValue = getFloat(R.styleable.SwipePicker_maxValue, maxValue)
                    require(minValue < maxValue) { "The minimum value is greater than the maximum." }
                    step = getFloat(R.styleable.SwipePicker_step, step)
                    value = getFloat(R.styleable.SwipePicker_value, value)
                    sticky = getBoolean(R.styleable.SwipePicker_sticky, sticky)
                    looped = getBoolean(R.styleable.SwipePicker_looped, looped)
                    hoverViewStyle = getResourceId(
                            R.styleable.SwipePicker_hoverViewStyle, hoverViewStyle)
                } finally {
                    recycle()
                }
            }

    /**
     * Read floating array from resources xml.
     *
     * @param index Index of attribute to retrieve.
     * @return Read floating array. Throws exception if
     * the given resId does not exist or can't get array.
     */
    private fun TypedArray.getFloatArray(index: Int): FloatArray {
        var typedArray: TypedArray? = null
        return try {
            typedArray = resources.obtainTypedArray(getResourceId(index, -1))

            val result = FloatArray(typedArray.length())
            for (i in 0 until typedArray.length()) {
                result[i] = typedArray.getFloat(i, 0f)
            }

            if (result.isNotEmpty()) result else throw IllegalArgumentException("Array is empty.")
        } catch (e: Throwable) {
            throw IllegalArgumentException("Can't get float array by specified resource ID.", e)
        } finally {
            typedArray?.recycle()
        }
    }

    private fun TypedArray.getTintMode(resId: Int, default: PorterDuff.Mode) =
            when (getInt(resId, -1)) {
                3 -> PorterDuff.Mode.SRC_OVER
                5 -> PorterDuff.Mode.SRC_IN
                9 -> PorterDuff.Mode.SRC_ATOP
                14 -> PorterDuff.Mode.MULTIPLY
                15 -> PorterDuff.Mode.SCREEN
                16 -> PorterDuff.Mode.ADD
                else -> default
            }

    fun setHintTextSize(unit: Int, size: Float) = hintTextView.setTextSize(unit, size)

    fun setHintTextColor(@ColorInt color: Int) = hintTextView.setTextColor(color)

    fun setHintTextAppearance(resId: Int) = TextViewCompat.setTextAppearance(hintTextView, resId)

    fun setInputTextSize(unit: Int, size: Float) = inputEditText.setTextSize(unit, size)

    fun setInputTextColor(@ColorInt color: Int) = inputEditText.setTextColor(color)

    fun setInputTextAppearance(resId: Int) = TextViewCompat.setTextAppearance(inputEditText, resId)

    fun setBackgroundInputTint(@ColorInt color: Int) {
        backgroundInputTint = ColorStateList.valueOf(color)
    }

    fun setTintColor(@ColorInt color: Int) {
        if (color == Color.TRANSPARENT) {
            backgroundInputTint = null
        } else {
            setBackgroundInputTint(color)
        }
        hoverView.colorTint = color
    }

    fun setMaxLength(length: Int) {
        inputFilters = inputFilters
                .filter { f -> f !is InputFilter.LengthFilter }
                .toMutableList()
                .apply { add(0, InputFilter.LengthFilter(length)) }
                .toTypedArray()
    }

    fun addInputFilter(filter: InputFilter) {
        inputFilters = inputFilters
                .toMutableList()
                .apply { add(filter) }
                .toTypedArray()
    }

    fun getInputText(): String = inputEditText.text.toString()

    fun setValue(value: CharSequence): Boolean {
        val result = valueTransformer.stringToFloat(this, value.toString())

        return result?.let {
            this.value = it
            true
        } ?: false
    }

    fun setValueTransformer(transformer: ValueTransformer) {
        valueTransformer = transformer
        invalidateValue()
    }

    fun setScaleHandler(handler: ScaleHandler) {
        scaleHandler = handler
        if (sticky && scale != null) {
            value = value // stick value to scale
        }
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener?) {
        stateChangeListener = listener
    }

    fun setOnValueChangeListener(listener: (oldValue: Float, newValue: Float) -> Unit) =
            setOnValueChangeListener(object : OnValueChangeListener {
                override fun onValueChanged(view: SwipePicker, oldValue: Float, newValue: Float) {
                    listener(oldValue, newValue)
                }
            })

    fun setOnValueChangeListener(listener: OnValueChangeListener?) {
        valueChangeListener = listener
    }

    override fun onSaveInstanceState(): Parcelable {
        hintAnimator?.end() // end animation before save
        val state = SavedState(super.onSaveInstanceState())

        state.allowDeactivate = allowDeactivate
        state.allowFling = allowFling
        state.manualInput = manualInput
        state.scale = scale
        state.minValue = minValue
        state.maxValue = maxValue
        state.step = step
        state.value = value
        state.sticky = sticky
        state.looped = looped
        state.activated = isActivated
        state.selected = isSelected

        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        allowDeactivate = state.allowDeactivate
        allowFling = state.allowFling
        manualInput = state.manualInput
        scale = state.scale
        minValue = state.minValue
        maxValue = state.maxValue
        step = state.step
        value = state.value
        sticky = state.sticky
        looped = state.looped
        isActivated = state.activated
        isSelected = state.selected

        hintAnimator?.end() // end animation before restore
        super.onRestoreInstanceState(state.superState)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            val instantly = hintAnimator?.isRunning != true

            animateHint(isActivated)
            if (instantly) {
                hintAnimator?.end()
            }

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

    private fun onTouch(event: MotionEvent): Boolean {
        var result = gestureDetector.onTouchEvent(event)
        if (!result && event.action == ACTION_UP || event.action == ACTION_CANCEL) {
            scrollHandler.finishFling()
            playSoundEffect(CLICK)

            isPressed = false
            result = true
        }
        return result
    }

    override fun isActivated() = activated

    override fun setActivated(activated: Boolean) {
        if (activated == isActivated) return

        if (!activated) {
            isPressed = false
            isSelected = false
        }

        animateHint(activated)
        this.activated = activated

        stateChangeListener?.onActivated(this, activated)
    }

    private fun animateHint(activated: Boolean) {
        hintAnimator?.removeAllListeners()
        hintAnimator?.cancel()

        val yPosition = calculateHintPosition(activated)
        val fontScale = calculateFontScale(activated)

        hintAnimator = if (activated) {
            createHintAnimation(yPosition, fontScale).addEndListener {
                super.setActivated(activated)
                setInputVisible(true)
            }
        } else {
            createHintAnimation(yPosition, fontScale).addStartListener {
                super.setActivated(activated)
                setInputVisible(false)
            }
        }
        hintAnimator?.start()
    }

    private fun createHintAnimation(y: Float, scale: Float): AnimatorSet {
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
        if (!manualInput || selected == isSelected) return

        if (selected && !isActivated) {
            isActivated = true
        }

        super.setSelected(selected)
        invalidateValue()

        if (inputEditText.visibility == View.VISIBLE) {
            setInputEnable(selected)
        }

        stateChangeListener?.onSelected(this, selected)
    }

    private fun setInputVisible(visible: Boolean) {
        if (visible) {
            inputEditText.visibility = View.VISIBLE
            setInputEnable(isSelected)
        } else {
            setInputEnable(false)
            inputEditText.visibility = View.INVISIBLE
        }
    }

    private fun setInputEnable(enable: Boolean) {
        if (inputEditText.isEnabled == enable) {
            return
        }

        inputEditText.isEnabled = enable

        if (enable) {
            with(inputEditText) {
                requestFocus()
                selectAll() // selectAllOnFocus not always working when rotation
                showKeyboard()
            }
        } else {
            with(inputEditText) {
                clearFocus()
                hideKeyBoard()
            }
        }
    }

    private fun onInputCancel() =
            if (isSelected) {
                isSelected = false
                true
            } else {
                false
            }


    private fun onInputDone(actionId: Int, event: KeyEvent?) =
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val result = valueTransformer.stringToFloat(this, inputEditText.text.toString())

                if (result == null) {
                    inputEditText.selectAll()
                } else {
                    value = result
                    isSelected = false
                }

                playSoundEffect(CLICK)
                true
            } else {
                false
            }

    private fun onFocusChange(hasFocus: Boolean) {
        if (!hasFocus) {
            isSelected = false
        }
    }

    override fun setPressed(pressed: Boolean) {
        // remove all future events associated with changing the state
        handler.removeCallbacksAndMessages(hoverView)

        if (pressed == isPressed) return

        if (pressed && !isActivated) {
            hintTextView.alpha = 0f // hide hint immediately

            isActivated = true
            hintAnimator?.end()
        }

        super.setPressed(pressed)
        if (pressed) {
            showHoverView()
        } else {
            hideHoverView()
        }

        stateChangeListener?.onPressed(this, pressed)
    }

    private fun showHoverView() {
        hoverView.text = inputEditText.text
        invalidateHoverViewPosition()

        hintTextView.animate().alpha(0f).setDuration(ANIMATION_DURATION).start()
        hoverView.animate().alpha(1f).setDuration(ANIMATION_DURATION).start()

        if (hoverView.parent == null) {
            windowManager.addView(hoverView, hoverViewLayoutParams)
        }
    }

    private fun hideHoverView() {
        // animate with duration 0 to undo the previous animation
        hintTextView.animate().alpha(1f).setDuration(0).start()
        hoverView.animate().alpha(0f).setDuration(0).start()

        hoverView.parent?.let {
            windowManager.removeViewImmediate(hoverView)
        }
    }

    private fun invalidateHoverViewPosition() {
        hoverView.measure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY)
        inputAreaView.getLocationOnScreen(inputAreaPosition)

        hoverView.x = inputAreaPosition[0] +
                inputAreaView.width / 2f - hoverView.measuredWidth / 2f
        hoverView.y = inputAreaPosition[1] -
                hoverView.measuredHeight - hoverViewMargin.toFloat()
    }

    private fun invalidateValue() {
        if (isSelected) return

        inputEditText.setText(valueTransformer.floatToString(this, value))
        if (isPressed) {
            // get the value from input to take into account the maximum length
            hoverView.text = inputEditText.text
            invalidateHoverViewPosition()
        }
    }

    override fun onDetachedFromWindow() {
        scrollHandler.finishFling()
        isPressed = false

        super.onDetachedFromWindow()
    }

    interface ValueTransformer {

        /**
         * Transform value from string to float.
         *
         * @param value Manual entered value.
         * @return Converted internal value or {@code null}.
         * If null then input mode don't disable and value not apply.
         */
        fun stringToFloat(view: SwipePicker, value: String): Float? = with(view) {
            return value.toFloatOrNull() ?: this.value
        }

        /**
         * Transform value from float to string.
         *
         * @param value Internal value.
         * @return Converted display value or {@code null}.
         * If null then set empty value.
         */
        fun floatToString(view: SwipePicker, value: Float): String? = with(view) {
            return numberFormat.format(value)
        }
    }

    interface ScaleHandler {

        /**
         * Handling of stick the value to a scale.
         *
         * @param view SwipePicker of initiating event.
         * @param value The value for which is searched closest on the scale.
         * @return Closest value on scale, given the limit.
         */
        fun onStick(view: SwipePicker, value: Float): Float = with(view) {
            val scale = scale ?: return value

            val closestValue = scaleHelper.stickToScale(scale, step, value)
            // we are attracted to the limit if it is closer
            return floatArrayOf(closestValue, minValue, maxValue)
                    .reduce { r, i -> scaleHelper.getClosestValue(r, i, value) }
        }

        /**
         * Handling a swipe gesture.
         *
         * @param view SwipePicker of initiating event.
         * @param value The value from which need moved.
         * @param division The number of divisions that have moved.
         * @return The calculated value after the gesture processing which must be set to the view.
         */
        fun onSwipe(view: SwipePicker, value: Float, division: Int): Float = with(view) {
            var result = scaleHelper.moveToDivision(scale, step, value, division)

            if (!looped) return result

            val (from, to) = if (division < 0)
                Pair(minValue, maxValue) else Pair(maxValue, minValue)

            while (result !in minValue..maxValue) {
                var remainder = scaleHelper.getNumberDivisions(scale, step, from, result)
                val boundary = scaleHelper.moveToDivision(scale, step, result, -remainder)

                if (boundary != from) {
                    if (Math.abs(remainder) == 1) {
                        return from
                    } else {
                        remainder -= division.sign
                    }
                }

                val offset = remainder - division.sign
                result = scaleHelper.moveToDivision(scale, step, to, offset)
            }
            return result
        }
    }

    interface OnStateChangeListener {

        /**
         * The listener is called every time the activation state changes.
         *
         * @param view SwipePicker of initiating event.
         * @param isActivated Current activated state.
         */
        fun onActivated(view: SwipePicker, isActivated: Boolean) {
            /* empty for usability */
        }

        /**
         * The listener is called every time the selection state changes.
         *
         * @param view SwipePicker of initiating event.
         * @param isSelected Current selected state.
         */
        fun onSelected(view: SwipePicker, isSelected: Boolean) {
            /* empty for usability */
        }

        /**
         * The listener is called every time the pressed state changes.
         *
         * @param view SwipePicker of initiating event.
         * @param isPressed Current pressed state.
         */
        fun onPressed(view: SwipePicker, isPressed: Boolean) {
            /* empty for usability */
        }
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

    private class SavedState : BaseSavedState {

        var allowDeactivate = true
        var allowFling = true
        var manualInput = true
        var scale: List<Float>? = null
        var minValue = -Float.MAX_VALUE
        var maxValue = Float.MAX_VALUE
        var step = 1f
        var value = 1f
        var sticky = false
        var looped = false
        var activated = false
        var selected = false

        constructor(superState: Parcelable) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            allowDeactivate = parcel.readByte() != 0.toByte()
            allowFling = parcel.readByte() != 0.toByte()
            manualInput = parcel.readByte() != 0.toByte()
            @Suppress("UNCHECKED_CAST")
            scale = parcel.readSerializable() as List<Float>?
            minValue = parcel.readFloat()
            maxValue = parcel.readFloat()
            step = parcel.readFloat()
            value = parcel.readFloat()
            sticky = parcel.readByte() != 0.toByte()
            looped = parcel.readByte() != 0.toByte()
            activated = parcel.readByte() != 0.toByte()
            selected = parcel.readByte() != 0.toByte()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            super.writeToParcel(parcel, flags)
            parcel.writeByte(if (allowDeactivate) 1 else 0)
            parcel.writeByte(if (allowFling) 1 else 0)
            parcel.writeByte(if (manualInput) 1 else 0)
            parcel.writeSerializable(scale as Serializable?)
            parcel.writeFloat(minValue)
            parcel.writeFloat(maxValue)
            parcel.writeFloat(step)
            parcel.writeFloat(value)
            parcel.writeByte(if (sticky) 1 else 0)
            parcel.writeByte(if (looped) 1 else 0)
            parcel.writeByte(if (activated) 1 else 0)
            parcel.writeByte(if (selected) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
            scrollHandler.startFrom(value)

            // Use it because onShowPress(MotionEvent e) causes wrong behavior.
            handler.postAtTime({ isPressed = true },
                    hoverView, event.downTime + PRESS_DELAY_SHOW)

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isPressed) {
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
            scrollHandler.scrollTo(e2.x - e1.x)
            return isPressed
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (allowFling) {
                val velocity = (velocityX / VELOCITY_DOWNSCALE).toInt()
                if (Math.abs(velocity) > VELOCITY_THRESHOLD) {
                    scrollHandler.startFrom(value).flingWith(velocity)
                    return true
                }
            }
            return false
        }
    }

    private inner class ScrollHandler {

        private val swipeThreshold = resources.getDimensionPixelSize(R.dimen.swipePicker_swipeThreshold)

        private val scroller by lazy { createScroller() }
        private val animator by lazy { createAnimator() }

        private var initialValue = 0f
        private var lastDivision = 0

        fun startFrom(value: Float): ScrollHandler {
            finishFling()

            initialValue = value
            lastDivision = 0

            return this
        }

        fun scrollTo(distance: Float) {
            val division = Math.round(distance / swipeThreshold)

            if (lastDivision != division) {
                isPressed = true // scroll started, show a pop-up immediately
                value = scaleHandler.onSwipe(this@SwipePicker, initialValue, division)
            }

            lastDivision = division
        }

        fun flingWith(velocity: Int) {
            scroller.fling(0, 0, velocity, 0,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0)

            animator.duration = scroller.duration.toLong()
            animator.start()
        }

        fun finishFling() {
            if (allowFling) {
                scroller.forceFinished(true)
            }
        }

        private fun createScroller() = Scroller(context, null, true)

        private fun createAnimator(): ValueAnimator {
            val result = ValueAnimator.ofFloat(0f, 1f)

            result.addUpdateListener {
                if (scroller.isFinished) {
                    animator.cancel()
                } else {
                    scroller.computeScrollOffset()
                    scrollTo(scroller.currX.toFloat())

                    if (!looped && (value == minValue || value == maxValue)) {
                        finishFling()
                    }
                }
            }

            result.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (windowVisibility == View.VISIBLE) {
                        handler.postAtTime({
                            playSoundEffect(CLICK)
                            isPressed = false
                        }, hoverView, SystemClock.uptimeMillis() + PRESS_DELAY_HIDE)
                    }
                }
            })

            return result
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
