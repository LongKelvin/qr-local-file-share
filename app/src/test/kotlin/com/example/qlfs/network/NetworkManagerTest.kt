package com.example.qlfs.network

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkManagerTest {

    private class TestNetworkManager(
        context: Context, 
        private val mockInterfaces: List<NetworkInterface>?
    ) : NetworkManager(context) {
        override fun getNetworkInterfaces(): List<NetworkInterface>? {
            return mockInterfaces
        }
    }

    private lateinit var context: Context
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo

    @Before
    fun setup() {
        context = mockk()
        wifiManager = mockk()
        wifiInfo = mockk()
        
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
    }

    @Test
    fun testWifiIpIsReturned() {
        val ipInt = 192 or (168 shl 8) or (1 shl 16) or (1 shl 24)
        every { wifiInfo.ipAddress } returns ipInt
        
        val manager = TestNetworkManager(context, null)
        assertEquals("192.168.1.1", manager.getLocalIpAddress())
    }

    @Test
    fun testFallbackIpIsReturned() {
        every { wifiInfo.ipAddress } returns 0
        
        val mockInterface = mockk<NetworkInterface>()
        val mockInetAddress = mockk<Inet4Address>()
        
        every { mockInterface.isLoopback } returns false
        every { mockInterface.isUp } returns true
        every { mockInterface.inetAddresses } returns java.util.Collections.enumeration(listOf(mockInetAddress))
        every { mockInetAddress.isLoopbackAddress } returns false
        every { mockInetAddress.hostAddress } returns "192.168.43.1"
        
        val manager = TestNetworkManager(context, listOf(mockInterface))
        assertEquals("192.168.43.1", manager.getLocalIpAddress())
    }

    @Test
    fun testNullReturnedWhenNoInterfaceAvailable() {
        every { wifiInfo.ipAddress } returns 0
        
        val manager = TestNetworkManager(context, emptyList())
        assertNull(manager.getLocalIpAddress())
    }
}
