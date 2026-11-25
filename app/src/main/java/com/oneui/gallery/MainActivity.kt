package com.oneui.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.oneui.gallery.ui.GalleryApp

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var coilInitialized = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!coilInitialized) {
            coilInitialized = true
            val appCtx = applicationContext
            val imageLoader = ImageLoader.Builder(appCtx)
                .allowRgb565(true)
                .crossfade(false)
                .respectCacheHeaders(false)
                .memoryCache {
                    MemoryCache.Builder(appCtx)
                        .maxSizePercent(0.28)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(appCtx.cacheDir.resolve("gallery_thumb_cache"))
                        .maxSizeBytes(120L * 1024 * 1024)
                        .build()
                }
                .build()
            Coil.setImageLoader(imageLoader)
        }
        enableEdgeToEdge()
        setContent {
            GalleryApp()
        }
    }
}
