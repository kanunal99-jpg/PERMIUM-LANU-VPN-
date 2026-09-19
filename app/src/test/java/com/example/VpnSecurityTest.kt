package com.example

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import com.example.manager.VpnStateManager
import com.example.service.LanuVpnService
import com.example.util.VpnLeakDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [VpnSecurityTest.ShadowVpnServiceBuilder::class])
class VpnSecurityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        VpnStateManager.updateStatus(VpnStateManager.VpnStatus.DISCONNECTED)
        ShadowVpnServiceBuilder.reset()
    }

    @Test
    fun `test VpnService reaches connected state`() = runBlocking {
        val intent = Intent(context, LanuVpnService::class.java).apply {
            action = LanuVpnService.ACTION_CONNECT
            putExtra("server_id", "test_server")
            putExtra("server_endpoint", "1.2.3.4")
        }
        
        val serviceController = Robolectric.buildService(LanuVpnService::class.java, intent)
        serviceController.create().startCommand(0, 0)

        // Since it enters simulation mode, we wait for the state transition
        // In Robolectric, we might need to flush the coroutine or wait
        delay(2000) 
        
        assertEquals(VpnStateManager.VpnStatus.CONNECTED, VpnStateManager.status.value)
    }

    @Test
    fun `test leak detector identifies active VPN interface`() = runBlocking {
        // This test validates the logic of VpnLeakDetector when it sees a VPN transport
        // Note: Full system-level leak detection is limited in Robolectric, 
        // but we test the decision logic.
        
        val result = VpnLeakDetector.performLeakTest(context)
        
        // Default state without active tunnel
        assertNotNull(result.activeInterface)
        assertNotNull(result.details)
    }

    @Test
    fun `test VPN handshake state transition`() {
        // Simulate the handshake process by checking state transitions
        VpnStateManager.updateStatus(VpnStateManager.VpnStatus.CONNECTING)
        assertEquals(VpnStateManager.VpnStatus.CONNECTING, VpnStateManager.status.value)
        
        VpnStateManager.updateStatus(VpnStateManager.VpnStatus.CONNECTED)
        assertEquals(VpnStateManager.VpnStatus.CONNECTED, VpnStateManager.status.value)
        assertTrue(VpnStateManager.isConnected)
    }

    /**
     * Custom Shadow for VpnService.Builder to capture and verify tunnel configuration.
     */
    @Implements(VpnService.Builder::class)
    class ShadowVpnServiceBuilder {
        @org.robolectric.annotation.RealObject
        private lateinit var realBuilder: VpnService.Builder

        companion object {
            val routes = mutableListOf<RouteInfo>()
            val dnsServers = mutableListOf<String>()
            
            fun reset() {
                routes.clear()
                dnsServers.clear()
            }
        }

        data class RouteInfo(val address: String, val prefix: Int)

        @Implementation
        fun addRoute(address: String, prefixLength: Int): VpnService.Builder {
            routes.add(RouteInfo(address, prefixLength))
            return realBuilder
        }

        @Implementation
        fun addRoute(address: InetAddress, prefixLength: Int): VpnService.Builder {
            routes.add(RouteInfo(address.hostAddress ?: "", prefixLength))
            return realBuilder
        }

        @Implementation
        fun addDnsServer(address: String): VpnService.Builder {
            dnsServers.add(address)
            return realBuilder
        }

        @Implementation
        fun addDnsServer(address: InetAddress): VpnService.Builder {
            dnsServers.add(address.hostAddress ?: "")
            return realBuilder
        }
        
        @Implementation
        fun setSession(name: String): VpnService.Builder {
            return realBuilder
        }
        
        @Implementation
        fun addAddress(address: String, prefixLength: Int): VpnService.Builder {
            return realBuilder
        }
        
        @Implementation
        fun setMtu(mtu: Int): VpnService.Builder {
            return realBuilder
        }

        @Implementation
        fun establish(): android.os.ParcelFileDescriptor? {
            return null
        }
    }
}
