package com.meshlink.stress

import org.junit.Assume
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SoakTest {

    private fun assumeNightlyOrExtended() {
        // Skip long soak tests unless explicitly enabled via environment variable
        val runSoak = System.getenv("RUN_SOAK_TESTS") == "true"
        Assume.assumeTrue("Skipping soak test. Set RUN_SOAK_TESTS=true to run.", runSoak)
    }

    @Test
    fun testFastSmokeSoak() {
        assumeNightlyOrExtended()
        // 5-minute simulated soak test
        val scenario = stressTest {
            nodes(10)
            duration(5.minutes)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(0)
            seed(499L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }

    @Test
    fun test30MinuteSoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(30)
            duration(30.minutes)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(0)
            seed(500L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }

    @Test
    fun test2HourSoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(50)
            duration(2.hours)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(0)
            seed(501L)
        }
        val env = SoakTestEnvironment(scenario)
        env.runSoakTest()
    }

    @Test
    fun test8HourSoak() {
        assumeNightlyOrExtended()
        val scenario = stressTest {
            nodes(50)
            duration(8.hours)
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
