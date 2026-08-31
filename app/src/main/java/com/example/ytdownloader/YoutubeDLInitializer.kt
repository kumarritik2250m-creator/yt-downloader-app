package com.example.ytdownloader

import android.content.Context
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * "instance not initialized" error tab aata tha jab user URL search kar deta tha
 * isse pehle ki background init poora ho. Ye object ensure karta hai ki koi bhi
 * download/fetch call hamesha init complete hone ke baad hi yt-dlp use kare —
 * agar init already ho chuka hai to turant return, warna wait karega.
 */
object YoutubeDLInitializer {

    @Volatile private var initialized = false
    private val mutex = Mutex()

    suspend fun ensureInitialized(context: Context) {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                Aria2c.getInstance().init(context.applicationContext)
            }
            initialized = true
            Log.d("YoutubeDLInitializer", "yt-dlp/ffmpeg/aria2c initialized successfully")
        }
    }
}
