package com.flagsnerd.prism.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Finds poster art on the internet. No API key needed:
 *  - shows  -> TVmaze  (api.tvmaze.com)
 *  - movies -> iTunes Search (itunes.apple.com)
 * Results (and misses) are cached in SharedPreferences so each title is looked up once.
 */
object Covers {
    private val gate = Semaphore(3)
    private const val MISS_TTL_MS = 3L * 24 * 3600 * 1000

    private fun norm(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Poster URL, or null if none was found / we are offline. */
    suspend fun find(title: String, year: Int?, series: Boolean): String? {
        val t = title.trim()
        if (t.length < 2) return null
        val key = "cv:" + (if (series) "tv" else "mv") + ":" + norm(t) + ":" + (year ?: 0)
        val hit = readCache(key)
        if (hit != null) return hit.ifBlank { null }
        return gate.withPermit {
            val again = readCache(key)
            if (again != null) {
                again.ifBlank { null }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        val url = if (series) tv(t) else movie(t, year)
                        writeCache(key, url)
                        url
                    } catch (e: IOException) {
                        null // offline: do not remember the miss
                    } catch (e: Exception) {
                        writeCache(key, null)
                        null
                    }
                }
            }
        }
    }

    private fun readCache(key: String): String? {
        val v = Store.cacheGet(key) ?: return null
        if (v.startsWith("-|")) {
            val ts = v.substring(2).toLongOrNull() ?: 0L
            return if (System.currentTimeMillis() - ts < MISS_TTL_MS) "" else null
        }
        return v
    }

    private fun writeCache(key: String, url: String?) {
        Store.cachePut(key, url ?: ("-|" + System.currentTimeMillis()))
    }

    /** Returns the body, null when the server says "not found", throws IOException when offline. */
    private fun get(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("User-Agent", "Prism/1.0 (Android)")
            return if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }

    private fun tv(title: String): String? {
        val body = get("https://api.tvmaze.com/singlesearch/shows?q=" + enc(title)) ?: return null
        val img = JSONObject(body).optJSONObject("image") ?: return null
        val u = img.optString("original").ifBlank { img.optString("medium") }
        return if (u.isBlank()) null else u.replace("http://", "https://")
    }

    private fun movie(title: String, year: Int?): String? {
        val body = get("https://itunes.apple.com/search?media=movie&entity=movie&limit=8&term=" + enc(title))
            ?: return null
        val arr = JSONObject(body).optJSONArray("results") ?: return null
        val want = norm(title)
        var best: String? = null
        var bestScore = Int.MIN_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val art = o.optString("artworkUrl100")
            if (art.isBlank()) continue
            val name = norm(o.optString("trackName"))
            var score: Int
            if (name == want) score = 10
            else if (name.startsWith(want) || want.startsWith(name)) score = 4
            else continue
            val y = o.optString("releaseDate").take(4).toIntOrNull()
            if (year != null && y != null) {
                score += when {
                    y == year -> 5
                    kotlin.math.abs(y - year) <= 1 -> 2
                    else -> -3
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = art
            }
        }
        return best?.replace("100x100bb", "600x900bb")
    }
}
