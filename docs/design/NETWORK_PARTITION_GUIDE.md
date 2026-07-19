# Network Partition Guide

## Detecting Split Brain Scenarios
In large deployments, the mesh can fracture into isolated "islands" (e.g., Team A moves to the basement, Team B stays on the roof).

### `PartitionManager`
- Continuously evaluates the total known graph versus the currently reachable graph.
- If it detects multiple isolated clusters, or if >50% of the historical mesh drops simultaneously, it flags a partition.
- **Recommendation Engine**: Automatically advises the transport layer to increase advertising duty cycles specifically to search for the missing bridge node and heal the split.
