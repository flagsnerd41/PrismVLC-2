package com.flagsnerd.prism.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.flagsnerd.prism.UiState
import com.flagsnerd.prism.data.Library
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video
import com.flagsnerd.prism.player.PlayerController
import com.flagsnerd.prism.update.Updater

private class NavDef(val label: String, val icon: ImageVector)

private val NavItems = listOf(
    NavDef("Home", Icons.Rounded.Home),
    NavDef("Series", Icons.Rounded.Tv),
    NavDef("Folders", Icons.Rounded.FolderOpen),
    NavDef("Settings", Icons.Rounded.Settings),
)

@Composable
fun PrismRoot() {
    PrismTheme {
        val ctx = LocalContext.current
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
        else Manifest.permission.READ_EXTERNAL_STORAGE

        var granted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED)
        }
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
        val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

        var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var tab by rememberSaveable { mutableStateOf(0) }
        var openSeries by rememberSaveable { mutableStateOf<String?>(null) }
        var openFolder by rememberSaveable { mutableStateOf<String?>(null) }
        var searching by rememberSaveable { mutableStateOf(false) }
        var showUpdate by remember { mutableStateOf(false) }

        LaunchedEffect(granted, UiState.refresh) {
            if (granted) videos = Library.load(ctx)
            loading = false
        }
        LaunchedEffect(Unit) { Updater.check(ctx) }
        LaunchedEffect(granted) {
            if (granted && Build.VERSION.SDK_INT >= 33 && !Store.notifAsked &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Store.notifAsked = true
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        LaunchedEffect(UiState.openUpdate) {
            if (UiState.openUpdate) {
                UiState.openUpdate = false
                Updater.check(ctx)
                showUpdate = true
            }
        }
        LaunchedEffect(UiState.pendingExternal) {
            val u = UiState.pendingExternal
            if (u != null) {
                UiState.pendingExternal = null
                val v = Video(
                    id = u.toString().hashCode().toLong(), uri = u,
                    fileName = u.lastPathSegment ?: "Video", folder = "External",
                    durationMs = 0L, dateAdded = 0L, size = 0L
                )
                PlayerController.play(v)
                PlayerController.showPlayer = true
            }
        }

        val series = remember(videos) { Library.series(videos) }
        val folders = remember(videos) { Library.folders(videos) }

        fun play(v: Video, fromStart: Boolean = false) {
            PlayerController.play(v, Library.queueFor(v, videos), fromStart)
            PlayerController.showPlayer = true
        }

        BackHandler(enabled = searching) { searching = false }
        BackHandler(enabled = !searching && (openSeries != null || openFolder != null)) {
            openSeries = null
            openFolder = null
        }

        val hasMini = PlayerController.current != null && !PlayerController.showPlayer
        val bottomPad = if (hasMini) 190.dp else 124.dp

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (!granted) {
                PermissionScreen { permLauncher.launch(perm) }
            } else {
                val s = openSeries?.let { name -> series.firstOrNull { it.name == name } }
                val f = openFolder?.let { name -> folders.firstOrNull { it.name == name } }
                when {
                    searching -> SearchScreen(videos, bottomPad, { searching = false }) { play(it) }
                    s != null -> SeriesDetail(s, bottomPad, { openSeries = null }) { v, fromStart -> play(v, fromStart) }
                    f != null -> FolderDetail(f, bottomPad, { openFolder = null }) { play(it) }
                    tab == 0 -> HomeScreen(
                        videos, loading, bottomPad,
                        onPlay = { v, fromStart -> play(v, fromStart) },
                        onSearch = { searching = true },
                        onUpdate = { showUpdate = true }
                    )
                    tab == 1 -> SeriesListScreen(series, bottomPad) { openSeries = it.name }
                    tab == 2 -> FoldersScreen(folders, bottomPad) { openFolder = it.name }
                    else -> SettingsScreen(bottomPad) { showUpdate = true }
                }

                Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                    MiniPlayer { PlayerController.showPlayer = true }
                    NavBar(tab) {
                        tab = it
                        openSeries = null
                        openFolder = null
                        searching = false
                    }
                }
            }

            if (PlayerController.showPlayer && PlayerController.current != null) {
                PlayerScreen(onClose = {
                    PlayerController.saveProgress(true)
                    PlayerController.showPlayer = false
                })
            }

            val rel = Updater.available
            if (showUpdate && rel != null) {
                UpdateDialog(rel) { showUpdate = false }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Prism", fontFamily = Display, fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
        Text(
            "Allow access to your videos so Prism can build your library.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 20.dp)
        )
        Button(onClick = onGrant) { Text("Allow access") }
    }
}

@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(34.dp)
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(66.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItems.forEachIndexed { i, item ->
            NavButton(item, i == selected) { onSelect(i) }
        }
    }
}

@Composable
private fun NavButton(item: NavDef, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "navbg"
    )
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(bg)
            .clickable(onClick = onClick).animateContentSize()
            .padding(horizontal = if (selected) 18.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, item.label, tint = fg, modifier = Modifier.size(24.dp))
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(item.label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MiniPlayer(onOpen: () -> Unit) {
    val v = PlayerController.current ?: return
    if (PlayerController.showPlayer) return
    val shape = RoundedCornerShape(26.dp)
    val frac = if (PlayerController.lengthMs > 0) PlayerController.timeMs.toFloat() / PlayerController.lengthMs else 0f
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(v, Modifier.size(width = 64.dp, height = 40.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(v.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            ProgressLine(frac, Modifier.fillMaxWidth().clip(RoundedCornerShape(50)))
        }
        IconButton(onClick = { PlayerController.toggle() }) {
            Icon(if (PlayerController.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause")
        }
        IconButton(onClick = { PlayerController.stop() }) { Icon(Icons.Rounded.Close, "Close") }
    }
}
