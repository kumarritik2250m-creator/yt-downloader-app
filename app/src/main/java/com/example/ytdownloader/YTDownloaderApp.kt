package com.example.ytdownloader

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YTDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDLInitializer.ensureInitialized(this@YTDownloaderApp)
            } catch (e: Exception) {
                Log.e("YTDownloaderApp", "yt-dlp init failed", e)
            }
        }
    }
}
