package one.xcorp.widget.swipepicker

import java.math.RoundingMode
import java.math.RoundingMode.*
import kotlin.math.sign

internal class ScaleHelper {

    /**
     * Calculate divisions between to values.
     *
     * @param step Step of changing the values on the scale.
     * If is 0 then meaning between values can be no more than one division.
     * @param from First value.
     * @param to Second value.
     * @param rounding Specifies the offset by modulo if the number of divisions is not exact.
     * @return Number divisions between values with direction sign.
     */
    fun getNumberDivisions(step: Float, from: Float, to: Float, rounding: RoundingMode = UP): Int {
        return if (step == 0f) {
            (to - from).sign.toInt() // single division with direction
        } else {
            ((to - from) / step).toBigDecimal().setScale(0, rounding).toInt()
        }
    }

    /**
     * Calculate divisions on scale between to values.
     *
     * @param scale The scale of values for which the distance in divisions is determined.
     * Сan be {@code null} if you only need to take into account the step.
     * @param step The step of changing values outside the scale.
     * If is 0 then meaning between outside values is zero. Between boundary and outside value 1.
     * @param from First value.
     * @param to Second value.
     * @return Number divisions between values with direction sign.
     */
    fun getNumberDivisions(scale: List<Float>?, step: Float, from: Float, to: Float): Int {
        if (scale == null) return getNumberDivisions(step, from, to)
        // determine the direction of move
        val direction = (to - from).sign.toInt()
        // the direction is inverted to capture the maximum number of divisions
        val fromIndex = findIndexOnScale(scale, step, from, direction)
        val toIndex = findIndexOnScale(scale, step, to, -direction)
        // calculate the number of divisions between values
        return toIndex - fromIndex
    }

    private fun findIndexOnScale(scale: List<Float>, value: Float, direction: Int): Int {
        require(value in scale.first()..scale.last()) { "The value is outside the scale." }

        var index = scale.binarySearch(value)
        // value not found return closest value index taking into account the direction
        if (direction != 0 && index < 0) {
            // if direction less 0 then return right value index otherwise left
            index = -(index + if (direction < 0) 1 else 2)
        }
        return index // if direction equals 0 then return element insertion position -(index + 1)
    }

    private fun findIndexOnScale(scale: List<Float>, step: Float, value: Float, direction: Int): Int {
        if (value in scale.first()..scale.last()) {
            return findIndexOnScale(scale, value, direction)
        }
        // if direction less 0 then return right value index otherwise left
        val (index, boundary, rounding) = if (value < scale.first()) { // on left
            val rounding = if (direction < 0) DOWN else UP
            Triple(0, scale.first(), rounding)
        } else { // on right
            val rounding = if (direction < 0) UP else DOWN
            Triple(scale.lastIndex, scale.last(), rounding)
        }
        // calculate index for outside value
        return index + getNumberDivisions(step, boundary, value, rounding)
    }

    /**
     * Find the closest value to the specified values.
     *
     * @param first First value.
     * @param second Second value.
     * @param value The value for which to search for the closest.
     * @return Closest value first or second. The first value in priority.
     */
    fun getClosestValue(first: Float, second: Float, value: Float) =
            if (Math.abs(value - first) <= Math.abs(second - value)) first else second

    /**
     * Constructed the scale to the value and determines the closest value on the scale.
     *
     * @param boundary The value from which the scale will be constructed to the specified value.
     * @param step Step with which the scale is constructed, if step equal 0 then return boundary.
     * @param value The value for which the closest on the scale will be searched.
     * @return Closest value on scale, the closest values to the boundary in priority.
     */
    fun getClosestOutside(boundary: Float, step: Float, value: Float): Float {
        if (step == 0f) return boundary

        // rounding to the closest to the boundary
        val divisions = getNumberDivisions(step, boundary, value, HALF_DOWN)
        val offsetValue = divisions.toBigDecimal() * step.toBigDecimal()

        return (boundary.toBigDecimal() + offsetValue).toFloat()
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
        // check whether the value is outside
        if (value < scale.first()) { // outside value from left side
            return getClosestOutside(scale.first(), step, value)
        } else if (value > scale.last()) { // outside value from right side
            return getClosestOutside(scale.last(), step, value)
        }
        // value on the scale, we find its index
        var index = findIndexOnScale(scale, value, 0)
        if (index >= 0) return value else index = -(index + 1)
        // value is absent on the scale, we return the closest
        return getClosestValue(scale[index - 1], scale[index], value)
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
        val index = findIndexOnScale(scale, value, division.sign)
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
            closestValue = getClosestOutside(scale.first(), step, value)
            offset = if (closestValue < value) 1 else 0
        } else { // direction left to right
            closestValue = getClosestOutside(scale.last(), step, value)
            offset = if (closestValue > value) -1 else 0
        }
        // calculate the value based on the step from closest value
        return moveByStep(step, closestValue, division + offset)
    }

    private fun moveByScaleInside(scale: List<Float>, step: Float, value: Float, division: Int): Float {
        // division > 0 direction left to right otherwise right to left
        val (boundaryIndex, offset) = if (division > 0) Pair(0, -1) else Pair(scale.lastIndex, 1)
        // if step 0 means we are attracted to the boundary of the scale and move along it
        if (step == 0f) return moveByScale(scale, step, boundaryIndex, division + offset)
        // the number of divisions up to the scale of values remaining after the move
        val remainder = division + getNumberDivisions(step, scale[boundaryIndex], value)
        // move from the scale outwards or along it, depending on the sign of the remainder
        return moveByScale(scale, step, boundaryIndex, remainder)
    }

    private fun moveByScale(scale: List<Float>, step: Float, index: Int, division: Int): Float {
        val destination = index + division

        if (destination < 0) { // move on the scale outwards to the left
            return moveByStep(step, scale.first(), destination)
        } else if (destination > scale.lastIndex) { // move on the scale outwards to the right
            return moveByStep(step, scale.last(), (destination - scale.lastIndex))
        }
        // move on the scale
        return scale[destination]
    }
}