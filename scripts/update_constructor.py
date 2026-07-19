import re

file_path = "app/src/main/java/com/meshlink/ble/data/MeshMessagingManager.kt"
with open(file_path, 'r') as f:
    content = f.read()

# Add missing dependencies to constructor
new_constructor = """
class MeshMessagingManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val cryptoManager: MeshCryptoManager,
    private val meshRouter: MeshRouter,
    private val transferManager: MediaTransferManager,
    private val locationProvider: LocationProvider,
    private val routingCoordinator: RoutingCoordinator,
    private val sessionManager: com.meshlink.security.data.SessionManager,
    private val trustManager: com.meshlink.security.data.TrustManager,
    private val rekeyManager: com.meshlink.security.data.RekeyManager,
    private val voiceTransport: com.meshlink.voice.transport.VoiceTransport,
    private val videoTransport: com.meshlink.video.transport.VideoTransport,
    private val connectionManager: BleConnectionManager,
    private val discoveryManager: DiscoveryManager
) {
"""

content = re.sub(r'class MeshMessagingManager @Inject constructor\([^)]+\)\s*\{', new_constructor.strip(), content)

with open(file_path, 'w') as f:
    f.write(content)
