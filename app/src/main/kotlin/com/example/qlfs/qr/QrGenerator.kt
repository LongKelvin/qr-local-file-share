package com.example.qlfs.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrGenerator {
    fun generate(url: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        // Build the pixel array in one pass then write it in a single bulk JNI call
        // instead of sizePx×sizePx individual setPixel() calls.
        val pixels = IntArray(sizePx * sizePx) { i ->
            if (matrix[i % sizePx, i / sizePx]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }
}
