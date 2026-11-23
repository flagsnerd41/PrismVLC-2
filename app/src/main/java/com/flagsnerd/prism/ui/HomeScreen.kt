package com.flagsnerd.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.flagsnerd.prism.data.Series
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

/** Header used by the inner screens (Series, Folders, Settings). */
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
private fun RailTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp)
    )
}

/**
 * Home = a cinematic hero on top (bleeds under the status bar), then poster rails.
 * Posters come from the internet when found, otherwise a frame of the video.
 */
@Composable
fun HomeScreen(
    videos: List<Video>,
    series: List<Series>,
    loading: Boolean,
    bottomPad: Dp,
    onPlay: (Video, Boolean) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onSearch: () -> Unit,
    onUpdate: () -> Unit,
) {
    val version = Store.version
    val progress = remember(videos, version) {
        videos.filter { Store.inProgress(it) }.sortedByDescending { Store.last(it.id) }
    }
    val resuming = progress.isNotEmpty()
    val hero = progress.firstOrNull() ?: videos.firstOrNull()
    val more = if (resuming) progress.drop(1) else emptyList()
    val movies = remember(videos) { videos.filter { !it.isEpisode }.take(40) }
    val recent = remember(videos) { videos.take(24) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPad),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            if (hero != null) {
                HeroBanner(hero, resuming, onPlay, onSearch)
            } else {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    ScreenHeader("Prism", greeting(Store.userName)) {
                        GlassIcon(Icons.Rounded.Search, "Search", size = 44.dp, iconSize = 22.dp, onClick = onSearch)
                    }
                    if (!loading) {
                        Text("No videos found", fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 80.dp))
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

        if (Updater.available != null) {
            item {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
                        .bounceClick(onClick = onUpdate)
                        .glass(RoundedCornerShape(24.dp), 0.14f)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Update available", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Tap to download and install", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (more.isNotEmpty()) {
            item {
                Column(Modifier.staggerIn(0, "rail_continue")) {
                    RailTitle("Continue watching")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(more, key = { it.id }) { v ->
                            VideoCard(v, { onPlay(v, false) }, Modifier.width(250.dp))
                        }
                    }
                }
            }
        }

        if (series.isNotEmpty()) {
            item {
                Column(Modifier.staggerIn(1, "rail_series")) {
                    RailTitle("Shows")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(series, key = { it.name }) { s ->
                            SeriesPoster(s, { onOpenSeries(s) }, Modifier.width(138.dp))
                        }
                    }
                }
            }
        }

        if (movies.isNotEmpty()) {
            item {
                Column(Modifier.staggerIn(2, "rail_movies")) {
                    RailTitle("Movies")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(movies, key = { it.id }) { v ->
                            MoviePoster(v, { onPlay(v, false) }, Modifier.width(138.dp))
                        }
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                Column(Modifier.staggerIn(3, "rail_recent")) {
                    RailTitle("Recently added")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recent, key = { it.id }) { v ->
                            VideoCard(v, { onPlay(v, false) }, Modifier.width(250.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    v: Video,
    resuming: Boolean,
    onPlay: (Video, Boolean) -> Unit,
    onSearch: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val cover by rememberCoverFor(v)
    val poster = cover
    val left = ((v.durationMs - Store.position(v.id)) / 60000L).coerceAtLeast(1L)

    Box(Modifier.fillMaxWidth().height(500.dp).staggerIn(0, "hero")) {
        // backdrop: a sharp frame of the video, fading into the page colour
        Thumb(v, Modifier.fillMaxSize(), large = true)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.30f to Color.Transparent,
                    0.62f to cs.background.copy(alpha = 0.72f),
                    1f to cs.background
                )
            )
        )

        // top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Prism", fontFamily = Display, fontSize = 32.sp, color = Color.White)
                Text(greeting(Store.userName), fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
            GlassIcon(Icons.Rounded.Search, "Search", size = 46.dp, iconSize = 22.dp, onClick = onSearch)
        }

        // poster + details
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (poster != null) {
                val shape = RoundedCornerShape(22.dp)
                Box(
                    Modifier.width(122.dp).aspectRatio(2f / 3f).clip(shape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
                ) {
                    CoverImage(poster, v, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (resuming) "CONTINUE WATCHING" else "LATEST ADDITION",
                    color = cs.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    v.title,
                    color = cs.onBackground,
                    fontFamily = Display,
                    fontSize = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    if (resuming) "${v.subtitle} · $left min left" else v.subtitle,
                    color = cs.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.bounceClick { onPlay(v, false) }
                            .clip(CircleShape).background(cs.primary)
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = cs.onPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (resuming) "Resume" else "Play",
                            color = cs.onPrimary, fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (resuming) {
                        Spacer(Modifier.width(10.dp))
                        Row(
                            Modifier.bounceClick { onPlay(v, true) }
                                .glass(CircleShape)
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restart", color = cs.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (resuming) {
                    Spacer(Modifier.height(16.dp))
                    ProgressLine(Store.fraction(v), Modifier.fillMaxWidth().clip(CircleShape))
                }
            }
        }
    }
}
