package com.example.qlfs.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class HotspotInfo(val ssid: String, val password: String, val ip: String)

class HotspotManager(private val context: Context) {

    private var activeReservation: WifiManager.LocalOnlyHotspotReservation? = null

    /**
     * Starts the local-only hotspot and returns SSID, password, and the AP gateway IP.
     * Requires ACCESS_FINE_LOCATION (API 26–32) or NEARBY_WIFI_DEVICES (API 33+) at runtime.
     * Must be called from a coroutine.
     */
    suspend fun startHotspot(): HotspotInfo = suspendCancellableCoroutine { cont ->
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiManager.startLocalOnlyHotspot(
            object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    activeReservation = reservation

                    val ssid: String
                    val password: String

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = reservation.softApConfiguration
                        ssid = config.ssid ?: "QLFS_Share"
                        password = config.passphrase ?: ""
                    } else {
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        ssid = config?.SSID?.trim('"') ?: "QLFS_Share"
                        password = config?.preSharedKey?.trim('"') ?: ""
                    }

                    // Poll for the tethering interface instead of a fixed delay.
                    // Returns as soon as the IP is visible (often <200ms), fallback at 1500ms.
                    val handler = Handler(Looper.getMainLooper())
                    var pollCount = 0
                    fun poll() {
                        if (!cont.isActive) return
                        val ip = findHotspotIp()
                        if (ip != null) {
                            cont.resume(HotspotInfo(ssid, password, ip))
                        } else if (pollCount++ >= 14) { // 14 × 100ms = 1400ms max
                            cont.resume(HotspotInfo(ssid, password, "192.168.49.1"))
                        } else {
                            handler.postDelayed({ poll() }, 100)
                        }
                    }
                    poll()
                }

                override fun onFailed(reason: Int) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            Exception(
                                "Could not start hotspot (code $reason). " +
                                "Please grant the required permission and try again."
                            )
                        )
                    }
                }

                override fun onStopped() {
                    // Stopped externally — the reservation is no longer valid
                    activeReservation = null
                }
            },
            Handler(Looper.getMainLooper())
        )

        cont.invokeOnCancellation { stopHotspot() }
    }

    fun stopHotspot() {
        activeReservation?.close()
        activeReservation = null
    }

    /** Find the AP gateway IP from the network interface list. */
    private fun findHotspotIp(): String? = try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
            if (!iface.isUp || iface.isLoopback) return@forEach
            val name = iface.name
            // Skip cellular (rmnet*, ccmni*), dummies, p2p, loopback, Ethernet
            if (name.startsWith("rmnet") || name.startsWith("ccmni") ||
                name.startsWith("dummy") || name.startsWith("p2p") ||
                name.startsWith("lo") || name.startsWith("eth")) return@forEach
            iface.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        null
    } catch (e: Exception) { null }
}
