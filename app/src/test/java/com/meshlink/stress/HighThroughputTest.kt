package com.meshlink.stress

import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class HighThroughputTest {

    @Test
    fun test1kMessages() {
        val scenario = stressTest {
            nodes(25)
            duration(30.seconds)
            profile(NetworkFailureProfiles.PerfectNetwork)
            messages(1000)
            concurrentSenders(5)
            seed(101L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }

    @Test
    fun test10kMessages() {
        val scenario = stressTest {
            nodes(50)
            duration(60.seconds)
            profile(NetworkFailureProfiles.PerfectNetwork)
            messages(10000)
            concurrentSenders(20)
            seed(102L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }

    @Test
    fun test50kMessages() {
        val scenario = stressTest {
            nodes(100)
            duration(120.seconds)
            profile(NetworkFailureProfiles.PerfectNetwork)
            messages(50000)
            concurrentSenders(50)
            seed(103L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }
}
