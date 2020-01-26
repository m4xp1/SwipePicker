package one.xcorp.swipepicker

import android.animation.TypeEvaluator
import android.graphics.Color

class ColorEvaluator : TypeEvaluator<Int> {

    override fun evaluate(fraction: Float, start: Int, end: Int): Int {
        val startA = Color.alpha(start)
        val startR = Color.red(start)
        val startG = Color.green(start)
        val startB = Color.blue(start)

        val aDelta = ((Color.alpha(end) - startA) * fraction).toInt()
        val rDelta = ((Color.red(end) - startR) * fraction).toInt()
        val gDelta = ((Color.green(end) - startG) * fraction).toInt()
        val bDelta = ((Color.blue(end) - startB) * fraction).toInt()

        return Color.argb(startA + aDelta,
                startR + rDelta, startG + gDelta, startB + bDelta)
    }
}
