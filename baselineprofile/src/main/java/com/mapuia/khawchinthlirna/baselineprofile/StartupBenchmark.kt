package com.mapuia.khawchinthlirna.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class benchmarks the target app's startup performance.
 *
 * Run this benchmark to measure the startup time with different compilation modes:
 * - None: No compilation (interpreted)
 * - Partial: Uses Baseline Profile for optimized compilation
 * - Full: Full AOT compilation
 *
 * Compare the results to see the improvements from the Baseline Profile.
 *
 * To run this test:
 * ./gradlew :baselineprofile:connectedBenchmarkAndroidTest -P android.testInstrumentationRunnerArguments.class=com.mapuia.khawchinthlirna.baselineprofile.StartupBenchmark
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = benchmark(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() = benchmark(
        CompilationMode.Partial(BaselineProfileMode.Require)
    )

    private fun benchmark(compilationMode: CompilationMode) {
        rule.measureRepeated(
            packageName = "com.mapuia.khawchinthlirna",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                pressHome()
            }
        ) {
            // Start the app
            startActivityAndWait()

            // Wait for the main content to be displayed
            device.waitForIdle()
        }
    }
}

