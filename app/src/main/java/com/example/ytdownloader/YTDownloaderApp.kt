package com.example.ytdownloader

import android.app.Application
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * App start hote hi yt-dlp binary, ffmpeg aur aria2c initialize karte hain.
 * Ye ek time ka setup hai — isके bina download engine kaam nahi karega.
 */
class YTDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@YTDownloaderApp)
                FFmpeg.getInstance().init(this@YTDownloaderApp)
                Aria2c.getInstance().init(this@YTDownloaderApp)
                Log.d("YTDownloaderApp", "yt-dlp/ffmpeg/aria2c initialized successfully")
            } catch (e: YoutubeDLException) {
                Log.e("YTDownloaderApp", "Init failed: ${e.message}")
            }
        }
    }
}
