import re

filepath = 'app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt'
with open(filepath, 'r') as f:
    content = f.read()

# Add wifiDirectManager to constructor
new_constructor = r"""private val wifiDirectManager: com.meshlink.wifi.WifiDirectManager,
    private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,"""
content = content.replace("private val securityMonitor: com.meshlink.security.data.MeshSecurityMonitor,", new_constructor)

# Add the methods at the end (before the last brace)
methods_to_add = """
    fun checkAndTriggerHandshake(address: String) {
        val state = connectionManager.peerStates[address] ?: return
        if (state == com.meshlink.ble.model.PeerConnectionState.SERVICES_DISCOVERED || state == com.meshlink.ble.model.PeerConnectionState.MTU_READY) {
            val peerId = discoveryManager.scannedDevices.value.values.firstOrNull { it.address == address }?.meshId
                ?: meshRouter.routeTable.entries.firstOrNull { it.value.nextHop == address }?.key
                
            if (peerId != null) {
                scope.launch {
                    val reqEnc = userRepository.isEncryptionEnabled.first()
                    if (reqEnc) {
                        if (cryptoManager.hasPeerKey(peerId)) {
                            connectionManager.updatePeerState(address, com.meshlink.ble.model.PeerConnectionState.SESSION_READY)
                            retryPendingMessages()
                        } else {
                            val currentState = connectionManager.peerStates[address]
                            if (currentState != com.meshlink.ble.model.PeerConnectionState.KEY_EXCHANGE_STARTED) {
                                connectionManager.peerStates[address] = com.meshlink.ble.model.PeerConnectionState.KEY_EXCHANGE_STARTED
                                val user = userRepository.getLocalUser()
                                if (user != null) {
                                    val localPeerId = routingCoordinator.networkId(user.meshId)
                                    val packetBase = generateSignedKeyExchange(localPeerId)
                                    val packet = packetBase.copy(targetId = peerId)
                                    dispatchSinglePacket(peerId, packet)
                                }
                            }
                        }
                    } else {
                        connectionManager.updatePeerState(address, com.meshlink.ble.model.PeerConnectionState.SESSION_READY)
                        retryPendingMessages()
                    }
                }
            }
        }
    }

    private suspend fun receiveSosMessage(packet: MeshPacket) {
        if (chatDao.getMessageByUuid(packet.packetId) != null) return // Ignore duplicate

        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val lat = json.optDouble("lat", 0.0)
        val lng = json.optDouble("lng", 0.0)
        val battery = json.optInt("battery", -1)
        val senderName = json.optString("senderName", packet.senderId.takeLast(8))

        val chatId = routingCoordinator.incomingChatId(packet.senderId)

        val message = MessageEntity(
            messageId = packet.packetId,
            chatId = chatId,
            senderId = packet.senderId,
            text = "🚨 SOS EMERGENCY from $senderName — Lat: $lat, Lng: $lng — Battery: $battery%",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng,
            batteryPercent = battery
        )
        chatDao.insertMessageAndUpdateChat(message, "🚨 $senderName")
    }

    private suspend fun handleWifiNegotiation(packet: MeshPacket) {
        val json = try { JSONObject(packet.payload) } catch (_: Exception) { return }
        val peerMac = json.optString("wifiMac")
        if (peerMac.isNotEmpty()) {
            MeshLogger.d(TAG, "Received Wi-Fi Direct MAC from peer: $peerMac, initiating connect...")
            withContext(Dispatchers.Main) {
                wifiDirectManager.connectToPeer(peerMac)
            }
        }
    }
}
"""

content = content[:content.rfind('}')] + methods_to_add

with open(filepath, 'w') as f:
    f.write(content)

print("Methods added")
