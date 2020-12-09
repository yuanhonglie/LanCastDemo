package com.yhl.lanlink

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.text.DecimalFormat

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.yhl.cast", appContext.packageName)
    }

    @Test
    fun floatFormat1() {
        val text = String.format("%.02f", 1.200f)
        assertEquals("1.20", text)
    }

    @Test
    fun floatFormat2() {

        val format = DecimalFormat(".##")
        assertEquals("1.2", format.format(1.200f))
        assertEquals("1.21", format.format(1.210f))
    }
}