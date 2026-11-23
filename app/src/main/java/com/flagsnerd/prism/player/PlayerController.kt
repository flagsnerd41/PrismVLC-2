package com.flagsnerd.prism.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.data.Video
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/** One libVLC player that outlives the screens, so playback survives while you browse (mini player). */
object PlayerController {
    class Track(val id: Int, val name: String)

    private lateinit var app: Context
    private var libVLC: LibVLC? = null
    private var mp: MediaPlayer? = null
    private var pfd: ParcelFileDescriptor? = null
    private val main = Handler(Looper.getMainLooper())
    private var lastSave = 0L
    private var queue: List<Video> = emptyList()

    // Playback must start AFTER the video surface is attached, otherwise libVLC opens the
    // stream with no video output and you get a black screen (or nothing at all).
    private var pendingPlay = false
    private var attempt = 0
    private var startAt = 0L

    var current by mutableStateOf<Video?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var timeMs by mutableLongStateOf(0L)
        private set
    var lengthMs by mutableLongStateOf(0L)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var fill by mutableStateOf(false)
        private set

    /** Full-screen player visible? (false = mini player / library) */
    var showPlayer by mutableStateOf(false)

    fun init(ctx: Context) {
        app = ctx.applicationContext
    }

    private fun player(): MediaPlayer {
        mp?.let { return it }
        // Default options: no "--no-drop-late-frames" / "--no-skip-frames", they make video
        // stall on hardware decoders that cannot keep up.
        val lib = LibVLC(app)
        libVLC = lib
        val p = MediaPlayer(lib)
        p.setEventListener(MediaPlayer.EventListener { e -> onEvent(e) })
        mp = p
        return p
    }

    private fun onEvent(e: MediaPlayer.Event) {
        when (e.type) {
            MediaPlayer.Event.Playing -> {
                isPlaying = true
                pendingPlay = false
            }
            MediaPlayer.Event.Paused -> {
                isPlaying = false
                saveProgress(true)
            }
            MediaPlayer.Event.Stopped -> isPlaying = false
            MediaPlayer.Event.EncounteredError -> {
                isPlaying = false
                main.post { onError() }
            }
            MediaPlayer.Event.EndReached -> main.post { ended() }
            MediaPlayer.Event.LengthChanged -> lengthMs = e.lengthChanged
            MediaPlayer.Event.TimeChanged -> {
                timeMs = e.timeChanged
                val now = SystemClock.elapsedRealtime()
                if (now - lastSave > 5000L) {
                    lastSave = now
                    saveProgress(false)
                }
            }
        }
    }

    /** Retry with software decoding / a different way of opening the file before giving up. */
    private fun onError() {
        val v = current ?: return
        if (attempt < 2) {
            attempt++
            startMedia(v)
        } else {
            Toast.makeText(app, "Couldn't play this video", Toast.LENGTH_LONG).show()
        }
    }

    private fun closeFd() {
        try { pfd?.close() } catch (e: Exception) { }
        pfd = null
    }

    private fun viewsAttached(): Boolean =
        try { mp?.vlcVout?.areViewsAttached() == true } catch (e: Exception) { false }

    fun play(
        v: Video,
        q: List<Video> = listOf(v),
        fromStart: Boolean = false,
        openPlayer: Boolean = true,
    ) {
        saveProgress(true)
        player()
        queue = q
        current = v
        timeMs = 0L
        lengthMs = v.durationMs
        if (openPlayer) showPlayer = true
        attempt = 0
        pendingPlay = false
        val saved = Store.position(v.id)
        startAt = if (fromStart || Store.watched(v)) 0L else saved
        startMedia(v)
    }

    /**
     * attempt 0: file descriptor, hardware decoding allowed
     * attempt 1: file descriptor, software decoding
     * attempt 2: content Uri, software decoding
     */
    private fun startMedia(v: Video) {
        val p = player()
        val lib = libVLC ?: return
        closeFd()
        val media: Media = if (attempt <= 1) {
            try {
                val fd = app.contentResolver.openFileDescriptor(v.uri, "r")
                if (fd != null) {
                    pfd = fd
                    Media(lib, fd.fileDescriptor)
                } else {
                    Media(lib, v.uri)
                }
            } catch (e: Exception) {
                Media(lib, v.uri)
            }
        } else {
            Media(lib, v.uri)
        }
        if (attempt >= 1) media.setHWDecoderEnabled(false, false)
        if (startAt > 10_000L) media.addOption(":start-time=${startAt / 1000}")
        p.media = media
        media.release()
        p.rate = speed
        // wait for the surface when the full player is about to show
        if (viewsAttached() || !showPlayer) p.play() else pendingPlay = true
    }

    fun attach(layout: VLCVideoLayout) {
        val p = player()
        try {
            if (p.vlcVout.areViewsAttached()) p.detachViews()
            p.attachViews(layout, null, true, true)
        } catch (e: Exception) { }
        applyFill()
        if (pendingPlay) {
            pendingPlay = false
            main.post { mp?.play() }
        }
    }

    fun detach() {
        try {
            mp?.let { if (it.vlcVout.areViewsAttached()) it.detachViews() }
        } catch (e: Exception) { }
    }

    fun toggle() {
        val p = mp ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(ms: Long) {
        val p = mp ?: return
        val max = if (lengthMs > 0) lengthMs else Long.MAX_VALUE
        val t = ms.coerceIn(0L, max)
        p.time = t
        timeMs = t
    }

    fun seekBy(delta: Long) {
        val p = mp ?: return
        seekTo(p.time + delta)
    }

    // @JvmName avoids a JVM clash with the private setter generated for `var speed`
    @JvmName("applySpeed")
    fun setSpeed(r: Float) {
        speed = r
        mp?.rate = r
    }

    fun toggleFill() {
        fill = !fill
        applyFill()
    }

    private fun applyFill() {
        mp?.videoScale =
            if (fill) MediaPlayer.ScaleType.SURFACE_FIT_SCREEN else MediaPlayer.ScaleType.SURFACE_BEST_FIT
    }

    // ---- tracks ----
    fun audioTracks(): List<Track> =
        mp?.audioTracks?.map { Track(it.id, it.name ?: "Track ${it.id}") } ?: emptyList()
    fun audioTrack(): Int = mp?.audioTrack ?: -1
    fun setAudioTrack(id: Int) { mp?.audioTrack = id }

    fun subtitleTracks(): List<Track> =
        mp?.spuTracks?.map { Track(it.id, it.name ?: "Track ${it.id}") } ?: emptyList()
    fun subtitleTrack(): Int = mp?.spuTrack ?: -1
    fun setSubtitleTrack(id: Int) { mp?.spuTrack = id }

    // ---- queue ----
    fun nextVideo(): Video? {
        val c = current ?: return null
        val i = queue.indexOfFirst { it.id == c.id }
        return if (i >= 0 && i + 1 < queue.size) queue[i + 1] else null
    }

    fun prevVideo(): Video? {
        val c = current ?: return null
        val i = queue.indexOfFirst { it.id == c.id }
        return if (i > 0) queue[i - 1] else null
    }

    fun playNext() { nextVideo()?.let { play(it, queue, true) } }
    fun playPrev() { prevVideo()?.let { play(it, queue, true) } }

    private fun ended() {
        val c = current ?: return
        Store.save(c.id, if (lengthMs > 0) lengthMs else c.durationMs, true)
        val n = nextVideo()
        if (n != null && Store.autoNext) play(n, queue, true, openPlayer = false) else isPlaying = false
    }

    fun saveProgress(notify: Boolean) {
        val c = current ?: return
        val t = mp?.time ?: timeMs
        if (t > 0L) Store.save(c.id, t, notify)
    }

    /** Stop for good (mini player X). */
    fun stop() {
        saveProgress(true)
        pendingPlay = false
        mp?.stop()
        closeFd()
        current = null
        isPlaying = false
        showPlayer = false
    }
}
