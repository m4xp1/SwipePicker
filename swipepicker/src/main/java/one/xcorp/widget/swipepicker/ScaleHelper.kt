package one.xcorp.widget.swipepicker

import java.math.RoundingMode

internal class ScaleHelper {

    /**
     * Find the closest value to the specified values.
     *
     * @param left Value in left side.
     * @param right Value in right side.
     * @param value The value for which to search for the closest.
     * @return Closest value left or right. The left value in priority.
     */
    fun getClosestValue(left: Float, right: Float, value: Float) =
            if (Math.abs(value - left) <= Math.abs(right - value)) left else right

    /**
     * Constructed the scale to the value and determines the closest value on the scale.
     *
     * @param boundary The value from which the scale will be constructed to the specified value.
     * @param step Step with which the scale is constructed, if step equal 0 then return boundary.
     * @param value The value for which the closest on the scale will be searched.
     * @return Closest value on scale, the closest values to the boundary in priority.
     */
    fun getClosestOnScale(boundary: Float, step: Float, value: Float): Float {
        if (step == 0f) return boundary

        // rounding to the closest to the boundary
        val distance = ((value - boundary) / step)
                .toBigDecimal().setScale(0, RoundingMode.HALF_DOWN).toInt()
        val offset = distance.toBigDecimal() * step.toBigDecimal()

        return (boundary.toBigDecimal() + offset).toFloat()
    }

    /**
     * Attracts a value to the scale.
     *
     * @param scale The scale in which you must move.
     * If {@code null} then do nothing and return specified value.
     * @param step Step with which the scale is constructed, if step equal 0 then return
     * boundary if the value lies outside the boundary of the scale.
     * @param value The value for which the closest on the scale will be searched.
     * @return Closest value on scale, the closest values to the scale boundary in priority.
     */
    fun stickToScale(scale: List<Float>?, step: Float, value: Float): Float {
        if (scale == null) return value
        // find value on the scale
        val index = scale.binarySearch(value)
        if (index >= 0) return value
        // value does not belong to the scale, we find the closest
        val insertion = -index - 1
        return when (insertion) {
        // outside value from left side
            0 -> getClosestOnScale(scale.first(), step, value)
        // outside value from right side
            scale.size -> getClosestOnScale(scale.last(), step, value)
        // value on scale
            else -> getClosestValue(scale[insertion - 1], scale[insertion], value)
        }
    }

    /**
     * Move by scale from the value to the specified division.
     *
     * @param scale The scale in which you must move.
     * Сan be {@code null} if you only need to take into account the step.
     * @param step Step with which you need to move.
     * If is 0 then movement outside the scale is forbidden.
     * @param value The value from which need moved.
     * @param division The number of divisions that have moved.
     * @return The calculated value for specified division.
     */
    fun moveToDivision(scale: List<Float>?, step: Float, value: Float, division: Int): Float {
        if (division == 0) return value
        // the scale is not specified, calculate the value based on the step
        if (scale == null) return moveByStep(step, value, division)
        // movement outside the scale without crossing it
        if ((value < scale.first() && division < 0) || (value > scale.last() && division > 0)) {
            return moveByScaleOutside(scale, step, value, division)
        }
        // movement outside the scale with a possible intersection of scale
        if (value !in scale.first()..scale.last()) {
            return moveByScaleInside(scale, step, value, division)
        }
        // Finding the index of the value on the scale. If the value is not found
        // returns the index of the nearest value taking into account the direction of the gesture.
        var index: Int = scale.binarySearch(value)
        if (index < 0) {
            val offset = if (division < 0) 1 else 2
            index = -(index + offset)
        }
        // the value index lies on the scale, we move along it
        return moveByScale(scale, step, index, division)
    }

    private fun moveByStep(step: Float, value: Float, division: Int) =
            (value.toBigDecimal() + division.toBigDecimal() * step.toBigDecimal()).toFloat()

    private fun moveByScaleOutside(scale: List<Float>, step: Float, value: Float, division: Int): Float {
        // outward movement is impossible
        if (step == 0f) return value

        val closestValue: Float
        val offset: Int

        // Attract to the value on the scale
        if (division < 0) { // direction right to left
            closestValue = getClosestOnScale(scale.first(), step, value)
            offset = if (closestValue < value) 1 else 0
        } else { // direction left to right
            closestValue = getClosestOnScale(scale.last(), step, value)
            offset = if (closestValue > value) -1 else 0
        }
        // calculate the value based on the step from closest value
        return moveByStep(step, closestValue, division + offset)
    }

    private fun moveByScaleInside(scale: List<Float>, step: Float, value: Float, division: Int): Float {
        val boundaryIndex: Int
        val offset: Int

        if (division > 0) {  // direction left to right
            boundaryIndex = 0
            offset = -1
        } else {  // direction right to left
            boundaryIndex = scale.lastIndex
            offset = 1
        }
        // if step 0 means we are attracted to the boundary of the scale and move along it
        if (step == 0f) return moveByScale(scale, step, boundaryIndex, division + offset)

        val distance = ((value - scale[boundaryIndex]) / step)
        // the number of divisions up to the scale of values remaining after the move
        val remainder = division + distance
                .toBigDecimal().setScale(0, RoundingMode.UP).toInt()
        // move from the scale outwards or along it, depending on the sign of the remainder
        return moveByScale(scale, step, boundaryIndex, remainder)
    }

    private fun moveByScale(scale: List<Float>, step: Float, index: Int, division: Int): Float {
        val destination = index + division

        return when {
        // move on the scale outwards to the left
            destination < 0 ->
                moveByStep(step, scale.first(), destination)
        // move on the scale outwards to the right
            destination > scale.lastIndex ->
                moveByStep(step, scale.last(), (destination - scale.lastIndex))
        // move on the scale
            else -> scale[destination]
        }
    }
}