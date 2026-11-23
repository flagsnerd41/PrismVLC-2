package com.flagsnerd.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flagsnerd.prism.data.Folder
import com.flagsnerd.prism.data.Series
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video
import com.flagsnerd.prism.fmtTime

@Composable
private fun BackHeader(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, fontFamily = Display, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- series

@Composable
fun SeriesListScreen(series: List<Series>, bottomPad: Dp, onOpen: (Series) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomPad),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Shows", "${series.size} shows")
        }
        if (series.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Name your files like Show.S01E02.mkv and they will be grouped here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        }
        items(series, key = { it.name }) { s ->
            SeriesPoster(s, { onOpen(s) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SeriesDetail(
    s: Series,
    bottomPad: Dp,
    onBack: () -> Unit,
    onPlay: (Video, Boolean) -> Unit,
) {
    val version = Store.version
    val target = remember(s, version) {
        s.episodes.firstOrNull { Store.inProgress(it) }
            ?: s.episodes.firstOrNull { !Store.watched(it) }
            ?: s.episodes.first()
    }
    val seasons = remember(s) { s.episodes.groupBy { it.season }.toSortedMap() }
    val resuming = Store.inProgress(target)
    val cover by rememberCover(s.name, null, true)
    val poster = cover

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
        modifier = Modifier.fillMaxSize()
    ) {
        item { BackHeader(s.name, "${s.episodes.size} episodes", onBack) }
        item {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(28.dp))) {
                Thumb(target, Modifier.fillMaxSize(), large = true)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))
                    )
                )
                Row(
                    Modifier.align(Alignment.BottomStart).padding(18.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (poster != null) {
                        Box(Modifier.width(78.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp))) {
                            CoverImage(poster, target, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            "${target.code} · ${target.title}", color = Color.White, fontSize = 16.sp,
                            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                                .clickable { onPlay(target, false) }
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (resuming) "Continue" else "Play",
                                color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        seasons.forEach { (season, eps) ->
            item(key = "season_$season") {
                SectionTitle(if (season > 0) "Season $season" else "Episodes", Modifier.padding(top = 18.dp))
            }
            items(eps, key = { it.id }) { v ->
                VideoRow(v, { onPlay(v, false) }, label = "E${v.episode} · ${v.title}")
            }
        }
    }
}

// ---------------------------------------------------------------- folders

@Composable
fun FoldersScreen(folders: List<Folder>, bottomPad: Dp, onOpen: (Folder) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { ScreenHeader("Folders", "${folders.size} folders") }
        items(folders, key = { it.name }) { f ->
            val total = f.videos.sumOf { it.durationMs }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onOpen(f) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${f.videos.size} videos · ${fmtTime(total)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FolderDetail(f: Folder, bottomPad: Dp, onBack: () -> Unit, onPlay: (Video) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPad),
        modifier = Modifier.fillMaxSize()
    ) {
        item { BackHeader(f.name, "${f.videos.size} videos", onBack) }
        items(f.videos, key = { it.id }) { v -> VideoRow(v, { onPlay(v) }) }
    }
}

// ---------------------------------------------------------------- search

@Composable
fun SearchScreen(videos: List<Video>, bottomPad: Dp, onBack: () -> Unit, onPlay: (Video) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = remember(query, videos) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else videos.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.fileName.contains(q, ignoreCase = true) ||
                (it.series?.contains(q, ignoreCase = true) ?: false)
        }.take(100)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search your videos") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = bottomPad), modifier = Modifier.fillMaxSize()) {
            if (query.isNotBlank() && results.isEmpty()) {
                item {
                    Text(
                        "Nothing found", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            items(results, key = { it.id }) { v -> VideoRow(v, { onPlay(v) }) }
        }
    }
}
