package com.example.data.repository

import com.example.core.domain.error.ModelManagerException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Resumable HTTP download (§8.2): writes [url] into [tmp], continuing from an
 * existing partial file via the `Range: bytes=<size>-` header (HTTP 206).
 *
 * Guarantees:
 * - A partial file is hashed first, so the returned SHA-256 always covers the
 *   complete file regardless of how many resume attempts it took.
 * - Servers that ignore `Range` (plain 200) restart from zero transparently.
 * - A partial that already satisfies the server size (416) is verified as-is.
 *
 * On network/HTTP failure the exception propagates and the partial [tmp] is
 * left in place for the caller to resume later.
 */
internal object ResumableDownloader {

    fun download(url: String, tmp: File, onProgress: (Float) -> Unit): String {
        var digest = MessageDigest.getInstance("SHA-256")
        var downloaded = if (tmp.exists()) tmp.length() else 0L
        if (downloaded > 0L) {
            FileInputStream(tmp).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
        }

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        if (downloaded > 0L) {
            connection.setRequestProperty("Range", "bytes=$downloaded-")
        }
        try {
            val code = connection.responseCode
            val resumeAccepted = code == HttpURLConnection.HTTP_PARTIAL
            when {
                code == HttpURLConnection.HTTP_OK && downloaded > 0L -> {
                    // The server ignored Range: restart from zero.
                    digest = MessageDigest.getInstance("SHA-256")
                    downloaded = 0L
                }
                code == 416 -> {
                    // The partial already covers the server-side file
                    // (Range Not Satisfiable).
                    return digest.digest().toHex()
                }
                code !in 200..299 -> {
                    throw ModelManagerException("HTTP $code while downloading $url")
                }
            }

            val fullTotal = connection.contentLengthLong + downloaded
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                FileOutputStream(tmp, resumeAccepted).use { out ->
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (fullTotal > 0L) {
                            onProgress(downloaded.toFloat() / fullTotal.toFloat())
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}