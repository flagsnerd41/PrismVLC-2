package com.flagsnerd.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video
import com.flagsnerd.prism.update.Updater
import java.util.Calendar

private fun greeting(name: String): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val g = when {
        h < 5 -> "Good night"
        h < 12 -> "Good morning"
        h < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) g else "$g, $name"
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Display, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun HomeScreen(
    videos: List<Video>,
    loading: Boolean,
    bottomPad: Dp,
    onPlay: (Video, Boolean) -> Unit,
    onSearch: () -> Unit,
    onUpdate: () -> Unit,
) {
    val version = Store.version
    val progress = remember(videos, version) {
        videos.filter { Store.inProgress(it) }.sortedByDescending { Store.last(it.id) }
    }
    val recent = remember(videos) { videos.take(120) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Prism", greeting(Store.userName)) {
                IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, "Search") }
            }
        }

        if (Updater.available != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onUpdate).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Update available", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Tap to download and install", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        if (progress.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Hero(progress.first(), onPlay)
            }
        }
        if (progress.size > 1) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Continue watching") }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(progress.drop(1), key = { it.id }) { v ->
                        VideoCard(v, { onPlay(v, false) }, Modifier.width(230.dp))
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Recently added") }
            items(recent, key = { it.id }) { v ->
                VideoCard(v, { onPlay(v, false) })
            }
        } else if (!loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No videos found", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Add some videos to your phone and they will show up here.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Hero(v: Video, onPlay: (Video, Boolean) -> Unit) {
    val left = ((v.durationMs - Store.position(v.id)) / 60000L).coerceAtLeast(1L)
    val shape = RoundedCornerShape(32.dp)
    Box(Modifier.fillMaxWidth().height(280.dp).clip(shape).clickable { onPlay(v, false) }) {
        Thumb(v, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.92f))
                )
            )
        )
        Text(
            "CONTINUE WATCHING",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp).glass(RoundedCornerShape(50), 0.14f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(
                v.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${v.subtitle} · $left min left",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .clickable { onPlay(v, false) }.padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Resume", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier.glass(CircleShape).clickable { onPlay(v, true) }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restart", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            ProgressLine(Store.fraction(v), Modifier.fillMaxWidth().clip(CircleShape))
        }
    }
}
