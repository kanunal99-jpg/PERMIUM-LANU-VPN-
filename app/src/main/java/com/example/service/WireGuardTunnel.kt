package com.example.service

import com.wireguard.android.backend.Tunnel

class WireGuardTunnel(private val name: String) : Tunnel {
    private var state = Tunnel.State.DOWN

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        state = newState
    }

    fun getState(): Tunnel.State = state
}
