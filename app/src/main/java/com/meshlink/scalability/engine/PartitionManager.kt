package com.meshlink.scalability.engine

import javax.inject.Inject
import javax.inject.Singleton

data class PartitionReport(
    val hasSplit: Boolean,
    val isolatedClustersCount: Int,
    val recommendation: String?
)

@Singleton
class PartitionManager @Inject constructor() {

    fun detectPartitions(totalExpectedNodes: Int, currentlyReachableNodes: Int, knownClusters: Int): PartitionReport {
        if (knownClusters > 1) {
            return PartitionReport(
                hasSplit = true,
                isolatedClustersCount = knownClusters,
                recommendation = "Mesh is split into $knownClusters islands. Increase BLE advertising duty cycle to bridge clusters."
            )
        }

        if (totalExpectedNodes > 0 && currentlyReachableNodes < (totalExpectedNodes / 2)) {
            return PartitionReport(
                hasSplit = true,
                isolatedClustersCount = 2,
                recommendation = "More than 50% of the mesh is unreachable. Possible bridge node failure."
            )
        }

        return PartitionReport(
            hasSplit = false,
            isolatedClustersCount = 1,
            recommendation = null
        )
    }
}
