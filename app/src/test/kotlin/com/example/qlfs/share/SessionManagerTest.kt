package com.example.qlfs.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SessionManagerTest {

    @Test
    fun testTokenGeneration() {
        val manager1 = SessionManager(tokenGenerator = { UUID.randomUUID().toString() })
        val manager2 = SessionManager(tokenGenerator = { UUID.randomUUID().toString() })

        assertTrue(manager1.token.isNotEmpty())
        assertNotEquals(manager1.token, manager2.token)
    }

    @Test
    fun testIsValidReturnsTrueWithinTTLAndCorrectToken() {
        var currentTime = 1000L
        val manager = SessionManager(timeProvider = { currentTime }, tokenGenerator = { "test-token" })

        assertTrue(manager.isValid("test-token"))
    }

    @Test
    fun testIsValidReturnsFalseWithWrongToken() {
        var currentTime = 1000L
        val manager = SessionManager(timeProvider = { currentTime }, tokenGenerator = { "test-token" })

        assertFalse(manager.isValid("wrong-token"))
    }

    @Test
    fun testIsValidReturnsFalseAfterTTLFails() {
        var currentTime = 1000L
        val manager = SessionManager(timeProvider = { currentTime }, tokenGenerator = { "test-token" })

        currentTime += 16 * 60 * 1000 // Advance 16 minutes

        assertFalse(manager.isValid("test-token"))
        assertTrue(manager.isExpired())
    }

    @Test
    fun testRemainingSeconds() {
        var currentTime = 1000L
        val manager = SessionManager(timeProvider = { currentTime }, tokenGenerator = { "test-token" })

        assertEquals(900L, manager.remainingSeconds())
        
        currentTime += 5000L // Advance 5 seconds
        assertEquals(895L, manager.remainingSeconds())
    }
}
