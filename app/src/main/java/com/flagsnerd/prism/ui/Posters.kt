package com.flagsnerd.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flagsnerd.prism.data.Covers
import com.flagsnerd.prism.data.Series
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video

// ---------------------------------------------------------------------------
// Poster art fetched from the internet (see data/Covers.kt).
// While nothing is found (or covers are off) we fall back to a frame of the video.
// ---------------------------------------------------------------------------

/** Poster URL for a movie or a show; null until found. Cached on disk after the first lookup. */
@Composable
fun rememberCover(title: String, year: Int?, series: Boolean): State<String?> =
    produceState<String?>(null, title, year, series, Store.covers) {
        value = if (Store.covers) Covers.find(title, year, series) else null
    }

/** Poster of the movie, or of the show when [v] is an episode. */
@Composable
fun rememberCoverFor(v: Video): State<String?> {
    val isSeries = v.isEpisode
    return rememberCover(
        if (isSeries) (v.series ?: v.title) else v.searchTitle,
        if (isSeries) null else v.year,
        isSeries
    )
}

@Composable
fun CoverImage(url: String?, fallback: Video, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (url == null || failed) {
            Thumb(fallback, Modifier.fillMaxSize())
        } else {
            val req = remember(url) {
                ImageRequest.Builder(ctx).data(url).crossfade(450).build()
            }
            AsyncImage(
                model = req,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 2:3 poster card for a movie (or an episode, which shows its series poster). */
@Composable
fun MoviePoster(v: Video, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cover by rememberCoverFor(v)
    val frac = Store.fraction(v)
    val watched = Store.watched(v)
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.bounceClick(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(shape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
        ) {
            CoverImage(cover, v, Modifier.fillMaxSize())
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

/** 2:3 poster card for a whole show. */
@Composable
fun SeriesPoster(s: Series, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cover by rememberCover(s.name, null, true)
    val watched = s.episodes.count { Store.watched(it) }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.aspectRatio(2f / 3f).bounceClick(onClick = onClick).clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        CoverImage(cover, s.cover, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(
                s.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${s.episodes.size} episodes · $watched watched",
                color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
