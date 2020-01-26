package one.xcorp.swipepicker.date

import android.text.InputFilter
import android.text.Spanned

class StringInputFilter(vararg values: String) : InputFilter {

    private val input = StringBuilder()
    private val legalValues = values

    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        input.replace(0, input.length, dest.toString())
        input.replace(dstart, dend, source.subSequence(start, end).toString())

        val coincidence = legalValues.any { it.startsWith(input.toString(), true) }
        return if (coincidence) null else ""
    }
}
