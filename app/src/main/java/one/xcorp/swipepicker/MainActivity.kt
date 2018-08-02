package one.xcorp.swipepicker

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import one.xcorp.swipepicker.date.StringInputFilter
import one.xcorp.swipepicker.time.LocalTime
import one.xcorp.swipepicker.time.TimeInputFilter
import one.xcorp.swipepicker.time.TimeOfDay
import one.xcorp.widget.swipepicker.SwipePicker
import java.lang.String.valueOf
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        val DAWN_START = TimeUnit.HOURS.toMillis(5)
        val DAWN_DURATION = TimeUnit.HOURS.toMillis(4)
        val SUNSET_START = TimeUnit.HOURS.toMillis(19)
        val SUNSET_DURATION = TimeUnit.HOURS.toMillis(4)
    }

    private val colorEvaluator = ColorEvaluator()
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configureScale()
        configureDate()
        configureTime()
    }

    private fun configureScale() {
        val negativeColor = ContextCompat.getColor(this, R.color.negative_background)

        scale.setOnValueChangeListener { _, new ->
            scale.setTintColor(if (new < 0) negativeColor else Color.TRANSPARENT)
        }
    }

    private fun configureDate() {
        val date = Calendar.getInstance()
        val stateListener = DateStateChangeListener()

        day.value = (date.get(Calendar.DATE)).toFloat()
        day.setOnStateChangeListener(stateListener)

        configureMonth(date, stateListener)

        year.value = (date.get(Calendar.YEAR)).toFloat()
        year.setOnStateChangeListener(stateListener)
        year.setOnValueChangeListener { _, _ ->
            if (month.value == (Calendar.FEBRUARY + 1).toFloat()) {
                invalidateMaxDay()
            }
        }

        invalidateMaxDay()
    }

    private fun configureMonth(date: Calendar, stateListener: SwipePicker.OnStateChangeListener) {
        val monthsNumbers = (1..12).map(::valueOf).toTypedArray()
        val monthsValues = resources.getStringArray(R.array.months)

        month.value = (date.get(Calendar.MONTH) + 1).toFloat()
        // Instead of text InputType, it's better to use number input. In this case done for example.
        month.addInputFilter(StringInputFilter(*monthsNumbers, *monthsValues))
        // Convert the value to and from the string
        month.setValueTransformer(object : SwipePicker.ValueTransformer {
            override fun stringToFloat(view: SwipePicker, value: String): Float? {
                val index = monthsValues.indexOfFirst { it.startsWith(value, true) }
                val number = (index + 1).toFloat()
                // If string empty or value not found then set month by number or don't change value.
                val coincidence = number > 0 && !value.isEmpty()
                return if (coincidence) number else super.stringToFloat(view, value)
            }

            override fun floatToString(view: SwipePicker, value: Float): String? {
                return monthsValues[value.toInt() - 1]
            }
        })
        month.setOnStateChangeListener(stateListener)
        month.setOnValueChangeListener { _, new ->
            setSeason(new)
            invalidateMaxDay()
        }
    }

    private fun configureTime() {
        // Set current time
        time.value = LocalTime().millis.toFloat()
        // Validate user input
        time.addInputFilter(TimeInputFilter(true))
        // Convert the value to and from the string
        time.setValueTransformer(object : SwipePicker.ValueTransformer {
            override fun stringToFloat(view: SwipePicker, value: String): Float? {
                val result = LocalTime.parse(completeTime(value), "HH:mm")
                if (result == null) {
                    showMessage(R.string.incorrect_input)
                }
                return result?.millis?.toFloat()
            }

            override fun floatToString(view: SwipePicker, value: Float): String? {
                return LocalTime(value.toLong()).toString("HH:mm")
            }
        })
        time.setOnStateChangeListener(object : SwipePicker.OnStateChangeListener {
            override fun onActivated(view: SwipePicker, isActivated: Boolean) {
                timeLabel.visibility = if (isActivated) VISIBLE else INVISIBLE
            }
        })
        setTimeOfDay(time.value) // call for init value
        time.setOnValueChangeListener { _, new -> setTimeOfDay(new) }
    }

    private fun invalidateMaxDay() {
        val date = Calendar.getInstance()

        date.set(Calendar.MONTH, month.value.toInt() - 1)
        date.set(Calendar.YEAR, year.value.toInt())

        day.maxValue = date.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
    }

    private fun setSeason(value: Float) {
        val (labelId, colorId) = when (value) {
            in 9.0f..11.0f -> Pair(R.string.autumn, R.color.autumn_background)
            in 3.0f..5.0f -> Pair(R.string.spring, R.color.spring_background)
            in 6.0f..8.0f -> Pair(R.string.summer, R.color.summer_background)
            else -> Pair(R.string.winter, R.color.winter_background)
        }

        val color = ContextCompat.getColor(this, colorId)

        monthLabel.setText(labelId)
        monthLabel.setTextColor(color)

        setDateTintColor(color)
    }

    private fun setDateTintColor(@ColorInt color: Int) {
        day.setTintColor(color)
        month.setTintColor(color)
        year.setTintColor(color)
    }

    private fun setTimeOfDay(value: Float) {
        val selectedTime = LocalTime(value.toLong())
        val timeOfDay = selectedTime.getTimeOfDay(
                DAWN_START, DAWN_DURATION, SUNSET_START, SUNSET_DURATION)

        val dayColor = ContextCompat.getColor(this, R.color.day_background)
        val nightColor = ContextCompat.getColor(this, R.color.night_background)

        val fraction = (value - timeOfDay.start) / timeOfDay.duration()
        val (color, label) = when (timeOfDay.type) {
            TimeOfDay.Type.DAWN ->
                Pair(colorEvaluator.evaluate(fraction, nightColor, dayColor), R.string.dawn)
            TimeOfDay.Type.DAY -> Pair(dayColor, R.string.day)
            TimeOfDay.Type.SUNSET ->
                Pair(colorEvaluator.evaluate(fraction, dayColor, nightColor), R.string.sunset)
            TimeOfDay.Type.NIGHT -> Pair(nightColor, R.string.night)
        }

        time.setTintColor(color)
        timeLabel.setTextColor(color)

        timeLabel.text = getString(label).toLowerCase()
    }

    private fun completeTime(value: String): String {
        return when (value.length) {
            1 -> "$value:00"
            2 -> if (value.last() == ':') value + "00" else "$value:00"
            3 -> if (value.last() == ':') value + "00" else value + "0"
            4 -> if (value[1] == ':') value else value + "0"
            else -> value
        }
    }

    private fun showMessage(@StringRes resId: Int) {
        toast?.cancel()
        toast = Toast.makeText(this, resId, Toast.LENGTH_LONG)
        toast?.show()
    }

    private inner class DateStateChangeListener : SwipePicker.OnStateChangeListener {

        override fun onActivated(view: SwipePicker, isActivated: Boolean) {
            day.isActivated = isActivated
            month.isActivated = isActivated
            year.isActivated = isActivated

            if (isActivated) {
                setSeason(month.value)
                monthLabel.visibility = VISIBLE
            } else {
                setDateTintColor(Color.TRANSPARENT)
                monthLabel.visibility = INVISIBLE
            }
        }
    }
}