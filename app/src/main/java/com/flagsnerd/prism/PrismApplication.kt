package com.flagsnerd.prism

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import com.flagsnerd.prism.data.Store
import com.flagsnerd.prism.player.PlayerController
import com.flagsnerd.prism.update.Updater

class PrismApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        PlayerController.init(this)
        Updater.schedule(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .crossfade(true)
            .build()
}
