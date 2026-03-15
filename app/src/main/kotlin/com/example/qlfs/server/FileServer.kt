package com.example.qlfs.server

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import com.example.qlfs.model.SharedFile
import com.example.qlfs.share.SessionManager
import com.example.qlfs.share.TransferEvent
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class FileServer(
    port: Int,
    private val files: List<SharedFile>,
    private val contentResolver: ContentResolver,
    private val session: SessionManager,
    private val onTransferEvent: (TransferEvent) -> Unit
) : NanoHTTPD(port) {

    private val activeConnections = AtomicInteger(0)

    init {
        val executor = Executors.newFixedThreadPool(5)
        setAsyncRunner(object : NanoHTTPD.AsyncRunner {
            override fun closeAll() {}
            override fun closed(clientHandler: NanoHTTPD.ClientHandler?) {}
            override fun exec(code: NanoHTTPD.ClientHandler?) {
                if (code != null) executor.execute(code)
            }
        })
    }

    override fun serve(httpSession: IHTTPSession): Response {
        return try {
            handleRequest(httpSession)
        } catch (e: Exception) {
            Log.e("QLFS", "Unhandled error", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error")
        }
    }

    private fun handleRequest(httpSession: IHTTPSession): Response {
        val method = httpSession.method
        val uri = httpSession.uri
        val params = httpSession.parameters

        if (activeConnections.get() >= 5) {
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, MIME_PLAINTEXT, "Service Unavailable")
        }

        return try {
            activeConnections.incrementAndGet()

            if (method == Method.GET && uri == "/") {
                val token = params["token"]?.firstOrNull() ?: ""
                val html = DownloadPageRenderer.render(files, token, session.remainingSeconds() / 60)
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html).apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                    addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                }
            }

            if ((method == Method.GET || method == Method.HEAD) && uri == "/download") {
                val token = params["token"]?.firstOrNull() ?: ""
                if (!session.isValid(token)) {
                    return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
                }

                val id = params["id"]?.firstOrNull() ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing file id")
                val targetFile = files.find { it.id == id } 
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")

                val inputStream = try {
                    contentResolver.openInputStream(targetFile.uri)
                } catch (e: Exception) {
                    null
                }

                if (inputStream == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
                }

                onTransferEvent(TransferEvent.Started)

                if (method == Method.HEAD) {
                    inputStream.close()
                    onTransferEvent(TransferEvent.Completed)
                    return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "").apply {
                        addHeader("Content-Disposition", "attachment; filename*=UTF-8''${encodeFilename(targetFile.name)}")
                        addHeader("Content-Length", targetFile.size.toString())
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Access-Control-Allow-Origin", "*")
                        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    }
                }

                // Handle GET request
                val rangeHeader = httpSession.headers["range"]
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val rangeSpan = rangeHeader.substring("bytes=".length)
                    val rangeParts = rangeSpan.split("-")
                    var startFrom = 0L
                    try {
                         if (rangeParts[0].isNotEmpty()) {
                             startFrom = rangeParts[0].toLong()
                         }
                         inputStream.skip(startFrom)
                    } catch (e: Exception) {
                         // ignore invalid range format parsing error
                    }
                    val length = targetFile.size - startFrom
                    val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, "application/octet-stream", inputStream, length).apply {
                        addHeader("Content-Disposition", "attachment; filename*=UTF-8''${encodeFilename(targetFile.name)}")
                        addHeader("Content-Range", "bytes $startFrom-${targetFile.size - 1}/${targetFile.size}")
                        addHeader("Content-Length", length.toString())
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Access-Control-Allow-Origin", "*")
                        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    }
                    onTransferEvent(TransferEvent.Completed)
                    return res
                } else {
                    val res = newFixedLengthResponse(Response.Status.OK, "application/octet-stream", inputStream, targetFile.size).apply {
                        addHeader("Content-Disposition", "attachment; filename*=UTF-8''${encodeFilename(targetFile.name)}")
                        addHeader("Content-Length", targetFile.size.toString())
                        addHeader("Accept-Ranges", "bytes")
                        addHeader("Access-Control-Allow-Origin", "*")
                        addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    }
                    onTransferEvent(TransferEvent.Completed)
                    return res
                }
            }

            if (method == Method.GET && uri == "/info") {
                val json = "[" + files.joinToString(",") { 
                    """{"id":"${it.id}","name":"${it.name}","size":${it.size},"mimeType":"${it.mimeType}"}"""
                } + "]"
                return newFixedLengthResponse(Response.Status.OK, "application/json", json).apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                    addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                }
            }

            if (method == Method.GET && uri == "/thumbnail") {
                val id = params["id"]?.firstOrNull()
                    ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing id")
                val targetFile = files.find { it.id == id }
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")

                if (!targetFile.mimeType.startsWith("image/")) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not an image")
                }

                return try {
                    val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentResolver.loadThumbnail(targetFile.uri, Size(256, 256), null)
                    } else {
                        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(targetFile.uri)?.use {
                            BitmapFactory.decodeStream(it, null, boundsOpts)
                        }
                        val sampleSize = calculateSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, 256, 256)
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        contentResolver.openInputStream(targetFile.uri)?.use {
                            BitmapFactory.decodeStream(it, null, decodeOpts)
                        }
                    }

                    if (bitmap == null) {
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Could not generate thumbnail")
                    }

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val bytes = baos.toByteArray()
                    newFixedLengthResponse(
                        Response.Status.OK, "image/jpeg",
                        ByteArrayInputStream(bytes), bytes.size.toLong()
                    ).apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                        addHeader("Cache-Control", "max-age=3600")
                    }
                } catch (e: Exception) {
                    Log.e("QLFS", "Thumbnail generation error", e)
                    newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error generating thumbnail")
                }
            }

            newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method Not Allowed")
        } catch (e: Exception) {
            onTransferEvent(TransferEvent.Failed(e))
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Server Error")
        } finally {
            activeConnections.decrementAndGet()
        }
    }

    private fun encodeFilename(name: String): String =
        URLEncoder.encode(name, "UTF-8").replace("+", "%20")

    private fun calculateSampleSize(width: Int, height: Int, targetW: Int, targetH: Int): Int {
        var sampleSize = 1
        if (width > targetW || height > targetH) {
            var halfW = width / 2
            var halfH = height / 2
            while (halfW / sampleSize >= targetW && halfH / sampleSize >= targetH) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}
