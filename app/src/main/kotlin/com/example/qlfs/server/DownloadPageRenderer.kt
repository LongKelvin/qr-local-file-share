package com.example.qlfs.server

import com.example.qlfs.model.SharedFile
import com.example.qlfs.util.FileSizeFormatter
import java.net.URLEncoder

object DownloadPageRenderer {

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun fileTypeLabel(mimeType: String): String = when {
        mimeType.startsWith("image/")                       -> "Image"
        mimeType.startsWith("audio/")                       -> "Audio"
        mimeType.startsWith("video/")                       -> "Video"
        mimeType == "application/pdf"                       -> "PDF"
        mimeType.startsWith("application/zip") ||
        mimeType == "application/x-zip-compressed" ||
        mimeType == "application/x-rar-compressed" ||
        mimeType == "application/x-7z-compressed"           -> "Archive"
        mimeType.startsWith("text/")                        -> "Text"
        else                                                -> "File"
    }

    fun render(files: List<SharedFile>, token: String, expiryMinutes: Long): String {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val fileItemsHtml = files.joinToString("\n") { file ->
            val sizeHuman  = FileSizeFormatter.format(file.size)
            val typeLabel  = fileTypeLabel(file.mimeType)
            val safeName   = file.name.escapeHtml()
            val isImage    = file.mimeType.startsWith("image/")

            val thumbHtml = if (isImage) {
                """<img class="thumb-img" src="/thumbnail?id=${file.id}" alt="$safeName" loading="lazy">"""
            } else {
                val emoji = when (typeLabel) {
                    "Audio"   -> "🎵"
                    "Video"   -> "🎬"
                    "PDF"     -> "📄"
                    "Archive" -> "🗜️"
                    "Text"    -> "📝"
                    else      -> "📁"
                }
                val bg = when (typeLabel) {
                    "Audio"   -> "#ede9fe"
                    "Video"   -> "#fee2e2"
                    "PDF"     -> "#fef9c3"
                    "Archive" -> "#d1fae5"
                    "Text"    -> "#e0f2fe"
                    else      -> "#f3f4f6"
                }
                """<div class="thumb-placeholder" style="background:$bg">$emoji</div>"""
            }

            """
            <div class="file-item">
                $thumbHtml
                <div class="file-info">
                    <div class="filename">$safeName</div>
                    <div class="meta"><span class="type-badge">$typeLabel</span> · $sizeHuman</div>
                </div>
                <a class="btn-small" href="/download?id=${file.id}&token=$encodedToken">⬇</a>
            </div>
            """.trimIndent()
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Files Ready — QLFS</title>
  <style>
    body{font-family:system-ui,sans-serif;max-width:480px;margin:48px auto;
         padding:16px;text-align:center;background:#f8f9fa;color:#212529}
    h1{font-size:1.6em;margin-bottom:24px}
    .file-item{display:flex;align-items:center;gap:12px;
               background:#fff;border-radius:12px;padding:12px;margin-bottom:12px;
               box-shadow:0 2px 4px rgba(0,0,0,0.05);text-align:left;}
    .thumb-img{width:56px;height:56px;border-radius:8px;object-fit:cover;flex-shrink:0;}
    .thumb-placeholder{width:56px;height:56px;border-radius:8px;flex-shrink:0;
                       display:flex;align-items:center;justify-content:center;font-size:1.8em;}
    .file-info{flex:1;min-width:0;}
    .filename{font-weight:600;font-size:0.95rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
    .meta{color:#6c757d;font-size:0.8rem;margin-top:4px;}
    .type-badge{background:#e8f0fe;color:#1a73e8;border-radius:4px;
                padding:1px 6px;font-size:0.75rem;font-weight:600;}
    .btn-small{display:flex;align-items:center;justify-content:center;
               background:#1a73e8;color:#fff;text-decoration:none;
               width:40px;height:40px;border-radius:20px;font-size:1.2rem;font-weight:bold;flex-shrink:0;}
    .btn-small:hover{background:#1558b0}
    .expires{font-size:0.8em;color:#adb5bd;margin-top:24px}
  </style>
</head>
<body>
  <h1>📂 Files Ready</h1>
  <div class="file-list">
    $fileItemsHtml
  </div>
  <p class="expires">Link expires in $expiryMinutes min</p>
</body>
</html>
        """.trimIndent()
    }
}
