package com.flagsnerd.prism.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Library {
    suspend fun load(ctx: Context): List<Video> = withContext(Dispatchers.IO) {
        val out = ArrayList<Video>()
        val base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
        )
        try {
            ctx.contentResolver.query(base, proj, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val iBucket = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    out.add(
                        Video(
                            id = id,
                            uri = ContentUris.withAppendedId(base, id),
                            fileName = c.getString(iName) ?: "Video",
                            folder = c.getString(iBucket) ?: "Internal storage",
                            durationMs = c.getLong(iDur),
                            dateAdded = c.getLong(iDate),
                            size = c.getLong(iSize),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // permission revoked mid-query etc. - return what we have
        }
        out
    }

    fun series(all: List<Video>): List<Series> =
        all.filter { it.isEpisode }
            .groupBy { it.series!!.lowercase() }
            .values
            .map { eps ->
                Series(
                    eps.first().series!!,
                    eps.sortedWith(compareBy<Video> { it.season }.thenBy { it.episode })
                )
            }
            .sortedBy { it.name.lowercase() }

    fun folders(all: List<Video>): List<Folder> =
        all.groupBy { it.folder }
            .map { (name, vids) -> Folder(name, vids) }
            .sortedBy { it.name.lowercase() }

    /** What plays after this one: the rest of the show, or just the video. */
    fun queueFor(v: Video, all: List<Video>): List<Video> {
        val s = v.series ?: return listOf(v)
        val eps = all.filter { it.series.equals(s, ignoreCase = true) }
            .sortedWith(compareBy<Video> { it.season }.thenBy { it.episode })
        return if (eps.isEmpty()) listOf(v) else eps
    }
}
