package com.example.ytdownloader

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App start hote hi background me yt-dlp/ffmpeg/aria2c initialization shuru kar deta hai
 * (warm start — taaki jab tak user URL type kare, init already poora ho chuka ho).
 * Lekin agar user bahut jaldi search kar de, DownloadManager khud
 * YoutubeDLInitializer.ensureInitialized() ke through wait kar lega — isliye
 * "instance not initialized" error ab kabhi nahi aayega.
 */
class YTDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            YoutubeDLInitializer.ensureInitialized(this@YTDownloaderApp)
        }
    }
}
