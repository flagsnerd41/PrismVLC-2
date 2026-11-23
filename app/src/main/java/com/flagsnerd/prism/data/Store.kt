package com.flagsnerd.prism.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Watch progress + settings. Reads are snapshot-tracked so the UI refreshes when [version] bumps. */
object Store {
    private lateinit var p: SharedPreferences

    var version by mutableIntStateOf(0)
        private set

    fun init(ctx: Context) {
        p = ctx.applicationContext.getSharedPreferences("prism", Context.MODE_PRIVATE)
    }

    private fun track(): Int = version

    // ---- progress ----
    fun position(id: Long): Long { track(); return p.getLong("pos_$id", 0L) }
    fun last(id: Long): Long { track(); return p.getLong("last_$id", 0L) }

    fun fraction(v: Video): Float {
        if (v.durationMs <= 0) return 0f
        return (position(v.id).toFloat() / v.durationMs).coerceIn(0f, 1f)
    }
    fun watched(v: Video): Boolean = fraction(v) > 0.93f
    fun inProgress(v: Video): Boolean = position(v.id) > 15_000L && !watched(v)

    fun save(id: Long, pos: Long, notify: Boolean) {
        p.edit().putLong("pos_$id", pos).putLong("last_$id", System.currentTimeMillis()).apply()
        if (notify) version++
    }

    // ---- settings ----
    var palette: String
        get() { track(); return p.getString("palette", "you") ?: "you" }
        set(v) { p.edit().putString("palette", v).apply(); version++ }

    var userName: String
        get() { track(); return p.getString("name", "") ?: "" }
        set(v) { p.edit().putString("name", v).apply(); version++ }

    var autoNext: Boolean
        get() { track(); return p.getBoolean("auto_next", true) }
        set(v) { p.edit().putBoolean("auto_next", v).apply(); version++ }

    var autoLandscape: Boolean
        get() { track(); return p.getBoolean("auto_landscape", true) }
        set(v) { p.edit().putBoolean("auto_landscape", v).apply(); version++ }

    /** Slowly drifting colour blobs behind the app. */
    var aurora: Boolean
        get() { track(); return p.getBoolean("aurora", true) }
        set(v) { p.edit().putBoolean("aurora", v).apply(); version++ }

    /** Springy presses, screen transitions, entrance animations. */
    var animations: Boolean
        get() { track(); return p.getBoolean("animations", true) }
        set(v) { p.edit().putBoolean("animations", v).apply(); version++ }

    /** 0.5 = subtle, 1 = default, 1.6 = very glassy. */
    var glassLevel: Float
        get() { track(); return p.getFloat("glass_level", 1f) }
        set(v) { p.edit().putFloat("glass_level", v).apply(); version++ }

    /** Download movie / show posters from the internet. */
    var covers: Boolean
        get() { track(); return p.getBoolean("covers", true) }
        set(v) { p.edit().putBoolean("covers", v).apply(); version++ }

    var notifAsked: Boolean
        get() = p.getBoolean("notif_asked", false)
        set(v) { p.edit().putBoolean("notif_asked", v).apply() }

    var notifiedBuild: Long
        get() = p.getLong("notified_build", 0L)
        set(v) { p.edit().putLong("notified_build", v).apply() }

    // ---- cover cache (not snapshot-tracked) ----
    fun cacheGet(key: String): String? = p.getString(key, null)

    fun cachePut(key: String, value: String) {
        p.edit().putString(key, value).apply()
    }

    fun clearCovers() {
        val e = p.edit()
        p.all.keys.filter { it.startsWith("cv:") }.forEach { e.remove(it) }
        e.apply()
        version++
    }
}
