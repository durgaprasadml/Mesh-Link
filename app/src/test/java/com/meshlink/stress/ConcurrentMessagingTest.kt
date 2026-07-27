package com.meshlink.stress

import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ConcurrentMessagingTest {

    @Test
    fun test10Nodes() {
        val scenario = stressTest {
            nodes(10)
            duration(30.seconds)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(2000)
            concurrentSenders(10) // All nodes concurrently
            seed(201L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }

    @Test
    fun test25Nodes() {
        val scenario = stressTest {
            nodes(25)
            duration(45.seconds)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(5000)
            concurrentSenders(25)
            seed(202L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }

    @Test
    fun test50Nodes() {
        val scenario = stressTest {
            nodes(50)
            duration(60.seconds)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(10000)
            concurrentSenders(50)
            seed(203L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }
    
    @Test
    fun test100Nodes() {
        val scenario = stressTest {
            nodes(100)
            duration(120.seconds)
            profile(NetworkFailureProfiles.NightlyStress)
            messages(20000)
            concurrentSenders(100)
            seed(204L)
        }
        val runner = StressScenarioRunner(scenario)
        runner.run()
    }
}
