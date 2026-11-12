package com.cyberdrop.transfer.core

import android.content.Context
import android.os.Build

class Prefs(ctx: Context) {

    private val sp = ctx.getSharedPreferences("cyberdrop_settings", Context.MODE_PRIVATE)

    var deviceName: String
        get() = sp.getString("device_name", null) ?: (Build.MODEL ?: "android")
        set(v) {
            sp.edit().putString("device_name", v).apply()
        }

    /** سقف حجم فایل برای ارسال */
    var maxBytes: Long
        get() = sp.getLong("max_bytes", DEFAULT_MAX)
        set(v) {
            sp.edit().putLong("max_bytes", v).apply()
        }

    var autoAccept: Boolean
        get() = sp.getBoolean("auto_accept", false)
        set(v) {
            sp.edit().putBoolean("auto_accept", v).apply()
        }

    var turnUrl: String
        get() = sp.getString("turn_url", "") ?: ""
        set(v) {
            sp.edit().putString("turn_url", v).apply()
        }

    var turnUser: String
        get() = sp.getString("turn_user", "") ?: ""
        set(v) {
            sp.edit().putString("turn_user", v).apply()
        }

    var turnPass: String
        get() = sp.getString("turn_pass", "") ?: ""
        set(v) {
            sp.edit().putString("turn_pass", v).apply()
        }

    /** نشستِ ارسالِ نیمه‌کاره، برای ادامه دادن پس از قطعی یا بستن برنامه. */
    var outgoing: String?
        get() = sp.getString("outgoing", null)
        set(v) {
            sp.edit().putString("outgoing", v).apply()
        }

    var filesJson: String?
        get() = sp.getString("files", null)
        set(v) {
            sp.edit().putString("files", v).apply()
        }

    var completedCount: Int
        get() = sp.getInt("completed_count", 0)
        set(v) {
            sp.edit().putInt("completed_count", v).apply()
        }

    var sentBytes: Long
        get() = sp.getLong("sent_bytes", 0L)
        set(v) {
            sp.edit().putLong("sent_bytes", v).apply()
        }

    companion object {
        const val GB = 1024L * 1024L * 1024L
        const val MB = 1024L * 1024L
        const val DEFAULT_MAX = 4 * GB
    }
}
