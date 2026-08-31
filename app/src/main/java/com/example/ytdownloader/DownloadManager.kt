package com.example.ytdownloader

import android.content.Context
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo as YTVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * yt-dlp library ke saare calls yahan wrap kiye gaye hain.
 * IMPORTANT: files hamesha public Movies/Music folder me save hoti hain
 * (app ke andar nahi) — isliye phone gallery/file-manager me bhi dikhengi
 * aur app uninstall hone par bhi safe rahengi.
 */
class DownloadManager(private val appContext: Context) {

    private val videoDir: File by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "YTdown"
        ).apply { if (!exists()) mkdirs() }
    }

    private val audioDir: File by lazy {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "YTdown"
        ).apply { if (!exists()) mkdirs() }
    }

    /** URL se video ka title, author, thumbnail aur saari available qualities fetch karta hai */
    suspend fun fetchVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            YoutubeDLInitializer.ensureInitialized(appContext)
            val request = YoutubeDLRequest(url)
            val info: YTVideoInfo = YoutubeDL.getInstance().getInfo(request)

            val allFormats = info.formats ?: emptyList()

            val videoFormats = allFormats
                .filter { it.vcodec != null && it.vcodec != "none" }
                .map { f ->
                    FormatOption(
                        formatId = f.formatId ?: "",
                        container = (f.ext ?: "mp4").uppercase(),
                        resolutionLabel = f.formatNote ?: f.formatNote ?: "unknown",
                        codec = f.vcodec ?: "",
                        fileSizeLabel = formatBytes(f.fileSize),
                        isAudioOnly = false
                    )
                }
                .sortedByDescending { it.resolutionLabel }

            val audioFormats = allFormats
                .filter { it.vcodec == null || it.vcodec == "none" }
                .map { f ->
                    FormatOption(
                        formatId = f.formatId ?: "",
                        container = (f.ext ?: "m4a").uppercase(),
                        resolutionLabel = "${f.abr?.toInt() ?: 0}kbps",
                        codec = f.acodec ?: "",
                        fileSizeLabel = formatBytes(f.fileSize),
                        isAudioOnly = true
                    )
                }

            Result.success(
                VideoInfo(
                    title = info.title ?: "video",
                    author = info.uploader ?: "" ?: "",
                    duration = info.duration.toLong(),
                    thumbnailUrl = info.thumbnail,
                    videoFormats = videoFormats,
                    audioFormats = audioFormats
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Diye gaye format ID ke sath video/audio download karta hai.
     * Result me final saved file ka path milta hai (public storage me).
     */
    suspend fun download(
        url: String,
        title: String,
        formatId: String?,
        audioOnly: Boolean,
        onProgress: (progress: Float, etaSeconds: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            YoutubeDLInitializer.ensureInitialized(appContext)
            val targetDir = if (audioOnly) audioDir else videoDir

            val request = YoutubeDLRequest(url).apply {
                addOption("-o", "${targetDir.absolutePath}/%(title)s.%(ext)s")

                if (audioOnly) {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    if (formatId != null) addOption("-f", formatId)
                } else if (formatId != null) {
                    addOption("-f", "$formatId+bestaudio/best")
                } else {
                    addOption("-f", "bestvideo+bestaudio/best")
                }

                addOption("--embed-thumbnail")
                addOption("--add-metadata")
            }

            YoutubeDL.getInstance().execute(request, null) { progress, etaSeconds, _ ->
                onProgress(progress, etaSeconds)
            }

            Result.success(targetDir.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes <= 0) return ""
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1) return "%.1f GB".format(gb)
        val mb = bytes / (1024.0 * 1024.0)
        return "%.1f MB".format(mb)
    }
}
