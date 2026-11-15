package com.flagsnerd.prism

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Small bag of app-wide UI state that the Activity can poke. */
object UiState {
    var inPip by mutableStateOf(false)
    var refresh by mutableIntStateOf(0)
    var openUpdate by mutableStateOf(false)
    var pendingExternal by mutableStateOf<Uri?>(null)
}

fun fmtTime(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.ROOT, "%d:%02d", m, sec)
}
