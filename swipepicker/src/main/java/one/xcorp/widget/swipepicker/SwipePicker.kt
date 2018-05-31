package one.xcorp.widget.swipepicker

import android.content.Context
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.widget.TextViewCompat
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView

class SwipePicker : GridLayout {

    private val gestureDetector: GestureDetectorCompat

    private val hintTextView: TextView
    private val backgroundView: View
    private val inputEditText: EditText

    private val density: Float = resources.displayMetrics.density

    var scaleValues: FloatArray? = null
    var minValue: Float = 0f
    var maxValue: Float = 10f
    var stepSize: Float = 1f
    var value: Float = 0f
    var restrictions: Int = Restrictions.LOWER
    var manualInput: Boolean = true

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

        val view = inflate(context, R.layout.swipe_picker, this)
        hintTextView = view.findViewById(android.R.id.hint)
        backgroundView = view.findViewById(android.R.id.background)
        inputEditText = view.findViewById(android.R.id.input)

        obtainStyledAttributes(context, attrs, defStyleAttr, defStyleRes)
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
                        R.style.TextAppearance_XcoRp_Widget_SwipePicker_Hint))
        TextViewCompat.setTextAppearance(inputEditText,
                typedArray.getResourceId(R.styleable.SwipePicker_inputTextAppearance,
                        R.style.TextAppearance_XcoRp_Widget_SwipePicker_Input))
        backgroundView.background = typedArray.getDrawable(
                R.styleable.SwipePicker_android_background); background = null
        isActivated = typedArray.getBoolean(
                R.styleable.SwipePicker_android_state_activated, false)
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
        value = typedArray.getFloat(R.styleable.SwipePicker_value, minValue)
        restrictions = typedArray.getInt(R.styleable.SwipePicker_restrictions, restrictions)
        manualInput = typedArray.getBoolean(R.styleable.SwipePicker_manualInput, manualInput)
        inputEditText.inputType = typedArray.getInt(
                R.styleable.SwipePicker_inputType, InputType.TYPE_CLASS_NUMBER)
        inputEditText.filters = arrayOf(InputFilter.LengthFilter(
                typedArray.getInt(R.styleable.SwipePicker_android_maxLength,
                        resources.getInteger(R.integer.swipePicker_maxLength))))

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

    private class GestureListener : GestureDetector.SimpleOnGestureListener()
}