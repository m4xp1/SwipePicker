package one.xcorp.widget.swipepicker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.TextView

internal class LabelAnimation(private val view: TextView) {

    val isRunning
        get() = animatorSet.isRunning

    private val animatorSet = AnimatorSet()

    fun to(y: Float, textSize: Float): LabelAnimation {
        val animY = ObjectAnimator.ofFloat(view, "translationY", y)
        val animTextSize = ObjectAnimator.ofFloat(view, "textSize",
                view.textSize / view.resources.displayMetrics.scaledDensity, textSize)

        animatorSet.playTogether(animY, animTextSize)
        return this
    }

    fun setDuration(duration: Long): LabelAnimation {
        animatorSet.duration = duration
        return this
    }

    fun addStartListener(listener: () -> Unit): LabelAnimation {
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                listener()
            }
        })
        return this
    }

    fun addEndListener(listener: () -> Unit): LabelAnimation {
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