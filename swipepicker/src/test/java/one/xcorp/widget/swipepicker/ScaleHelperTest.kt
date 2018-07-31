package one.xcorp.widget.swipepicker

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sign

class ScaleHelperTest {

    private val scaleHelper = ScaleHelper()

    @Test
    fun isBelongsToScale() = with(scaleHelper) {
        val scale = listOf(-2f, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f, 3.01f, 3.5f)

        assertTrue(isBelongsToScale(scale, 0f, -1.34f))
        assertTrue(isBelongsToScale(scale, 0f, 1.53f))
        assertFalse(isBelongsToScale(scale, 0f, -1.5f))
        assertFalse(isBelongsToScale(scale, 0f, 2f))
        assertFalse(isBelongsToScale(scale, 0f, -3.5f))
        assertFalse(isBelongsToScale(scale, 0f, 8f))
        assertFalse(isBelongsToScale(scale, 0f, -4f))
        assertFalse(isBelongsToScale(scale, 0f, 7f))

        assertTrue(isBelongsToScale(scale, 1.5f, -1.34f))
        assertTrue(isBelongsToScale(scale, 1.5f, 1.53f))
        assertFalse(isBelongsToScale(scale, 1.5f, -1.5f))
        assertFalse(isBelongsToScale(scale, 1.5f, 2f))
        assertTrue(isBelongsToScale(scale, 1.5f, -3.5f))
        assertTrue(isBelongsToScale(scale, 1.5f, 8f))
        assertFalse(isBelongsToScale(scale, 1.5f, -4f))
        assertFalse(isBelongsToScale(scale, 1.5f, 7f))
    }

    @Test
    fun getNumberDivisions() = with(scaleHelper) {
        // move by step
        assertEquals(getNumberDivisions(null, 0f, -3.5f, -8f), -1)
        assertEquals(getNumberDivisions(null, 0f, 7f, 11.5f), 1)

        assertEquals(getNumberDivisions(null, 1.5f, -3.5f, -3.5f), 0)
        assertEquals(getNumberDivisions(null, 1.5f, 7f, 7f), 0)

        assertEquals(getNumberDivisions(null, 1.5f, -3.5f, -8f), -3)
        assertEquals(getNumberDivisions(null, 1.5f, -4f, -8f), -3)
        assertEquals(getNumberDivisions(null, 1.5f, -4f, -0.5f), 3)

        assertEquals(getNumberDivisions(null, 1.5f, 7f, 11.5f), 3)
        assertEquals(getNumberDivisions(null, 1.5f, 7.5f, 11.5f), 3)
        assertEquals(getNumberDivisions(null, 1.5f, 7.5f, 4f), -3)

        assertEquals(getNumberDivisions(null, 0f, 0f, -4.25f), -1)
        assertEquals(getNumberDivisions(null, 0f, 1f, 5f), 1)

        assertEquals(getNumberDivisions(null, 900000f, 8.64E7f, 8.55E7f), -1)
        assertEquals(getNumberDivisions(null, 900000f, 8.55E7f, 8.64E7f), 1)
        assertEquals(getNumberDivisions(null, 900000f, 6.12E7f, 0f), -68)
        assertEquals(getNumberDivisions(null, 900000f, 0f, 6.12E7f), 68)

        // move by scale
        var scale = listOf(-2f, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f, 3.01f, 3.5f)

        assertEquals(getNumberDivisions(scale, 0f, 0f, -1.99f), -3)
        assertEquals(getNumberDivisions(scale, 0f, -1.34f, 1.53f), 4)
        assertEquals(getNumberDivisions(scale, 0f, 1f, -1.5f), -5)
        assertEquals(getNumberDivisions(scale, 0f, -0.1f, 2f), 4)

        assertEquals(getNumberDivisions(scale, 0f, -1.995f, -5.1f), -2)
        assertEquals(getNumberDivisions(scale, 0f, -5f, -1.5f), 3)
        assertEquals(getNumberDivisions(scale, 0f, 3.25f, 7f), 2)
        assertEquals(getNumberDivisions(scale, 0f, 7f, 3.25f), -2)
        assertEquals(getNumberDivisions(scale, 0f, -5f, 7f), 10)
        assertEquals(getNumberDivisions(scale, 0f, 6.5f, -4.5f), -10)

        assertEquals(getNumberDivisions(scale, 0f, -4.5f, -8f), 0)
        assertEquals(getNumberDivisions(scale, 0f, -8f, -4.5f), 0)
        assertEquals(getNumberDivisions(scale, 0f, 6.5f, 10f), 0)
        assertEquals(getNumberDivisions(scale, 0f, 10f, 6.5f), 0)

        assertEquals(getNumberDivisions(scale, 1.5f, -1.995f, -5.1f), -4)
        assertEquals(getNumberDivisions(scale, 1.5f, -5f, -1.5f), 4)
        assertEquals(getNumberDivisions(scale, 1.5f, 3.25f, 7f), 4)
        assertEquals(getNumberDivisions(scale, 1.5f, 7f, 3.25f), -4)
        assertEquals(getNumberDivisions(scale, 1.5f, -5f, 7f), 13)
        assertEquals(getNumberDivisions(scale, 1.5f, 6.5f, -4.5f), -12)

        assertEquals(getNumberDivisions(scale, 1.5f, -4.5f, -8f), -3)
        assertEquals(getNumberDivisions(scale, 1.5f, -8.5f, -4.5f), 4)
        assertEquals(getNumberDivisions(scale, 1.5f, 7f, 10f), 3)
        assertEquals(getNumberDivisions(scale, 1.5f, 9.5f, 5f), -3)

        scale = listOf(0.1f, 0.2f, 0.3f, 0.5f, 0.8f,
                1.3f, 2.1f, 3.4f, 5.5f, 8.9f, 14.4f, 23.3f, 37.7f, 61.0f, 98.7f)

        assertEquals(getNumberDivisions(scale, 3.3f, -10f, -13.1f), -1)
        assertEquals(getNumberDivisions(scale, 3.3f, 110f, 111.9f), 1)
    }

    @Test
    fun getClosestValue() = with(scaleHelper) {
        assertEquals(getClosestValue(1f, 3f, 2f), 1f)
        assertEquals(getClosestValue(1f, 3f, 2.1f), 3f)

        assertEquals(getClosestValue(-1f, -3f, -2f), -1f)
        assertEquals(getClosestValue(-1f, -3f, -2.1f), -3f)
    }

    @Test
    fun stickToScale() = with(scaleHelper) {
        val scale = listOf(-2f, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f, 3.01f, 3.5f)

        assertEquals(stickToScale(scale, 0f, -5f), -2f)
        assertEquals(stickToScale(scale, 0f, 10f), 3.5f)

        assertEquals(stickToScale(scale, 1.5f, -3.5f), -3.5f)
        assertEquals(stickToScale(scale, 1.5f, 6.5f), 6.5f)

        assertEquals(stickToScale(scale, 1.5f, -8.75f), -8f)
        assertEquals(stickToScale(scale, 1.5f, 13.25f), 12.5f)

        assertEquals(stickToScale(scale, 1.5f, -8.76f), -9.5f)
        assertEquals(stickToScale(scale, 1.5f, 13.26f), 14f)

        assertEquals(stickToScale(scale, 1.5f, -1.34f), -1.34f)
        assertEquals(stickToScale(scale, 1.5f, 1.53f), 1.53f)

        assertEquals(stickToScale(scale, 1.5f, -2.75f), -2f)
        assertEquals(stickToScale(scale, 1.5f, -2.76f), -3.5f)

        assertEquals(stickToScale(scale, 1.5f, 7.25f), 6.5f)
        assertEquals(stickToScale(scale, 1.5f, 7.26f), 8f)

        assertEquals(stickToScale(scale, 1.5f, 0.375f), 0f)
        assertEquals(stickToScale(scale, 1.5f, 0.376f), 0.75f)
    }

    @Test
    fun moveToDivision() {
        // move by step
        SwipeSimulator(null, 0f).assertScale(3.6f, 3.6f, 3.6f, 3.6f)

        var simulator = SwipeSimulator(null, 1.5f)
        simulator.assertScale(1f, -0.5f, -2f, -3.5f, -5f, -6.5f, -8f, -9.5f)
        simulator.assertScale(1f, 2.5f, 4f, 5.5f, 7f, 8.5f, 10f, 11.5f)
        simulator.assertScale(-3.6f, -5.1f, -6.6f, -8.1f, -9.6f)
        simulator.assertScale(7.1f, 8.6f, 10.1f, 11.6f, 13.1f)
        simulator.assertScale(-2f, -0.5f, 1f, 2.5f, 4f, 5.5f)

        SwipeSimulator(null, 1123.534f).assertScale(
                -2.6734f, 1120.8606f, 2244.3946f, 3367.9286f, 4491.4626f, 5614.9966f)

        // move by single scale
        simulator = SwipeSimulator(listOf(1f), 0f)
        simulator.assertScale(-1.45f, -1.45f, -1.45f, -1.45f, reverse = false)
        simulator.assertScale(-11.45f, 1f, 1f, 1f, reverse = false)
        simulator.assertScale(2.15f, 2.15f, 2.15f, 2.15f, reverse = false)
        simulator.assertScale(12.15f, 1f, 1f, 1f, reverse = false)
        simulator.assertScale(1f, 1f, 1f, 1f)

        simulator = SwipeSimulator(listOf(1f), 1.5f)
        simulator.assertScale(1f, -0.5f, -2f, -3.5f, -5f, -6.5f)
        simulator.assertScale(1f, 2.5f, 4f, 5.5f, 7f, 8.5f)
        simulator.assertScale(-3.5f, -5f, -6.5f)
        simulator.assertScale(-3.5f, -2f, -0.5f)
        simulator.assertScale(-3.5f, -2f, -0.5f, 1f, 2.5f, 4f)
        simulator.assertScale(5.5f, 7f, 8.5f)
        simulator.assertScale(5.5f, 4f, 2.5f)
        simulator.assertScale(5.5f, 4f, 2.5f, 1f, -0.5f, -2f)
        simulator.assertScale(0.25f, -0.5f, -2f, -3.5f, -5f, reverse = false)
        simulator.assertScale(2.4f, 2.5f, 4f, 5.5f, 7f, reverse = false)
        simulator.assertScale(-3f, -3.5f, -5f, reverse = false)
        simulator.assertScale(-3f, -2f, -0.5f, reverse = false)
        simulator.assertScale(-3f, -2f, -0.5f, 1f, 2.5f, 4f, reverse = false)
        simulator.assertScale(5f, 5.5f, 7f, reverse = false)
        simulator.assertScale(5f, 4f, 2.5f, reverse = false)
        simulator.assertScale(5f, 4f, 2.5f, 1f, -0.5f, -2f, reverse = false)

        simulator = SwipeSimulator(listOf(-0.4565f), 0.1234235f)
        simulator.assertScale(-0.4565f, -0.5799235f, -0.703347f, -0.8267705f, -0.950194f)
        simulator.assertScale(
                -0.4f, -0.3330765f, -0.209653f, -0.0862295f, 0.037194f, reverse = false)

        // move by scale
        val scale = listOf(-2f, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f, 3.01f, 3.5f)

        simulator = SwipeSimulator(scale, 0f)
        simulator.assertScale(-1.99f, -2f, -2f, -2f, -2f, reverse = false)
        simulator.assertScale(-1.99999999f, -2f, -2f, -2f, reverse = false)
        simulator.assertScale(-1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f)
        simulator.assertScale(3.01f, 3.5f, 3.5f, 3.5f, 3.5f, reverse = false)
        simulator.assertScale(3.02f, 3.5f, 3.5f, 3.5f, reverse = false)
        simulator.assertScale(6.5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -2f, -2f, -2f, reverse = false)
        simulator.assertScale(-5.5f, -2f, -1.99f, -1.34f, -1f,
                0f, 0.75f, 1.53f, 3.01f, 3.5f, 3.5f, 3.5f, 3.5f, reverse = false)
        simulator.assertScale(7f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -2f, -2f, -2f, reverse = false)

        simulator = SwipeSimulator(scale, 1.5f)
        simulator.assertScale(-1.99f, -2f, -3.5f, -5f, -6.5f)
        simulator.assertScale(-1.99999f, -2f, -3.5f, -5f, reverse = false)
        simulator.assertScale(-1.35f, -1.99f, -2f, -3.5f, reverse = false)
        simulator.assertScale(-1.35f, -1.34f, -1f, 0f, 0.75f, reverse = false)
        simulator.assertScale(-1.98f, -1.99f, -2f, -3.5f, reverse = false)
        simulator.assertScale(-1.98f, -1.34f, -1f, 0f, 0.75f, reverse = false)
        simulator.assertScale(3.01f, 3.5f, 5f, 6.5f, 8f)
        simulator.assertScale(3.02f, 3.5f, 5f, 6.5f, reverse = false)
        simulator.assertScale(6.5f, 5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -3.5f, -5f, -6.5f)
        simulator.assertScale(-4f, -3.5f, -2f, -1.99f, -1.34f, -1f,
                0f, 0.75f, 1.53f, 3.01f, 3.5f, 5f, 6.5f, 8f, reverse = false)
        simulator.assertScale(5.5f, 5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -3.5f, -5f, -6.5f, reverse = false)
    }

    private inner class SwipeSimulator(val scale: List<Float>?, val step: Float) {

        fun bypass(value: Float, division: Int): List<Float> {
            val progression = if (division < 0) 0 downTo division else 0..division
            return progression.map { scaleHelper.moveToDivision(scale, step, value, it) }
        }

        fun assertScale(vararg reference: Float, reverse: Boolean = true) {
            val expected = reference.toList()
            val division = expected.lastIndex * direction(reference)

            assertEquals(expected, bypass(reference.first(), division))
            if (reverse) {
                assertEquals(expected.reversed(), bypass(reference.last(), -division))
            }
        }

        private fun direction(reference: FloatArray) = when {
            reference[0] == reference[1] -> reference[0].sign.toInt()
            reference[0] > reference[1] -> -1
            else -> 1
        }
    }
}