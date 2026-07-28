# API Documentation

This document describes the public-facing API for Mesh Link V3.0 (`com.meshlink.api`).

## `MeshManager`

The primary entry point for configuring and interacting with the mesh network.

### `init(context: Context, config: MeshConfig)`
*   **Responsibility:** Initializes the mesh framework. Must be called before any other operations.
*   **Parameters:** 
    *   `context`: Application context.
    *   `config`: Configuration parameters (e.g., MTU, Transport preferences).
*   **Thread Safety:** Must be called on the main thread.
*   **Error Handling:** Throws `IllegalStateException` if already initialized.

### `start()`
*   **Responsibility:** Starts peer discovery and accepts incoming connections.
*   **Return Value:** `Job` representing the background operation.
*   **Usage:**
    ```kotlin
    lifecycleScope.launch {
        MeshManager.getInstance().start()
    }
    ```

### `stop()`
*   **Responsibility:** Gracefully tears down all connections, stops advertising, and clears routing tables.

## `MeshClient`

Interface for sending and receiving data.

### `sendMessage(destination: String, payload: ByteArray): Flow<SendStatus>`
*   **Responsibility:** Enqueues a message for routing to the destination.
*   **Parameters:**
    *   `destination`: The unique ID of the target node.
    *   `payload`: The plaintext data to send.
*   **Return Value:** A Kotlin `Flow` emitting the status of the transmission (`ENQUEUED`, `ROUTING`, `DELIVERED`, `FAILED`).
*   **Error Handling:** Catches internal routing errors and emits a `SendStatus.FAILED` state.
*   **Thread Safety:** Safe to call from any coroutine context.

### `receiveMessages(): Flow<MeshMessage>`
*   **Responsibility:** Subscribes to incoming messages addressed to this node.
*   **Return Value:** A hot `Flow` of `MeshMessage` objects containing the sender ID and plaintext payload.
*   **Usage:**
    ```kotlin
    lifecycleScope.launch {
        meshClient.receiveMessages().collect { message ->
            println("Received from ${message.sender}: ${message.payload}")
        }
    }
    ```

## `MeshConfig.Builder`

Builder pattern for configuration.

### `setMtuSize(size: Int)`
*   **Responsibility:** Sets the preferred MTU. Default is 512.

### `setRoutingTtl(ttl: Int)`
*   **Responsibility:** Sets the maximum hop count for packets. Default is 10.
