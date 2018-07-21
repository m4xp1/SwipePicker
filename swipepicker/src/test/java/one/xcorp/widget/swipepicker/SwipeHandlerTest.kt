package one.xcorp.widget.swipepicker

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeHandlerTest {

    private val swipeHandler = SwipeHandler()

    @Test
    fun closestValue() = with(swipeHandler) {
        assertEquals(closestValue(1f, 3f, 2f), 1f)
        assertEquals(closestValue(1f, 3f, 2.1f), 3f)

        assertEquals(closestValue(-1f, -3f, -2f), -1f)
        assertEquals(closestValue(-1f, -3f, -2.1f), -3f)
    }

    @Test // Without limit.
    fun closestInBoundary1() = with(swipeHandler) {
        assertEquals(closestInBoundary(-2f, 0f, -5f), -2f)
        assertEquals(closestInBoundary(3.5f, 0f, 10f), 3.5f)

        assertEquals(closestInBoundary(-2f, 1.5f, -8.75f), -8f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 13.25f), 12.5f)
    }

    @Test // Given the limit.
    fun closestInBoundary2() = with(swipeHandler) {
        assertEquals(closestInBoundary(-2f, 0f, -5f, -3.5f), -2f)
        assertEquals(closestInBoundary(-2f, 0f, -5f, -3.6f), -5f)
        assertEquals(closestInBoundary(-2f, 0f, -5f, -6.5f), -5f)
        assertEquals(closestInBoundary(-2f, 0f, 1f, -6.5f), 1f)

        assertEquals(closestInBoundary(3.5f, 0f, 10f, 6.75f), 3.5f)
        assertEquals(closestInBoundary(3.5f, 0f, 10f, 6.76f), 10f)
        assertEquals(closestInBoundary(3.5f, 0f, 10f, 13f), 10f)
        assertEquals(closestInBoundary(3.5f, 0f, 1.5f, 13f), 1.5f)

        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -2.75f), -2f)
        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -2.76f), -3.5f)
        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -4.25f), -3.5f)
        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -4.26f), -5f)
        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -6.5f), -5f)
        assertEquals(closestInBoundary(-2f, 1.5f, 1.5f, -6.5f), 1.5f)

        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 4.25f), 3.5f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 4.26f), 5f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 9.75f), 9.5f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 9.76f), 10f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 11.5f), 10f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 1f, 11.5f), 1f)

        assertEquals(closestInBoundary(-2f, 1.5f, -5f, -3.5f), -3.5f)
        assertEquals(closestInBoundary(3.5f, 1.5f, 10f, 6.5f), 6.5f)
    }

    @Test // Swipe by step.
    fun onSwipe1() {
        var params = createParams(null, 0f)

        assertBypass(params, -3, 3.6f, 3.6f, 3.6f, 3.6f)

        params = createParams(null, 1.5f)

        assertBypass(params, -7, 1f, -0.5f, -2f, -3.5f, -5f, -6.5f, -8f, -9.5f)
        assertBypass(params, 7, 1f, 2.5f, 4f, 5.5f, 7f, 8.5f, 10f, 11.5f)

        assertBypass(params, -4, -3.6f, -5.1f, -6.6f, -8.1f, -9.6f)
        assertBypass(params, 4, 7.1f, 8.6f, 10.1f, 11.6f, 13.1f)

        assertBypass(params, 5, -2f, -0.5f, 1f, 2.5f, 4f, 5.5f)

        params = createParams(null, 1123.534f)

        assertBypass(params, 5,
                -2.6734f, 1120.8606f, 2244.3946f, 3367.9286f, 4491.4626f, 5614.9966f)
    }

    @Test // Swipe by single scale.
    fun onSwipe2() {
        var params = createParams(listOf(1f), 0f)

        assertBypass(params, -3, -1.45f, -1.45f, -1.45f, -1.45f, reverse = false)
        assertBypass(params, 3, -11.45f, 1f, 1f, 1f, reverse = false)
        assertBypass(params, 3, 2.15f, 2.15f, 2.15f, 2.15f, reverse = false)
        assertBypass(params, -3, 12.15f, 1f, 1f, 1f, reverse = false)
        assertBypass(params, -3, 1f, 1f, 1f, 1f)

        params = createParams(listOf(1f), 1.5f)

        assertBypass(params, -5, 1f, -0.5f, -2f, -3.5f, -5f, -6.5f)
        assertBypass(params, 5, 1f, 2.5f, 4f, 5.5f, 7f, 8.5f)

        assertBypass(params, -2, -3.5f, -5f, -6.5f)
        assertBypass(params, 2, -3.5f, -2f, -0.5f)
        assertBypass(params, 5, -3.5f, -2f, -0.5f, 1f, 2.5f, 4f)

        assertBypass(params, 2, 5.5f, 7f, 8.5f)
        assertBypass(params, -2, 5.5f, 4f, 2.5f)
        assertBypass(params, -5, 5.5f, 4f, 2.5f, 1f, -0.5f, -2f)

        assertBypass(params, -4, 0.25f, -0.5f, -2f, -3.5f, -5f, reverse = false)
        assertBypass(params, 4, 2.4f, 2.5f, 4f, 5.5f, 7f, reverse = false)

        assertBypass(params, -2, -3f, -3.5f, -5f, reverse = false)
        assertBypass(params, 2, -3f, -2f, -0.5f, reverse = false)
        assertBypass(params, 5, -3f, -2f, -0.5f, 1f, 2.5f, 4f, reverse = false)

        assertBypass(params, 2, 5f, 5.5f, 7f, reverse = false)
        assertBypass(params, -2, 5f, 4f, 2.5f, reverse = false)
        assertBypass(params, -5, 5f, 4f, 2.5f, 1f, -0.5f, -2f, reverse = false)

        params = createParams(listOf(-0.4565f), 0.1234235f)

        assertBypass(params, -4,
                -0.4565f, -0.5799235f, -0.703347f, -0.8267705f, -0.950194f)
        assertBypass(params, 4,
                -0.4f, -0.3330765f, -0.209653f, -0.0862295f, 0.037194f, reverse = false)
    }

    @Test // Swipe by scale.
    fun onSwipe3() {
        val scale = listOf(-2f, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f, 3.01f, 3.5f)
        var params = createParams(scale, 0f)

        assertBypass(params, -4, -1.99f, -2f, -2f, -2f, -2f, reverse = false)
        assertBypass(params, -3, -1.99999999f, -2f, -2f, -2f, reverse = false)
        assertBypass(params, 5, -1.99f, -1.34f, -1f, 0f, 0.75f, 1.53f)
        assertBypass(params, 4, 3.01f, 3.5f, 3.5f, 3.5f, 3.5f, reverse = false)
        assertBypass(params, 3, 3.02f, 3.5f, 3.5f, 3.5f, reverse = false)

        assertBypass(params, -12, 6.5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -2f, -2f, -2f, reverse = false)

        assertBypass(params, 12, -5.5f, -2f, -1.99f, -1.34f, -1f,
                0f, 0.75f, 1.53f, 3.01f, 3.5f, 3.5f, 3.5f, 3.5f, reverse = false)
        assertBypass(params, -12, 7f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -2f, -2f, -2f, reverse = false)

        params = createParams(scale, 1.5f)

        assertBypass(params, -4, -1.99f, -2f, -3.5f, -5f, -6.5f)
        assertBypass(params, -3, -1.99999f, -2f, -3.5f, -5f, reverse = false)

        assertBypass(params, -3, -1.35f, -1.99f, -2f, -3.5f, reverse = false)
        assertBypass(params, 4, -1.35f, -1.34f, -1f, 0f, 0.75f, reverse = false)
        assertBypass(params, -3, -1.98f, -1.99f, -2f, -3.5f, reverse = false)
        assertBypass(params, 4, -1.98f, -1.34f, -1f, 0f, 0.75f, reverse = false)

        assertBypass(params, 4, 3.01f, 3.5f, 5f, 6.5f, 8f)
        assertBypass(params, 3, 3.02f, 3.5f, 5f, 6.5f, reverse = false)

        assertBypass(params, -13, 6.5f, 5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -3.5f, -5f, -6.5f)

        assertBypass(params, 13, -4f, -3.5f, -2f, -1.99f, -1.34f, -1f,
                0f, 0.75f, 1.53f, 3.01f, 3.5f, 5f, 6.5f, 8f, reverse = false)
        assertBypass(params, -13, 5.5f, 5f, 3.5f, 3.01f, 1.53f, 0.75f,
                0f, -1f, -1.34f, -1.99f, -2f, -3.5f, -5f, -6.5f, reverse = false)
    }

    private fun createParams(scale: List<Float>?, step: Float) = SwipeHandler.Params(scale, step)

    private fun assertBypass(params: SwipeHandler.Params, division: Int,
                             vararg reference: Float, reverse: Boolean = true) {
        val referenceList = reference.toList()
        assertEquals(referenceList, bypassScale(params, reference.first(), division))

        if (reverse) {
            assertEquals(referenceList.reversed(), bypassScale(params, reference.last(), -division))
        }
    }

    private fun bypassScale(params: SwipeHandler.Params, value: Float, division: Int): List<Float> {
        val progression = if (division < 0) 0 downTo division else 0..division
        return progression.map { swipeHandler.onSwipe(params, value, it) }
    }
}
