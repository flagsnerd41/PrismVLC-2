package com.flagsnerd.prism.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
    NavDef("Shows", Icons.Rounded.Tv),
    NavDef("Folders", Icons.Rounded.FolderOpen),
    NavDef("Settings", Icons.Rounded.Settings),
)

/** Which way a screen change slides: tabs by position, anything opened from a tab sits "deeper". */
private fun rank(key: String): Int =
    if (key.startsWith("tab")) key.removePrefix("tab").toIntOrNull() ?: 0 else 10

@Composable
fun PrismRoot() {
    PrismTheme {
        val ctx = LocalContext.current
        val cs = MaterialTheme.colorScheme
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
        var lastMini by remember { mutableStateOf<Video?>(null) }

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
            }
        }

        val series = remember(videos) { Library.series(videos) }
        val folders = remember(videos) { Library.folders(videos) }

        // play() opens the full player itself and waits for the video surface before starting
        fun play(v: Video, fromStart: Boolean = false) {
            PlayerController.play(v, Library.queueFor(v, videos), fromStart)
        }

        BackHandler(enabled = searching) { searching = false }
        BackHandler(enabled = !searching && (openSeries != null || openFolder != null)) {
            openSeries = null
            openFolder = null
        }

        val hasMini = PlayerController.current != null && !PlayerController.showPlayer
        val bottomPad = if (hasMini) 200.dp else 130.dp

        val screenKey = when {
            searching -> "search"
            openSeries != null -> "series:$openSeries"
            openFolder != null -> "folder:$openFolder"
            else -> "tab$tab"
        }

        // Everything behind the bars is recorded into this layer so the glass can blur it for real.
        val glassLayer = rememberGraphicsLayer()

        CompositionLocalProvider(LocalGlassSource provides glassLayer) {
            Box(Modifier.fillMaxSize().background(cs.background)) {
                Box(Modifier.fillMaxSize().glassSource(glassLayer).background(cs.background)) {
                    if (Store.aurora && !PlayerController.showPlayer) {
                        AuroraBackground()
                    }

                    if (!granted) {
                        PermissionScreen { permLauncher.launch(perm) }
                    } else {
                        AnimatedContent(
                            targetState = screenKey,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (Store.animations) {
                                    val dir = if (rank(targetState) >= rank(initialState)) 1 else -1
                                    (fadeIn(tween(260, delayMillis = 40)) +
                                        slideInHorizontally(
                                            animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow)
                                        ) { dir * (it / 7) } +
                                        scaleIn(
                                            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
                                            initialScale = 0.96f
                                        )) togetherWith
                                        (fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { -dir * (it / 12) })
                                } else {
                                    EnterTransition.None togetherWith ExitTransition.None
                                }
                            },
                            label = "screen"
                        ) { key ->
                            remember(key) { Stagger.reset() }
                            when {
                                key == "search" -> SearchScreen(videos, bottomPad, { searching = false }) { play(it) }
                                key.startsWith("series:") -> {
                                    val sd = series.firstOrNull { it.name == key.removePrefix("series:") }
                                    sd?.let { SeriesDetail(it, bottomPad, { openSeries = null }) { v, fromStart -> play(v, fromStart) } }
                                }
                                key.startsWith("folder:") -> {
                                    val fd = folders.firstOrNull { it.name == key.removePrefix("folder:") }
                                    fd?.let { FolderDetail(it, bottomPad, { openFolder = null }) { v -> play(v) } }
                                }
                                key == "tab0" -> HomeScreen(
                                    videos, series, loading, bottomPad,
                                    onPlay = { v, fromStart -> play(v, fromStart) },
                                    onOpenSeries = { openSeries = it.name },
                                    onSearch = { searching = true },
                                    onUpdate = { showUpdate = true }
                                )
                                key == "tab1" -> SeriesListScreen(series, bottomPad) { openSeries = it.name }
                                key == "tab2" -> FoldersScreen(folders, bottomPad) { openFolder = it.name }
                                else -> SettingsScreen(bottomPad) { showUpdate = true }
                            }
                        }
                    }
                }

                if (granted) {
                    val cur = PlayerController.current
                    LaunchedEffect(cur) { if (cur != null) lastMini = cur }
                    val miniVideo = cur ?: lastMini

                    Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                        AnimatedVisibility(
                            visible = hasMini && miniVideo != null,
                            enter = slideInVertically(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                            ) { it } + fadeIn(),
                            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160))
                        ) {
                            if (miniVideo != null) {
                                MiniPlayer(miniVideo) { PlayerController.showPlayer = true }
                            }
                        }
                        NavBar(tab) {
                            tab = it
                            openSeries = null
                            openFolder = null
                            searching = false
                        }
                    }
                }

                AnimatedVisibility(
                    visible = PlayerController.showPlayer && PlayerController.current != null,
                    enter = fadeIn(tween(220)) + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) { it / 6 },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 8 }
                ) {
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

/** Floating liquid-glass bar. The highlight is one pill that springs from tab to tab. */
@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(38.dp)
    val pill = RoundedCornerShape(32.dp)
    BoxWithConstraints(
        Modifier.padding(horizontal = 22.dp, vertical = 10.dp).fillMaxWidth().height(72.dp)
            .glassPanel(shape)
            .padding(6.dp)
    ) {
        val itemW = maxWidth / NavItems.size
        val x by animateDpAsState(
            targetValue = itemW * selected,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            label = "navx"
        )
        Box(
            Modifier.offset(x = x).width(itemW).fillMaxHeight()
                .clip(pill)
                .background(cs.primary.copy(alpha = 0.22f))
                .border(1.dp, cs.primary.copy(alpha = 0.35f), pill)
        )
        Row(Modifier.fillMaxSize()) {
            NavItems.forEachIndexed { i, item ->
                NavButton(item, i == selected, Modifier.weight(1f)) { onSelect(i) }
            }
        }
    }
}

@Composable
private fun NavButton(item: NavDef, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fg by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.onSurfaceVariant,
        label = "navfg"
    )
    val pop by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navpop"
    )
    Column(
        modifier.fillMaxHeight().bounceClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            item.icon, item.label, tint = fg,
            modifier = Modifier.size(24.dp).graphicsLayer {
                scaleX = pop
                scaleY = pop
            }
        )
        Text(
            item.label, color = fg, fontSize = 11.sp, maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun MiniPlayer(v: Video, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val frac = if (PlayerController.lengthMs > 0) PlayerController.timeMs.toFloat() / PlayerController.lengthMs else 0f
    Row(
        Modifier.padding(horizontal = 22.dp).fillMaxWidth()
            .bounceClick(onClick = onOpen)
            .glassPanel(shape)
            .padding(10.dp),
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
