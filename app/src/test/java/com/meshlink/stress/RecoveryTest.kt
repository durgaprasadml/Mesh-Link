package com.meshlink.stress

import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RecoveryTest {

    @Test
    fun testNetworkPartitionAndHealing() {
        val scenario = stressTest {
            nodes(20)
            duration(30.seconds)
            profile(NetworkFailureProfiles.TypicalBLE)
            messages(1000)
            concurrentSenders(5)
            seed(401L)
        }
        val runner = StressScenarioRunner(scenario)
        val env = runner.run()
        
        val injector = FaultInjector(env, seed = scenario.seed)
        
        // Partition nodes into two groups
        val nodeIds = env.nodeIds()
        val mid = nodeIds.size / 2
        val groupA = nodeIds.subList(0, mid)
        val groupB = nodeIds.subList(mid, nodeIds.size)
        
        injector.createNetworkPartition(groupA, groupB)
        
        // Let simulation step to recognize partition
        env.step(1000L)
        
        // Heal
        injector.healNetworkPartition(groupA, groupB)
        
        // Traffic should continue flowing and route cache should converge
        env.runUntilQuiet()
    }
}
