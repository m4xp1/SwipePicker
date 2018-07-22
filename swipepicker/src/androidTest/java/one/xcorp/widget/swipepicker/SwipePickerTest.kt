package one.xcorp.widget.swipepicker

import android.support.test.InstrumentationRegistry
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipePickerTest {

    private lateinit var swipePicker: SwipePicker

    @Before
    fun setUp() {
        getInstrumentation().runOnMainSync {
            swipePicker = SwipePicker(InstrumentationRegistry.getTargetContext())
        }
    }

    @Test
    fun initialize() {
        assertEquals(1f, swipePicker.value)
    }
}
