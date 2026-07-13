import sys

def main():
    with open('app/src/main/java/com/meshlink/data/repository/BleRepository.kt', 'r') as f:
        content = f.read()

    # Find the line inside retryPendingMessages where we log "Retrying pending messages"
    idx = content.find('pending.forEach { msg ->')
    if idx == -1: return

    replacement = """pending.forEach { msg ->
            if (!hasDeliveryPath(msg.chatId)) {
                return@forEach
            }
            val reqEncCheck = kotlinx.coroutines.flow.first(userRepository.isEncryptionEnabled)
            if (reqEncCheck && !cryptoManager.hasPeerKey(msg.chatId)) {
                Log.w(TAG, "Missing key for ${msg.chatId}, requesting key exchange and postponing retry")
                val localUser = userRepository.getLocalUser()
                if (localUser != null) {
                    val localPeerId = networkId(localUser.meshId)
                    val publicKey = cryptoManager.getOrCreatePublicKey()
                    meshRouter.broadcastKeyExchange(localPeerId, publicKey)
                }
                return@forEach
            }"""

    content = content.replace('pending.forEach { msg ->', replacement)
    
    with open('app/src/main/java/com/meshlink/data/repository/BleRepository.kt', 'w') as f:
        f.write(content)

if __name__ == '__main__':
    main()
