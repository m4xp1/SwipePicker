package one.xcorp.widget.swipepicker

import android.content.Context
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SoundEffectConstants.CLICK
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale.US

class SwipePicker : GridLayout {

    var scaleValues: FloatArray? = null
    var minValue = 1f
    var maxValue = 10f
    var stepSize = 1f
    var defaultValue = 1f
    var value = 1f
        set(value) {
            field = value
            invalidateValue()
        }
    var restrictions = Restrictions.LOWER
    var manualInput = true
        set(enable) {
            if (!enable) {
                isSelected = false
            }
            field = enable
        }


    private companion object {
        const val ANIMATION_DURATION = 200L
    }

    private val gestureDetector: GestureDetectorCompat

    private val hoverView by lazy { HoverView(context, null, hoverViewStyle) }
    private val hintTextView by lazy { findViewById<TextView>(android.R.id.hint) }
    private val backgroundView by lazy { findViewById<View>(android.R.id.background) }
    private val inputEditText by lazy { findViewById<EditText>(android.R.id.input) }

    private var hoverViewStyle = 0
    private var activated = false
    private val fontScale: Float

    private var hintAnimation: LabelAnimation? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.swipePicker)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, R.style.XcoRp_Widget_SwipePicker)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr) {
        gestureDetector = GestureDetectorCompat(context, GestureListener())
        gestureDetector.setIsLongpressEnabled(false)

        clipChildren = false
        orientation = VERTICAL

        inflate(context, R.layout.swipe_picker, this)
        obtainStyledAttributes(context, attrs, defStyleAttr, defStyleRes)

        inputEditText.setOnEditorActionListener(::onInputDone)
        backgroundView.setOnTouchListener(::onTouch)

        fontScale = inputEditText.textSize / hintTextView.textSize
    }

    private fun obtainStyledAttributes(
            context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.SwipePicker, defStyleAttr, defStyleRes)

        minimumWidth = typedArray.getDimensionPixelSize(R.styleable.SwipePicker_android_minWidth,
                resources.getDimensionPixelSize(R.dimen.swipePicker_minWidth))
        val padding = typedArray.getDimensionPixelSize(R.styleable.SwipePicker_android_padding,
                resources.getDimensionPixelSize(R.dimen.swipePicker_padding))
        hintTextView.setPadding(padding, padding, padding, padding)
        inputEditText.setPadding(padding, padding, padding, padding)
        setPadding(0, 0, 0, 0)
        TextViewCompat.setTextAppearance(hintTextView,
                typedArray.getResourceId(R.styleable.SwipePicker_hintTextAppearance,
                        R.style.XcoRp_TextAppearance_SwipePicker_Hint))
        TextViewCompat.setTextAppearance(inputEditText,
                typedArray.getResourceId(R.styleable.SwipePicker_inputTextAppearance,
                        R.style.XcoRp_TextAppearance_SwipePicker_Input))
        ViewCompat.setBackground(backgroundView, typedArray.getDrawable(
                R.styleable.SwipePicker_android_background))
        ViewCompat.setBackground(this, null)
        activated = typedArray.getBoolean(
                R.styleable.SwipePicker_android_state_activated, false)
        ViewCompat.setTranslationZ(hintTextView, 1f)
        hintTextView.text = typedArray.getString(R.styleable.SwipePicker_android_hint)
        if (typedArray.hasValue(R.styleable.SwipePicker_scaleValues)) {
            scaleValues = obtainScaleValues(
                    typedArray.getResourceId(R.styleable.SwipePicker_scaleValues, 0))
        }
        minValue = typedArray.getFloat(
                R.styleable.SwipePicker_minValue, scaleValues?.first() ?: minValue)
        maxValue = typedArray.getFloat(
                R.styleable.SwipePicker_maxValue, scaleValues?.last() ?: maxValue)
        if (!isInEditMode && minValue >= maxValue) {
            throw IllegalArgumentException("The minimum value must be less than the maximum.")
        }
        stepSize = typedArray.getFloat(R.styleable.SwipePicker_stepSize, stepSize)
        defaultValue = typedArray.getFloat(R.styleable.SwipePicker_value, minValue)
        value = defaultValue
        restrictions = typedArray.getInt(R.styleable.SwipePicker_restrictions, restrictions)
        manualInput = typedArray.getBoolean(R.styleable.SwipePicker_manualInput, manualInput)
        inputEditText.inputType = typedArray.getInt(
                R.styleable.SwipePicker_inputType, InputType.TYPE_CLASS_NUMBER)
        inputEditText.filters = arrayOf(InputFilter.LengthFilter(
                typedArray.getInt(R.styleable.SwipePicker_android_maxLength,
                        resources.getInteger(R.integer.swipePicker_maxLength))))
        hoverViewStyle = typedArray.getResourceId(R.styleable.SwipePicker_hoverViewStyle,
                R.style.XcoRp_Style_SwipePicker_HoverView)

        typedArray.recycle()
    }

    fun getHint(): CharSequence {
        return hintTextView.text
    }

    fun setHint(text: CharSequence) {
        hintTextView.text = text
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            isActivated = activated
            hintAnimation?.end()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        isSelected = true
        playSoundEffect(CLICK)
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (!inputEditText.isEnabled) {
            return backgroundView.dispatchTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onTouch(v: View, event: MotionEvent): Boolean {
        val result = gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            playSoundEffect(CLICK)
        }
        return result
    }

    override fun isActivated(): Boolean {
        return activated
    }

    override fun setActivated(activated: Boolean) {
        hintAnimation?.cancel()
        hintAnimation = LabelAnimation(hintTextView).setDuration(ANIMATION_DURATION)

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
                val gravityOffset = (hintTextView.height - scaledHeight) / 2

                animation.to(scaledHeight + gravityOffset + inputEditText.top, fontScale)
                animation.addStartListener {
                    inputEditText.visibility = View.INVISIBLE
                    super.setActivated(activated)
                }.start()
            }
        }

        value = defaultValue
        this.activated = activated
    }

    override fun setSelected(selected: Boolean) {
        if (!manualInput || isSelected == selected) {
            return
        }

        if (!isActivated) {
            isActivated = true
        }

        super.setSelected(selected)
        if (hintAnimation?.isRunning == true) {
            hintAnimation?.addEndListener { setInputEnabled(isSelected) }
        } else {
            setInputEnabled(selected)
        }
    }

    private fun invalidateValue() {
        val text = NumberFormat.getInstance(US)
                .apply { isGroupingUsed = false }.format(value)
        if (isHovered) {
            hoverView.text = text
        }
        inputEditText.setText(text)
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
            inputEditText.hideKeyBoard()
            inputEditText.setSelection(0)
            inputEditText.clearFocus()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onInputDone(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            value = view.text.toString().toFloatOrNull() ?: value
            isSelected = false
            playSoundEffect(CLICK)
            return true
        }
        return false
    }

    private fun EditText.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(this, InputMethodManager.RESULT_UNCHANGED_SHOWN)
    }

    private fun EditText.hideKeyBoard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(this.windowToken, InputMethodManager.RESULT_UNCHANGED_SHOWN)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
            return true
        }

        override fun onShowPress(e: MotionEvent) {

        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            isSelected = true
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            isActivated = false
            return true
        }
    }
}