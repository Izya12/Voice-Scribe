package com.example.data.repository

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumableDownloaderTest {

    private val payload = ByteArray(300_000) { (it * 31 % 251).toByte() }

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** Local server; with [supportRange] it answers `Range` with 206/416. */
    private fun serverWith(supportRange: Boolean, rangeRequests: AtomicInteger = AtomicInteger()): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            if (supportRange) {
                val range = ex.requestHeaders.getFirst("Range")
                if (range != null) {
                    rangeRequests.incrementAndGet()
                    val start = range.removePrefix("bytes=").substringBefore("-").toLong()
                    if (start >= payload.size) {
                        ex.responseHeaders.set("Content-Range", "bytes */${payload.size}")
                        ex.sendResponseHeaders(416, -1)
                        return@createContext
                    }
                    val body = payload.copyOfRange(start.toInt(), payload.size)
                    ex.responseHeaders.set("Content-Range", "bytes $start-${payload.size - 1}/${payload.size}")
                    ex.sendResponseHeaders(206, body.size.toLong())
                    ex.responseBody.use { it.write(body) }
                    return@createContext
                }
            }
            ex.sendResponseHeaders(200, payload.size.toLong())
            ex.responseBody.use { it.write(payload) }
        }
        server.start()
        return server
    }

    private fun HttpServer.url() = "http://127.0.0.1:${address.port}/model.bin"

    @Test
    fun `fresh download writes complete file and returns sha`() {
        val server = serverWith(supportRange = true)
        val tmp = File.createTempFile("dl_", ".tmp")
        try {
            val hex = ResumableDownloader.download(server.url(), tmp) {}
            assertEquals(sha(payload), hex)
            assertTrue(tmp.readBytes().contentEquals(payload))
        } finally {
            tmp.delete()
            server.stop(0)
        }
    }

    @Test
    fun `resumes partial file from break point via range`() {
        val rangeRequests = AtomicInteger()
        val server = serverWith(supportRange = true, rangeRequests = rangeRequests)
        val tmp = File.createTempFile("dl_", ".tmp")
        try {
            tmp.writeBytes(payload.copyOfRange(0, payload.size * 4 / 10))
            val hex = ResumableDownloader.download(server.url(), tmp) {}
            assertEquals(sha(payload), hex)
            assertTrue(tmp.readBytes().contentEquals(payload))
            assertEquals(1, rangeRequests.get())
        } finally {
            tmp.delete()
            server.stop(0)
        }
    }

    @Test
    fun `server without range support restarts from zero`() {
        val server = serverWith(supportRange = false)
        val tmp = File.createTempFile("dl_", ".tmp")
        try {
            tmp.writeBytes(payload.copyOfRange(0, payload.size * 4 / 10))
            val hex = ResumableDownloader.download(server.url(), tmp) {}
            assertEquals(sha(payload), hex)
            assertTrue(tmp.readBytes().contentEquals(payload))
        } finally {
            tmp.delete()
            server.stop(0)
        }
    }

    @Test
    fun `already complete partial is verified without rewriting`() {
        val server = serverWith(supportRange = true)
        val tmp = File.createTempFile("dl_", ".tmp")
        try {
            tmp.writeBytes(payload)
            val hex = ResumableDownloader.download(server.url(), tmp) {}
            assertEquals(sha(payload), hex)
            assertTrue(tmp.readBytes().contentEquals(payload))
        } finally {
            tmp.delete()
            server.stop(0)
        }
    }
}