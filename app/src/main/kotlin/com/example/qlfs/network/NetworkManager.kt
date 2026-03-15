package com.example.qlfs.network

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

enum class NetworkMode { WIFI, HOTSPOT, NONE }

data class NetworkInfo(val ip: String?, val mode: NetworkMode)

open class NetworkManager(private val context: Context) {

    fun getNetworkInfo(): NetworkInfo {
        // Step 1: Try WifiManager (device is connected to WiFi as a client)
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            val wifiIp = wifiManager.connectionInfo.ipAddress
            if (wifiIp != 0) return NetworkInfo(intToIp(wifiIp), NetworkMode.WIFI)
        }

        // Step 2: Enumerate NetworkInterfaces — covers hotspot/AP mode.
        // When the phone is acting as a Wi-Fi hotspot, WifiManager returns 0 because
        // the phone is the AP, not a station. The AP gateway IP is still reachable via
        // NetworkInterface enumeration.
        val interfaces = getNetworkInterfaces()
        interfaces?.forEach { iface ->
            if (iface.isLoopback || !iface.isUp) return@forEach
            // Skip cellular (rmnet*), virtual (dummy*), and p2p interfaces
            val name = iface.name
            if (name.startsWith("rmnet") || name.startsWith("ccmni") ||
                name.startsWith("dummy") || name.startsWith("p2p") ||
                name.startsWith("lo")) return@forEach

            iface.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip != null) return NetworkInfo(ip, NetworkMode.HOTSPOT)
                }
            }
        }

        return NetworkInfo(null, NetworkMode.NONE)
    }

    // Keep for backward compatibility
    fun getLocalIpAddress(): String? = getNetworkInfo().ip

    // Visible for testing
    protected open fun getNetworkInterfaces(): List<NetworkInterface>? {
        return NetworkInterface.getNetworkInterfaces()?.toList()
    }

    private fun intToIp(ip: Int) =
        "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
}
