package com.cyberdrop.transfer.core

import android.content.Context
import java.security.SecureRandom

/**
 * شناسه دستگاه در اولین اجرا ساخته می‌شود و مادام‌العمر همان می‌ماند.
 * قالب: CD-XXXX-XXXX  (بدون حروف/ارقام گیج‌کننده)
 */
object Identity {

    private const val PREF = "cyberdrop_identity"
    private const val KEY_ID = "device_id"
    private const val ALPHA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun get(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.getString(KEY_ID, null)?.let { return it }
        val rnd = SecureRandom()
        val sb = StringBuilder("CD-")
        repeat(4) { sb.append(ALPHA[rnd.nextInt(ALPHA.length)]) }
        sb.append('-')
        repeat(4) { sb.append(ALPHA[rnd.nextInt(ALPHA.length)]) }
        val id = sb.toString()
        sp.edit().putString(KEY_ID, id).apply()
        return id
    }

    /** ورودی کاربر را به قالب استاندارد تبدیل می‌کند؛ در صورت نامعتبر بودن رشته خالی. */
    fun normalize(input: String): String {
        var s = input.uppercase().filter { it.isLetterOrDigit() }
        if (s.startsWith("CD")) s = s.substring(2)
        if (s.length != 8) return ""
        return "CD-" + s.substring(0, 4) + "-" + s.substring(4)
    }

    fun isValid(input: String): Boolean = normalize(input).isNotEmpty()
}
