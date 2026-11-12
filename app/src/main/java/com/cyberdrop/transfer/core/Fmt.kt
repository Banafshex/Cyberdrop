package com.cyberdrop.transfer.core

import java.util.Locale

object Fmt {

    fun bytes(b: Long): String {
        if (b < 0) return "—"
        val kb = 1024.0
        return when {
            b < 1024 -> "$b B"
            b < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", b / kb)
            b < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", b / (kb * kb))
            else -> String.format(Locale.US, "%.2f GB", b / (kb * kb * kb))
        }
    }

    fun speed(bps: Long): String =
        if (bps <= 0) "—" else bytes(bps) + "/s"

    fun eta(done: Long, total: Long, bps: Long): String {
        if (bps <= 0 || total <= 0 || done >= total) return "—"
        val sec = (total - done) / bps
        return when {
            sec < 60 -> "${sec}s"
            sec < 3600 -> "${sec / 60}m ${sec % 60}s"
            else -> "${sec / 3600}h ${(sec % 3600) / 60}m"
        }
    }

    fun hex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) {
            val v = x.toInt() and 0xff
            sb.append("0123456789abcdef"[v shr 4])
            sb.append("0123456789abcdef"[v and 0x0f])
        }
        return sb.toString()
    }

    fun shortHex(s: String, n: Int = 8): String = if (s.length <= n) s else s.substring(0, n)
}
