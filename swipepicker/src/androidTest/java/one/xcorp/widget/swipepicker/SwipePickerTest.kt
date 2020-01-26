package one.xcorp.widget.swipepicker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipePickerTest {

    private lateinit var swipePicker: SwipePicker

    @Before
    fun setUp() {
        getInstrumentation().runOnMainSync {
            swipePicker = SwipePicker(getInstrumentation().targetContext)
        }
    }

    @Test
    fun initialize() {
        assertEquals(1f, swipePicker.value)
    }
}
