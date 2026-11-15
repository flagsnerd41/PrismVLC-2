package com.flagsnerd.prism.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.flagsnerd.prism.MainActivity
import com.flagsnerd.prism.data.Store
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/** Looks at the newest GitHub Release; the CI publishes one on every push to main. */
object Updater {
    const val REPO = "flagsnerd41/PrismVLC-2"
    private const val CHANNEL = "updates"
    private const val NOTIF_ID = 4201

    class Release(val versionCode: Long, val tag: String, val apkUrl: String, val notes: String)

    var available by mutableStateOf<Release?>(null)
    var checking by mutableStateOf(false)
    var status by mutableStateOf("")

    fun installedCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    fun installedName(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"

    /** Blocking - background thread only. */
    fun fetchLatest(): Release? = try {
        val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        if (c.responseCode != 200) null else {
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val tag = j.optString("tag_name")
            val code = Regex("(\\d+)$").find(tag)?.value?.toLongOrNull()
            val assets = j.optJSONArray("assets")
            var url: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        url = a.optString("browser_download_url")
                        break
                    }
                }
            }
            if (code == null || url == null) null else Release(code, tag, url, j.optString("body"))
        }
    } catch (e: Exception) {
        null
    }

    /** Check from the UI; updates [available] and [status]. */
    fun check(ctx: Context) {
        if (checking) return
        checking = true
        status = "Checking…"
        val app = ctx.applicationContext
        Thread {
            val r = fetchLatest()
            val newer = r != null && r.versionCode > installedCode(app)
            Handler(Looper.getMainLooper()).post {
                checking = false
                available = if (newer) r else null
                status = when {
                    r == null -> "Couldn't reach GitHub"
                    newer -> "Update available"
                    else -> "You're up to date"
                }
            }
        }.start()
    }

    /** Background check: notify once per new release. */
    fun checkAndNotify(ctx: Context) {
        val r = fetchLatest() ?: return
        if (r.versionCode <= installedCode(ctx)) return
        if (Store.notifiedBuild >= r.versionCode) return
        Store.notifiedBuild = r.versionCode
        notify(ctx)
    }

    private fun notify(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("App updates")
                .build()
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra("update", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Prism update available")
            .setContentText("Tap to download and install the new version")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        nm.notify(NOTIF_ID, n)
    }

    fun schedule(ctx: Context) {
        val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val wm = WorkManager.getInstance(ctx)
        wm.enqueueUniquePeriodicWork(
            "prism_update_periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS).setConstraints(online).build()
        )
        wm.enqueueUniqueWork(
            "prism_update_now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UpdateWorker>().setConstraints(online).build()
        )
    }
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Updater.checkAndNotify(applicationContext)
        return Result.success()
    }
}
