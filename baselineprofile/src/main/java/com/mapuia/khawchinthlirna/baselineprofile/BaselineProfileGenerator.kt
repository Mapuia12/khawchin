package com.mapuia.khawchinthlirna.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test generates a baseline profile for the Khawchin Thlirna app.
 *
 * We use [BaselineProfileRule] to generate the profile. The profile is
 * stored in the app's assets folder and is used to optimize the app's
 * startup time and performance.
 *
 * To run this test:
 * ./gradlew :baselineprofile:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=com.mapuia.khawchinthlirna.baselineprofile.BaselineProfileGenerator
 *
 * Or use the managed device:
 * ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = "com.mapuia.khawchinthlirna",
            // Iterate through some of the most critical user journeys
            includeInStartupProfile = true,
        ) {
            // Start from the home screen
            pressHome()
            startActivityAndWait()

            // Wait for the app to fully load
            device.waitForIdle()

            // Let the main content render completely
            // The app shows weather data on the main screen
            Thread.sleep(2000)

            // Scroll the main content to trigger lazy loading and rendering
            device.findObject(By.scrollable(true))?.let { scrollable ->
                scrollable.scroll(androidx.test.uiautomator.Direction.DOWN, 0.5f)
                device.waitForIdle()
                Thread.sleep(500)

                scrollable.scroll(androidx.test.uiautomator.Direction.UP, 0.5f)
                device.waitForIdle()
            }

            // Wait for any animations to complete
            Thread.sleep(1000)
        }
    }
}

