package com.example.qlfs.share

import com.example.qlfs.util.TokenStore

class SessionManager(
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    tokenGenerator: () -> String = { TokenStore.generate() }
) {
    val token: String = tokenGenerator()
    val expiresAt: Long = timeProvider() + 15 * 60 * 1000

    fun isValid(tokenToCheck: String): Boolean {
        return !isExpired() && tokenToCheck == token
    }

    fun isExpired(): Boolean {
        return timeProvider() > expiresAt
    }

    fun remainingSeconds(): Long {
        val remaining = expiresAt - timeProvider()
        return if (remaining > 0) remaining / 1000 else 0
    }
}
