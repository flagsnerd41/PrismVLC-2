package com.flagsnerd.prism.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flagsnerd.prism.UiState
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.fmtTime
import com.flagsnerd.prism.player.PlayerController
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout
import kotlin.math.roundToInt

private fun speedLabel(s: Float): String =
    if (s == s.toInt().toFloat()) "${s.toInt()}x" else "${s}x"

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No activity")
}

@Composable
fun PlayerScreen(onClose: () -> Unit) {
    val v = PlayerController.current ?: return
    val ctx = LocalContext.current
    val activity = remember { ctx.findActivity() }
    val view = LocalView.current

    var controls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var scrub by remember { mutableStateOf<Float?>(null) }
    var hud by remember { mutableStateOf<String?>(null) }
    var hudValue by remember { mutableFloatStateOf(-1f) }
    var hudTick by remember { mutableIntStateOf(0) }
    var landscape by remember { mutableStateOf(true) }

    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol) }
    var bright by remember {
        val b = activity.window.attributes.screenBrightness
        mutableFloatStateOf(if (b < 0f) 0.5f else b)
    }

    BackHandler { onClose() }

    // immersive + keep awake + orientation
    DisposableEffect(Unit) {
        val w = activity.window
        val c = WindowCompat.getInsetsController(w, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Store.autoLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            c.show(WindowInsetsCompat.Type.systemBars())
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val lp = w.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            w.attributes = lp
        }
    }

    LaunchedEffect(controls, PlayerController.isPlaying, tick, dialog, scrub) {
        if (controls && PlayerController.isPlaying && dialog == null && scrub == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(hudTick) {
        if (hud != null) {
            delay(900)
            hud = null
        }
    }

    val length = if (PlayerController.lengthMs > 0) PlayerController.lengthMs else v.durationMs
    val shownFrac = scrub ?: if (length > 0) (PlayerController.timeMs.toFloat() / length) else 0f

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { c -> VLCVideoLayout(c).also { PlayerController.attach(it) } },
            onRelease = { PlayerController.detach() },
            modifier = Modifier.fillMaxSize()
        )

        // gesture zones: tap = controls, double tap = +/-10s, vertical drag = brightness (left) / volume (right)
        Row(Modifier.fillMaxSize()) {
            for (left in listOf(true, false)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .pointerInput(locked) {
                            detectTapGestures(
                                onTap = {
                                    controls = !controls
                                    tick++
                                },
                                onDoubleTap = {
                                    if (!locked) {
                                        PlayerController.seekBy(if (left) -10_000L else 10_000L)
                                        hud = if (left) "−10 s" else "+10 s"
                                        hudValue = -1f
                                        hudTick++
                                    }
                                }
                            )
                        }
                        .pointerInput(locked) {
                            if (!locked) {
                                detectVerticalDragGestures { change, dy ->
                                    change.consume()
                                    val h = size.height.toFloat() * 0.7f
                                    if (left) {
                                        bright = (bright - dy / h).coerceIn(0.01f, 1f)
                                        val lp = activity.window.attributes
                                        lp.screenBrightness = bright
                                        activity.window.attributes = lp
                                        hud = "Brightness"
                                        hudValue = bright
                                    } else {
                                        vol = (vol - dy / h).coerceIn(0f, 1f)
                                        am.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * maxVol).roundToInt(), 0)
                                        hud = "Volume"
                                        hudValue = vol
                                    }
                                    hudTick++
                                }
                            }
                        }
                )
            }
        }

        AnimatedVisibility(
            visible = controls && !UiState.inPip,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            if (locked) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) {
                    GlassIcon(Icons.Rounded.LockOpen, "Unlock", modifier = Modifier.align(Alignment.CenterStart)) {
                        locked = false
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                    )
                    Box(
                        Modifier.fillMaxWidth().height(240.dp).align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                    )

                    // top bar
                    Row(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIcon(Icons.Rounded.KeyboardArrowDown, "Minimize") { onClose() }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(
                                v.subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                v.title, color = Color.White, fontFamily = Display, fontSize = 20.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                        GlassIcon(Icons.Rounded.PictureInPictureAlt, "Picture in picture") {
                            activity.enterPictureInPictureMode(
                                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        GlassIcon(Icons.Rounded.Lock, "Lock controls") {
                            locked = true
                            controls = true
                            tick++
                        }
                    }

                    // centre transport
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIcon(Icons.Rounded.Replay10, "Back 10 seconds", size = 54.dp, iconSize = 28.dp) {
                            PlayerController.seekBy(-10_000L); tick++
                        }
                        GlassIcon(
                            if (PlayerController.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "Play or pause", size = 84.dp, iconSize = 44.dp
                        ) {
                            PlayerController.toggle(); tick++
                        }
                        GlassIcon(Icons.Rounded.Forward10, "Forward 10 seconds", size = 54.dp, iconSize = 28.dp) {
                            PlayerController.seekBy(10_000L); tick++
                        }
                    }

                    // bottom: times, seek bar, actions
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fmtTime((shownFrac * length).toLong()), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(fmtTime(length), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                        SeekBar(
                            progress = shownFrac,
                            onScrub = { scrub = it },
                            onDone = { p ->
                                PlayerController.seekTo((p * length).toLong())
                                scrub = null
                                tick++
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val next = PlayerController.nextVideo()
                            if (next != null) {
                                Row(
                                    Modifier.glass(CircleShape).clickable { PlayerController.playNext() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Next", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                GlassIcon(Icons.Rounded.MusicNote, "Audio track", size = 42.dp, iconSize = 21.dp) { dialog = "audio" }
                                GlassIcon(Icons.Rounded.Subtitles, "Subtitles", size = 42.dp, iconSize = 21.dp) { dialog = "subs" }
                                Box(
                                    Modifier.height(42.dp).glass(CircleShape).clickable { dialog = "speed" }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        speedLabel(PlayerController.speed),
                                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                                GlassIcon(Icons.Rounded.AspectRatio, "Fit or fill", size = 42.dp, iconSize = 21.dp) {
                                    PlayerController.toggleFill(); tick++
                                }
                                GlassIcon(Icons.Rounded.ScreenRotation, "Rotate", size = 42.dp, iconSize = 21.dp) {
                                    landscape = !landscape
                                    activity.requestedOrientation =
                                        if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                    tick++
                                }
                            }
                        }
                    }
                }
            }
        }

        // brightness / volume / seek feedback
        AnimatedVisibility(
            visible = hud != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 24.dp)
                    .glass(RoundedCornerShape(20.dp), 0.2f).padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(hud ?: "", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (hudValue >= 0f) {
                    Spacer(Modifier.height(6.dp))
                    ProgressLine(hudValue, Modifier.width(120.dp).clip(CircleShape))
                }
            }
        }
    }

    when (dialog) {
        "audio" -> TrackDialog(
            "Audio", PlayerController.audioTracks(), PlayerController.audioTrack(),
            onPick = { PlayerController.setAudioTrack(it); dialog = null },
            onDismiss = { dialog = null }
        )
        "subs" -> TrackDialog(
            "Subtitles", PlayerController.subtitleTracks(), PlayerController.subtitleTrack(),
            onPick = { PlayerController.setSubtitleTrack(it); dialog = null },
            onDismiss = { dialog = null }
        )
        "speed" -> {
            val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
            TrackDialog(
                "Speed",
                speeds.mapIndexed { i, s -> PlayerController.Track(i, speedLabel(s)) },
                speeds.indexOf(PlayerController.speed),
                onPick = { PlayerController.setSpeed(speeds[it]); dialog = null },
                onDismiss = { dialog = null }
            )
        }
    }
}

@Composable
private fun TrackDialog(
    title: String,
    entries: List<PlayerController.Track>,
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                if (entries.isEmpty()) {
                    Text("Nothing available", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                }
                LazyColumn(Modifier.height((entries.size.coerceIn(1, 6) * 52).dp)) {
                    items(entries, key = { it.id }) { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPick(t.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = t.id == selected, onClick = { onPick(t.id) })
                            Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
