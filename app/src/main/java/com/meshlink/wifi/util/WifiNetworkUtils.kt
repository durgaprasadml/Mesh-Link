package com.meshlink.wifi.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.meshlink.common.logger.MeshLogger
import java.io.File
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

object WifiNetworkUtils {
    private const val TAG = "WifiNetworkUtils"

    /**
     * Returns the local IPv4 address assigned to the active Wi-Fi interface.
     * Prioritizes Wi-Fi Direct (p2p*) interfaces first so P2P mesh IPs are accurately detected
     * even when cellular data or standard Wi-Fi is active.
     */
    fun getLocalWifiIp(context: Context): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            // 1. First priority: Wi-Fi Direct interface (p2p*)
            for (nif in interfaces) {
                if (nif.isUp && (nif.name.startsWith("p2p") || nif.name.contains("p2p"))) {
                    for (addr in nif.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "0.0.0.0") return host
                        }
                    }
                }
            }
            // 2. Second priority: Standard Wi-Fi interface (wlan*)
            for (nif in interfaces) {
                if (nif.isUp && nif.name.startsWith("wlan")) {
                    for (addr in nif.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "0.0.0.0") return host
                        }
                    }
                }
            }
            // 3. Third priority: Soft AP / Hotspot / Tethering interface (ap*, swlan*, rndis*)
            for (nif in interfaces) {
                if (nif.isUp && (nif.name.startsWith("ap") || nif.name.startsWith("swlan") || nif.name.startsWith("rndis"))) {
                    for (addr in nif.inetAddresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "0.0.0.0") return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to get local Wi-Fi IP from NetworkInterfaces: ${e.message}")
        }

        // 4. Fallback: ConnectivityManager
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val activeNetwork = cm.activeNetwork ?: return null
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

            val lp = cm.getLinkProperties(activeNetwork) ?: return null
            for (linkAddress in lp.linkAddresses) {
                val addr = linkAddress.address
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    val host = addr.hostAddress
                    if (!host.isNullOrBlank() && host != "0.0.0.0") return host
                }
            }
            null
        } catch (e: Exception) {
            MeshLogger.w(TAG, "Failed to get local Wi-Fi IP: ${e.message}")
            null
        }
    }

    /**
     * Resolves all candidate peer/gateway IPv4 addresses on the current Wi-Fi network.
     * In Wi-Fi Direct Hotspot mode or AP mode, the default gateway is the peer running the AP.
     * Also checks DHCP server address, DNS servers, and ARP table.
     */
    fun getCandidatePeerIps(context: Context): List<String> {
        val candidates = mutableListOf<String>()
        val localIp = getLocalWifiIp(context)

        // 1. Direct Wi-Fi Direct P2P Subnet Fast-Path:
        // In Wi-Fi Direct, the Group Owner is ALWAYS 192.168.49.1.
        if (localIp != null && localIp.startsWith("192.168.49.")) {
            if (localIp != "192.168.49.1") {
                // If this device is a client in the group, GO is guaranteed to be 192.168.49.1
                candidates.add("192.168.49.1")
            }
            // Add any ARP clients on 192.168.49.x subnet
            getArpClients().filter { it.startsWith("192.168.49.") }.forEach { candidates.add(it) }
            return candidates.filter { it != localIp }.distinct()
        }

        // 2. Direct Local Hotspot Subnet (192.168.43.x):
        if (localIp != null && localIp.startsWith("192.168.43.")) {
            if (localIp != "192.168.43.1") {
                candidates.add("192.168.43.1")
            }
            getArpClients().filter { it.startsWith("192.168.43.") }.forEach { candidates.add(it) }
            return candidates.filter { it != localIp }.distinct()
        }

        // 3. If on a standard Wi-Fi LAN, only include ARP entries on the exact same subnet
        // NEVER include the default internet router/gateway as a peer!
        if (localIp != null && !localIp.startsWith("127.")) {
            val prefix = localIp.substringBeforeLast(".") + "."
            getArpClients().filter { it.startsWith(prefix) && it != localIp }.forEach { candidates.add(it) }
        }

        return candidates
            .filter { it.isNotBlank() && it != "0.0.0.0" && it != "127.0.0.1" && it != localIp && !it.contains(":") }
            .distinct()
    }

    /**
     * Resolves the gateway IP or peer IP on the current Wi-Fi network.
     */
    fun getWifiGatewayOrPeerIp(context: Context): String? {
        return getCandidatePeerIps(context).firstOrNull()
    }

    /**
     * Reads /proc/net/arp to find active Wi-Fi clients connected to this device.
     */
    fun getArpClients(): List<String> {
        val result = mutableListOf<String>()
        try {
            val file = File("/proc/net/arp")
            if (file.exists() && file.canRead()) {
                file.forEachLine { line ->
                    val tokens = line.trim().split(Regex("\\s+"))
                    if (tokens.size >= 6 && tokens[0] != "IP") {
                        val ip = tokens[0]
                        val flags = tokens[2]
                        val iface = tokens[5]
                        // flags 0x2 = complete entry, iface starts with wlan, p2p, or ap
                        if (flags == "0x2" && (iface.startsWith("wlan") || iface.startsWith("p2p") || iface.startsWith("ap"))) {
                            result.add(ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * Probes whether a TCP port is open and responding on the target IP with a fast timeout (default 400ms).
     */
    fun isTcpPortReachable(ip: String, port: Int = 8988, timeoutMs: Int = 400): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
