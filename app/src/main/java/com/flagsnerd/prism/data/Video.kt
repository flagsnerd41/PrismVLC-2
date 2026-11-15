package com.flagsnerd.prism.data

import android.net.Uri
import java.util.Locale

data class Video(
    val id: Long,
    val uri: Uri,
    val fileName: String,
    val folder: String,
    val durationMs: Long,
    val dateAdded: Long,
    val size: Long,
) {
    private val parsed: Parsed = Parser.parse(fileName)

    /** Clean, human title (no release-group junk). */
    val title: String get() = parsed.title
    val series: String? get() = parsed.series
    val season: Int get() = parsed.season
    val episode: Int get() = parsed.episode
    val isEpisode: Boolean get() = parsed.series != null

    val code: String
        get() = String.format(Locale.ROOT, "S%02dE%02d", season, episode)

    /** Second line under a title: "Show · S01E02" for episodes, folder name otherwise. */
    val subtitle: String
        get() = if (isEpisode) "${parsed.series} · $code" else folder
}

data class Parsed(val title: String, val series: String?, val season: Int, val episode: Int)

object Parser {
    private val epRegex = Regex("""(?i)^(.*?)[\s._\-–]*\bS(\d{1,2})[\s._-]?E(\d{1,3})\b[\s._\-–]*(.*)$""")
    private val altRegex = Regex("""(?i)^(.*?)[\s._\-–]*\b(\d{1,2})x(\d{2,3})\b[\s._\-–]*(.*)$""")

    private const val TOKENS =
        "2160p|1080p|720p|480p|4k|uhd|bluray|blu-ray|brrip|bdrip|web-?dl|webrip|hdtv|dvdrip|x264|x265|" +
            "h\\.?264|h\\.?265|hevc|avc|aac|ac3|eac3|ddp5\\.1|dts|10bit|hdr|remux|proper|repack|amzn|" +
            "nf|ime|yify|yts|rarbg|ettv|extended|unrated"
    private val junkWord = Regex("(?i)\\b($TOKENS)\\b")
    private val groups = Regex("""\([^)]*\)|\[[^\]]*\]""")
    private val spaces = Regex("\\s+")
    private val trailingYear = Regex("""\s*\(\d{4}\)\s*$""")

    fun parse(file: String): Parsed {
        val dot = file.lastIndexOf('.')
        val base = if (dot > 0 && file.length - dot <= 5) file.substring(0, dot) else file
        val m = epRegex.find(base) ?: altRegex.find(base)
        if (m != null) {
            val series = clean(m.groupValues[1]).replace(trailingYear, "").trim()
            val s = m.groupValues[2].toIntOrNull() ?: 0
            val e = m.groupValues[3].toIntOrNull() ?: 0
            var rest = clean(m.groupValues[4])
            if (rest.isBlank() || rest.equals(series, ignoreCase = true)) rest = "Episode $e"
            if (series.isNotBlank()) return Parsed(rest, series, s, e)
        }
        return Parsed(clean(base).ifBlank { file }, null, 0, 0)
    }

    fun clean(s: String): String {
        var t = groups.replace(s) { mr -> if (junkWord.containsMatchIn(mr.value)) "" else mr.value }
        t = junkWord.replace(t, " ")
        if (!t.contains(' ')) t = t.replace('.', ' ')
        t = t.replace('_', ' ')
        t = spaces.replace(t, " ").trim()
        return t.trim('-', '–', ' ')
    }
}

data class Series(val name: String, val episodes: List<Video>) {
    val cover: Video get() = episodes.first()
}

data class Folder(val name: String, val videos: List<Video>)
