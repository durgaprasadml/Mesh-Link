package com.meshlink.stress

import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FaultInjectionTest {

    @Test
    fun testRandomNodeCrashes() {
        val scenario = stressTest {
            nodes(20)
            duration(30.seconds)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(2000)
            concurrentSenders(5)
            seed(301L)
        }
        val runner = StressScenarioRunner(scenario)
        val env = runner.run()
        
        val injector = FaultInjector(env, seed = scenario.seed)
        injector.injectRandomCrashes(probability = 0.1)
        
        // Let simulation stabilize
        env.runUntilQuiet()
        
        // Metrics collection is implicitly validated in StressScenarioRunner,
        // but if we do custom stuff we can call collector again.
    }

    @Test
    fun testLinkFlapping() {
        val scenario = stressTest {
            nodes(20)
            duration(30.seconds)
            profile(NetworkFailureProfiles.RouteFlapping)
            messages(2000)
            concurrentSenders(5)
            seed(302L)
        }
        val runner = StressScenarioRunner(scenario)
        val env = runner.run()
        
        val injector = FaultInjector(env, seed = scenario.seed)
        injector.injectLinkFlapping(probability = 0.2)
        
        env.runUntilQuiet()
    }
}
