package one.xcorp.swipepicker

import android.graphics.Color
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import one.xcorp.swipepicker.time.LocalTime
import one.xcorp.swipepicker.time.TimeInputFilter
import one.xcorp.widget.swipepicker.SwipePicker

class MainActivity : AppCompatActivity() {

    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configureScale()
        configureTime()
    }

    private fun configureScale() {
        val negativeBackgroundColor =
                ContextCompat.getColor(this, R.color.background_negative)

        scale.setOnValueChangeListener {
            if (it < 0) {
                scale.hoverView.colorTint = negativeBackgroundColor
                scale.setBackgroundInputTint(negativeBackgroundColor)
            } else {
                scale.hoverView.colorTint = Color.TRANSPARENT
                scale.backgroundInputTint = null
            }
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
                    showMessage(R.string.message_incorrect_input)
                }
                return result?.millis?.toFloat()
            }

            override fun floatToString(view: SwipePicker, value: Float): String? {
                return LocalTime(value.toLong()).toString("HH:mm")
            }
        })
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
}