package com.meshlink.stress

import org.junit.Assume
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SoakTest {

    private fun assumeNightlyOrExtended() {
        // Skip soak tests unless explicitly enabled via environment variable
        val runSoak = System.getenv("RUN_SOAK_TESTS") == "true"
        Assume.assumeTrue("Skipping soak test. Set RUN_SOAK_TESTS=true to run.", runSoak)
    }

    @Test
    fun test1HourSoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(50)
            duration(1.hours)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(0) // Messages are injected periodically by SoakTestEnvironment
            seed(501L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }

    @Test
    fun test6HourSoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(50)
            duration(6.hours)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(0)
            seed(502L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }

    @Test
    fun test24HourNightlySoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(100)
            duration(24.hours)
            profile(NetworkFailureProfiles.NightlyStress)
            messages(0)
            seed(503L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }
}
