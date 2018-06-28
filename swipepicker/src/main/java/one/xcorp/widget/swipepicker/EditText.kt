package one.xcorp.widget.swipepicker

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager

internal class EditText(context: Context?, attrs: AttributeSet?) :
        android.support.v7.widget.AppCompatEditText(context, attrs) {

    private var backPressedListener: (() -> Boolean)? = null

    fun setOnBackPressedListener(listener: (() -> Boolean)?) {
        backPressedListener = listener
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            return backPressedListener?.invoke() ?: super.dispatchKeyEvent(event)
        }

        return super.dispatchKeyEvent(event)
    }
}

internal fun EditText.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.showSoftInput(this, InputMethodManager.RESULT_UNCHANGED_SHOWN)
}

internal fun EditText.hideKeyBoard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(this.windowToken, InputMethodManager.RESULT_UNCHANGED_SHOWN)
}