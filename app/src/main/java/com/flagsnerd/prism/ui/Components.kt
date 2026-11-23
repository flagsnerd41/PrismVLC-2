package com.flagsnerd.prism.ui

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video
import com.flagsnerd.prism.fmtTime

// Modifier.glass / glassPanel / bounceClick live in Glass.kt

@Composable
fun GlassIcon(
    icon: ImageVector,
    desc: String,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.bounceClick(onClick = onClick).size(size).glass(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** Video frame used as a thumbnail. [large] = sharper frame for big hero banners. */
@Composable
fun Thumb(v: Video, modifier: Modifier = Modifier, large: Boolean = false) {
    val ctx = LocalContext.current
    val req = remember(v.id, large) {
        ImageRequest.Builder(ctx)
            .data(v.uri)
            .videoFrameMillis(maxOf(1000L, v.durationMs / 8))
            .size(if (large) 1080 else 480, if (large) 1080 else 270)
            .crossfade(true)
            .build()
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = req,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ProgressLine(frac: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(3.dp).background(Color.White.copy(alpha = 0.25f))) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun VideoCard(v: Video, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val frac = Store.fraction(v)
    val watched = Store.watched(v)
    val thumbShape = RoundedCornerShape(22.dp)
    Column(modifier.bounceClick(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(thumbShape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), thumbShape)
        ) {
            Thumb(v, Modifier.fillMaxSize())
            if (v.durationMs > 0) {
                Chip(fmtTime(v.durationMs), Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
            if (frac > 0.02f && !watched) {
                ProgressLine(frac, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
            }
            if (watched) {
                Icon(
                    Icons.Rounded.Check, "Watched", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).padding(3.dp)
                )
            }
        }
        Text(
            v.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            v.subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
        )
    }
}

@Composable
fun VideoRow(v: Video, onClick: () -> Unit, label: String? = null) {
    val frac = Store.fraction(v)
    val watched = Store.watched(v)
    Row(
        Modifier.fillMaxWidth().bounceClick(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp))) {
            Thumb(v, Modifier.fillMaxSize())
            if (frac > 0.02f && !watched) {
                ProgressLine(frac, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label ?: v.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val info = if (v.durationMs > 0) "${fmtTime(v.durationMs)} · ${v.folder}" else v.folder
            Text(
                info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (watched) {
            Icon(
                Icons.Rounded.Check, "Watched", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
fun SeekBar(
    progress: Float,
    onScrub: (Float) -> Unit,
    onDone: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val thumb by animateDpAsState(if (dragging) 20.dp else 12.dp, label = "thumb")
    val scrubCb by rememberUpdatedState(onScrub)
    val doneCb by rememberUpdatedState(onDone)
    Box(
        modifier
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val p = (o.x / size.width).coerceIn(0f, 1f)
                    scrubCb(p)
                    doneCb(p)
                }
            }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        last = (o.x / size.width).coerceIn(0f, 1f)
                        scrubCb(last)
                    },
                    onDragEnd = {
                        dragging = false
                        doneCb(last)
                    },
                    onDragCancel = {
                        dragging = false
                        doneCb(last)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        last = (change.position.x / size.width).coerceIn(0f, 1f)
                        scrubCb(last)
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val h = 4.dp.toPx()
            val y = size.height / 2f
            val pr = progress.coerceIn(0f, 1f)
            drawRoundRect(
                Color.White.copy(alpha = 0.28f),
                Offset(0f, y - h / 2f),
                Size(size.width, h),
                CornerRadius(h / 2f)
            )
            drawRoundRect(
                Color.White,
                Offset(0f, y - h / 2f),
                Size(size.width * pr, h),
                CornerRadius(h / 2f)
            )
            drawCircle(Color.White, thumb.toPx() / 2f, Offset(size.width * pr, y))
        }
    }
}
