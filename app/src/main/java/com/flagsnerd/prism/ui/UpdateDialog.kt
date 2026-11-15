package com.flagsnerd.prism.ui

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.flagsnerd.prism.update.Updater
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun UpdateDialog(release: Updater.Release, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var progress by remember { mutableFloatStateOf(-1f) }
    var cancelled by remember { mutableStateOf(false) }

    fun install(file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        onDismiss()
    }

    fun start() {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(ctx, "Allow Prism to install updates, then tap Update again", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        progress = 0f
        val main = Handler(Looper.getMainLooper())
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        Thread {
            val file = File(dir, "Prism.apk")
            try {
                val c = URL(release.apkUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 20000
                c.readTimeout = 20000
                val total = c.contentLengthLong
                c.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            if (cancelled) return@Thread
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) main.post { progress = done.toFloat() / total }
                        }
                    }
                }
                main.post { install(file) }
            } catch (e: Exception) {
                main.post {
                    progress = -1f
                    Toast.makeText(ctx, "Download failed", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    val notes = release.notes.trim().take(500)
    AlertDialog(
        onDismissRequest = { if (progress < 0f) onDismiss() },
        title = { Text("Update available") },
        text = {
            Column {
                Text(if (notes.isEmpty()) "A new version of Prism is ready." else notes)
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (progress < 0f) start() }, enabled = progress < 0f) {
                Text(if (progress < 0f) "Update" else "Downloading…")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                cancelled = true
                onDismiss()
            }) { Text(if (progress < 0f) "Later" else "Cancel") }
        }
    )
}
