package com.meshlink.ble.data

import com.meshlink.ble.data.handlers.AckManager
import com.meshlink.ble.data.handlers.BroadcastHandler
import com.meshlink.ble.data.handlers.KeyExchangeHandler
import com.meshlink.ble.data.handlers.LocationMessageHandler
import com.meshlink.ble.data.handlers.MediaMessageHandler
import com.meshlink.ble.data.handlers.TextMessageHandler
import com.meshlink.ble.data.handlers.VoiceMessageHandler
import com.meshlink.common.logger.MeshLogger
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import com.meshlink.security.policy.PacketEncryptionPolicy
import com.meshlink.transfer.TransferManager
import com.meshlink.util.MeshIdNormalizer
import com.meshlink.voice.transport.VoiceTransport
import com.meshlink.ble.data.handlers.BeaconHandler
import com.meshlink.emergency.incident.EmergencyFormSync
import com.meshlink.emergency.incident.Incident
import com.meshlink.emergency.incident.IncidentManager
import com.meshlink.emergency.incident.IncidentStatus
import com.meshlink.emergency.incident.IncidentType
import com.meshlink.emergency.logistics.Resource
import com.meshlink.emergency.logistics.ResourceCategory
import com.meshlink.emergency.logistics.ResourceManager
import com.meshlink.emergency.rescue.TeamMemberStatus
import com.meshlink.emergency.rescue.TeamRole
import com.meshlink.emergency.rescue.TeamTracker
import com.meshlink.video.transport.VideoTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONObject

@Singleton
class IncomingPacketDispatcher(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val cryptoManager: MeshCryptoManager,
    private val sessionManager: SessionManager,
    private val trustManager: TrustManager,
    private val rekeyManager: RekeyManager,
    private val transferManager: TransferManager,
    private val voiceTransport: VoiceTransport,
    private val keyExchangeHandler: KeyExchangeHandler,
    private val textMessageHandler: TextMessageHandler,
    private val mediaMessageHandler: MediaMessageHandler,
    private val locationMessageHandler: LocationMessageHandler,
    private val voiceMessageHandler: VoiceMessageHandler,
    private val broadcastHandler: BroadcastHandler,
    private val ackManager: AckManager,
    private val beaconHandler: BeaconHandler,
    private val profileSyncManagerProvider: javax.inject.Provider<com.meshlink.profile.ProfileSyncManager>,
    private val videoTransport: VideoTransport?,
    private val incidentManager: IncidentManager?,
    private val emergencyFormSync: EmergencyFormSync?,
    private val resourceManager: ResourceManager?,
    private val teamTracker: TeamTracker?
) {
    @Inject
    constructor(
        userRepository: UserRepository,
        settingsRepository: SettingsRepository,
        cryptoManager: MeshCryptoManager,
        sessionManager: SessionManager,
        trustManager: TrustManager,
        rekeyManager: RekeyManager,
        transferManager: TransferManager,
        voiceTransport: VoiceTransport,
        keyExchangeHandler: KeyExchangeHandler,
        textMessageHandler: TextMessageHandler,
        mediaMessageHandler: MediaMessageHandler,
        locationMessageHandler: LocationMessageHandler,
        voiceMessageHandler: VoiceMessageHandler,
        broadcastHandler: BroadcastHandler,
        ackManager: AckManager,
        beaconHandler: BeaconHandler,
        profileSyncManagerProvider: javax.inject.Provider<com.meshlink.profile.ProfileSyncManager>
    ) : this(
        userRepository = userRepository,
        settingsRepository = settingsRepository,
        cryptoManager = cryptoManager,
        sessionManager = sessionManager,
        trustManager = trustManager,
        rekeyManager = rekeyManager,
        transferManager = transferManager,
        voiceTransport = voiceTransport,
        keyExchangeHandler = keyExchangeHandler,
        textMessageHandler = textMessageHandler,
        mediaMessageHandler = mediaMessageHandler,
        locationMessageHandler = locationMessageHandler,
        voiceMessageHandler = voiceMessageHandler,
        broadcastHandler = broadcastHandler,
        ackManager = ackManager,
        beaconHandler = beaconHandler,
        profileSyncManagerProvider = profileSyncManagerProvider,
        videoTransport = null,
        incidentManager = null,
        emergencyFormSync = null,
        resourceManager = null,
        teamTracker = null
    )

    @Inject
    lateinit var injectedVideoTransport: VideoTransport

    @Inject
    lateinit var injectedIncidentManager: IncidentManager

    @Inject
    lateinit var injectedEmergencyFormSync: EmergencyFormSync

    @Inject
    lateinit var injectedResourceManager: ResourceManager

    @Inject
    lateinit var injectedTeamTracker: TeamTracker

    private fun resolvedVideoTransport(): VideoTransport? =
        videoTransport ?: if (::injectedVideoTransport.isInitialized) injectedVideoTransport else null

    private fun resolvedIncidentManager(): IncidentManager? =
        incidentManager ?: if (::injectedIncidentManager.isInitialized) injectedIncidentManager else null

    private fun resolvedEmergencyFormSync(): EmergencyFormSync? =
        emergencyFormSync ?: if (::injectedEmergencyFormSync.isInitialized) injectedEmergencyFormSync else null

    private fun resolvedResourceManager(): ResourceManager? =
        resourceManager ?: if (::injectedResourceManager.isInitialized) injectedResourceManager else null

    private fun resolvedTeamTracker(): TeamTracker? =
        teamTracker ?: if (::injectedTeamTracker.isInitialized) injectedTeamTracker else null
    private val TAG = "IncomingPacketDispatcher"

    suspend fun dispatch(packet: MeshPacket) {
        val isBroadcast = packet.targetId.equals("BROADCAST", ignoreCase = true)
        if (!isBroadcast) {
            val myMeshId = userRepository.getLocalUser()?.meshId
            if (myMeshId != null) {
                val myNetworkId = MeshIdNormalizer.canonicalize(myMeshId)
                val targetNorm = MeshIdNormalizer.canonicalize(packet.targetId)
                val targetsMe = packet.targetId.equals(myMeshId, ignoreCase = true) ||
                                packet.targetId.equals(myNetworkId, ignoreCase = true) ||
                                (targetNorm.isNotBlank() && targetNorm == myNetworkId)

                if (!targetsMe && packet.type != PacketType.KEY_EXCHANGE) {
                    MeshLogger.d(TAG, "Routing packet to ${MeshIdNormalizer.canonicalize(packet.targetId)}")
                    return
                }
            }
        }

        var processedPacket = packet

        // 1. Always attempt decryption if the packet claims to be encrypted, regardless of type
        if (packet.encrypted) {
            var finalPayload = packet.payload
            var validAad: ByteArray? = null
            var usePreviousKey = false

            if (finalPayload.startsWith("v2|")) {
                val unwrapped = sessionManager.validateAndUnwrap(packet.senderId, finalPayload)
                if (unwrapped == null) {
                    MeshLogger.w(TAG, "Dropping packet: session validation failed")
                    return
                }
                validAad = unwrapped.first
                finalPayload = unwrapped.second
                val packetKv = unwrapped.third
                val session = sessionManager.getSession(packet.senderId)
                if (session != null && packetKv == session.previousKeyVersion) {
                    usePreviousKey = true
                }
            }

            val decrypted = cryptoManager.decrypt(finalPayload, packet.senderId, validAad, usePreviousKey)
            if (decrypted == null) {
                MeshLogger.w(TAG, "Dropping packet ${packet.packetId}: Failed to decrypt payload.")
                return
            }
            if (validAad != null) {
                sessionManager.commitAuthenticatedPacket(packet.senderId, validAad)
            }
            trustManager.increaseTrustScore(packet.senderId, 1)
            processedPacket = packet.copy(payload = decrypted)
        }

        // 2. Validate encryption policy centrally AFTER decryption phase
        val hasSecureSession = sessionManager.getSession(processedPacket.senderId) != null
        val strictMode = settingsRepository.advancedEncryptionEnforcement.first()
        
        if (!PacketEncryptionPolicy.validatePacketEncryption(processedPacket, strictMode, hasSecureSession)) {
            MeshLogger.w(TAG, "Dropping packet ${processedPacket.packetId}: fails central Encryption policy")
            return
        }

        // 3. Dispatch using exhaustive when (compiler enforces all branches)
        try {
            when (processedPacket.type) {
                PacketType.KEY_EXCHANGE -> {
                    keyExchangeHandler.handleKeyExchange(processedPacket)
                }
                PacketType.TEXT -> {
                    if (processedPacket.targetId == "BROADCAST") {
                        broadcastHandler.receiveBroadcastTextMessage(processedPacket)
                    } else {
                        textMessageHandler.receiveMessage(processedPacket)
                    }
                }
                PacketType.MEDIA_META,
                PacketType.MEDIA_CHUNK,
                PacketType.MEDIA_ACK,
                PacketType.MEDIA_NACK -> {
                    if (processedPacket.type == PacketType.MEDIA_META && processedPacket.transferId != null) {
                        mediaMessageHandler.insertPlaceholderIncomingMedia(processedPacket)
                    }
                    transferManager.handleIncomingPacket(processedPacket)
                }
                PacketType.LOCATION -> {
                    locationMessageHandler.receiveLocationMessage(processedPacket)
                }
                PacketType.SOS -> {
                    broadcastHandler.receiveSosMessage(processedPacket)
                }
                PacketType.DELIVERY_ACK -> {
                    ackManager.handleDeliveryAck(processedPacket)
                }
                PacketType.READ_RECEIPT -> {
                    ackManager.handleReadReceipt(processedPacket)
                }

                PacketType.SESSION_REKEY -> {
                    rekeyManager.handleRekeyPacket(
                        processedPacket.senderId,
                        processedPacket.payload,
                        cryptoManager.getPeerPublicKey(processedPacket.senderId)
                    )
                }
                PacketType.VOICE_SIGNAL,
                PacketType.VOICE_FRAME -> {
                    voiceTransport.handleIncomingPacket(processedPacket)
                }
                PacketType.BEACON -> {
                    beaconHandler.handleBeaconPacket(processedPacket)
                }
                PacketType.PROFILE_IMAGE_REQUEST -> {
                    profileSyncManagerProvider.get().handleProfileImageRequest(processedPacket)
                }
                PacketType.PROFILE_IMAGE_RESPONSE -> {
                    profileSyncManagerProvider.get().handleProfileImageResponse(processedPacket)
                }
                PacketType.VIDEO_SIGNAL,
                PacketType.VIDEO_FRAME -> {
                    resolvedVideoTransport()?.handleIncomingPacket(processedPacket)
                }
                PacketType.FORM_SYNC -> {
                    resolvedEmergencyFormSync()?.receiveSync(processedPacket.payload)
                }
                PacketType.INCIDENT_REPORT -> {
                    handleIncidentReport(processedPacket)
                }
                PacketType.CHECK_IN -> {
                    handleCheckIn(processedPacket)
                }
                PacketType.RESOURCE_SYNC -> {
                    handleResourceSync(processedPacket)
                }
                PacketType.MAP_SYNC -> {
                    MeshLogger.i(TAG, "Received MAP_SYNC packet from ${processedPacket.senderId} (packetId=${processedPacket.packetId})")
                }
            }
        } catch (e: Exception) {
            MeshLogger.e(TAG, "Error handling packet: ${e.message}")
        }
    }

    private fun handleIncidentReport(packet: MeshPacket) {
        val manager = resolvedIncidentManager() ?: return
        val json = JSONObject(packet.payload)
        val typeName = json.optString("type", IncidentType.MEDICAL.name)
        val incidentType = runCatching { IncidentType.valueOf(typeName) }.getOrDefault(IncidentType.MEDICAL)
        val statusName = json.optString("status", IncidentStatus.ACTIVE.name)
        val incidentStatus = runCatching { IncidentStatus.valueOf(statusName) }.getOrDefault(IncidentStatus.ACTIVE)
        val incident = Incident(
            id = json.optString("id", packet.packetId),
            type = incidentType,
            severity = json.optInt("severity", 3).coerceIn(1, 5),
            locationDescription = json.optString("locationDescription", "Unknown"),
            commanderId = json.optString("commanderId", packet.senderId),
            timestamp = json.optLong("timestamp", packet.timestamp),
            status = incidentStatus
        )
        manager.syncIncomingIncident(incident)
    }

    private fun handleCheckIn(packet: MeshPacket) {
        val tracker = resolvedTeamTracker() ?: return
        val json = JSONObject(packet.payload)
        val role = runCatching { TeamRole.valueOf(json.optString("role", TeamRole.VOLUNTEER.name)) }
            .getOrDefault(TeamRole.VOLUNTEER)
        val status = runCatching { TeamMemberStatus.valueOf(json.optString("status", TeamMemberStatus.SAFE.name)) }
            .getOrDefault(TeamMemberStatus.SAFE)
        tracker.processCheckIn(
            id = json.optString("id", packet.senderId),
            name = json.optString("name", packet.senderId),
            role = role,
            status = status,
            batteryPct = json.optInt("batteryPct", 100).coerceIn(0, 100)
        )
    }

    private fun handleResourceSync(packet: MeshPacket) {
        val manager = resolvedResourceManager() ?: return
        val json = JSONObject(packet.payload)
        val category = runCatching { ResourceCategory.valueOf(json.optString("category", ResourceCategory.MEDICAL.name)) }
            .getOrDefault(ResourceCategory.MEDICAL)
        val resource = Resource(
            id = json.optString("id", packet.packetId),
            category = category,
            name = json.optString("name", "Resource"),
            quantity = json.optInt("quantity", 0).coerceAtLeast(0),
            location = json.optString("location", "Unknown"),
            ownerId = json.optString("ownerId", packet.senderId)
        )
        manager.syncResource(resource)
    }
}
