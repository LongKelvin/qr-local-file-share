package com.example.qlfs.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD

/**
 * Lightweight HTTP server that tries to bind to port 80.
 *
 * When a client device joins the hotspot its OS probes for internet
 * connectivity by sending HTTP requests to well-known URLs.  This server
 * intercepts those probes on the AP gateway IP and returns an HTTP 302
 * redirect to the QLFS download page.  The OS then treats the
 * non-204 / non-expected response as a captive portal and automatically
 * opens a browser — no second QR scan needed.
 *
 * Port 80 requires CAP_NET_BIND_SERVICE on Linux.  On most unrooted
 * Android devices [startIfPossible] will catch the BindException and
 * return false, and the fallback URL card in [SharingActiveScreen]
 * covers those devices.
 */
class CaptivePortalServer(
    private val downloadUrl: String
) : NanoHTTPD(80) {

    companion object {
        private const val TAG = "QLFS/CaptivePortal"

        // All well-known captive-portal probe paths across Android, iOS, Windows
        private val PROBE_PATHS = setOf(
            "/generate_204",              // Android ConnectivityManager (primary)
            "/gen_204",                   // Android (alternate)
            "/hotspot-detect.html",       // Apple iOS / macOS
            "/library/test/success.html", // Apple (alternate)
            "/connecttest.txt",           // Windows NCSI
            "/ncsi.txt",                  // Windows NCSI (alternate)
            "/redirect",                  // Samsung
            "/mobile/status.php",         // Carrier portals
            "/success.txt",               // Firefox connectivity check
            "/canonical.html",            // Ubuntu
        )
    }

    /**
     * Attempts to start the server on port 80.
     * Returns true if the port was successfully bound, false if denied (expected on most devices).
     */
    fun startIfPossible(): Boolean = try {
        start(SOCKET_READ_TIMEOUT, false)
        Log.d(TAG, "Captive portal server listening on :80 — probes will auto-trigger browser")
        true
    } catch (e: Exception) {
        Log.d(TAG, "Port 80 unavailable (expected on non-rooted devices): ${e.message}")
        false
    }

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri.lowercase()
        Log.d(TAG, "Intercepted captive probe: $path → redirecting to download page")
        return newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "").apply {
            addHeader("Location", downloadUrl)
            addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
        }
    }
}
