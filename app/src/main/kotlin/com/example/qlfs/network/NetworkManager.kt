package com.example.qlfs.network

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

open class NetworkManager(private val context: Context) {

    fun getLocalIpAddress(): String? {
        // Step 1: Try WifiManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            val wifiIp = wifiManager.connectionInfo.ipAddress
            if (wifiIp != 0) return intToIp(wifiIp)
        }

        // Step 2: Fallback — enumerate NetworkInterfaces
        val interfaces = getNetworkInterfaces()
        interfaces?.forEach { iface ->
            if (iface.isLoopback || !iface.isUp) return@forEach
            iface.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr is Inet4Address)
                    return addr.hostAddress
            }
        }
        return null
    }

    // Visible for testing
    protected open fun getNetworkInterfaces(): List<NetworkInterface>? {
        return NetworkInterface.getNetworkInterfaces()?.toList()
    }

    private fun intToIp(ip: Int) =
        "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
}
