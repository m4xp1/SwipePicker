package one.xcorp.widget.swipepicker

import android.animation.*
import android.widget.TextView

internal class HintAnimation(private val view: TextView) {

    val isRunning
        get() = animatorSet.isRunning

    private val animatorSet = AnimatorSet()

    fun to(y: Float, scale: Float): HintAnimation {
        val animMove = ObjectAnimator.ofFloat(view, "translationY", y)
        val animScale = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("scaleX", scale),
                PropertyValuesHolder.ofFloat("scaleY", scale))

        animatorSet.playTogether(animMove, animScale)
        return this
    }

    fun setDuration(duration: Long): HintAnimation {
        animatorSet.duration = duration
        return this
    }

    fun addStartListener(listener: () -> Unit): HintAnimation {
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                listener()
            }
        })
        return this
    }

    fun addEndListener(listener: () -> Unit): HintAnimation {
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                listener()
            }
        })
        return this
    }

    fun start() {
        animatorSet.start()
    }

    fun end() {
        animatorSet.end()
    }

    fun cancel() {
        animatorSet.removeAllListeners()
        animatorSet.cancel()
    }
}