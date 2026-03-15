package com.example.qlfs.util

import android.util.Base64
import java.security.SecureRandom

object TokenStore {
    fun generate(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
