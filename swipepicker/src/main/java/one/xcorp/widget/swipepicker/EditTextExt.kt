package one.xcorp.widget.swipepicker

import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.RESULT_UNCHANGED_SHOWN
import android.widget.EditText

internal fun EditText.showKeyboard() {
    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.showSoftInput(this, RESULT_UNCHANGED_SHOWN)
}

internal fun EditText.hideKeyBoard() {
    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(this.windowToken, RESULT_UNCHANGED_SHOWN)
}