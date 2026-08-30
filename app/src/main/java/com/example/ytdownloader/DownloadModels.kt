package com.example.ytdownloader

/** Video ki basic info (yt-dlp se fetch hoti hai) */
data class VideoInfo(
    val title: String,
    val author: String,
    val duration: Long,
    val thumbnailUrl: String?,
    val videoFormats: List<FormatOption>,
    val audioFormats: List<FormatOption>
)

/** Ek downloadable format option — image 3 ki tarah har detail (container, codec, size, id) */
data class FormatOption(
    val formatId: String,
    val container: String,        // MP4 / WEBM
    val resolutionLabel: String,  // e.g. "2160p60" ya "3840x2160"
    val codec: String,            // AV01.0.13 / VP9 etc
    val fileSizeLabel: String,    // "1.7 GB"
    val isAudioOnly: Boolean
)

/** Ek recent search entry — home screen par history dikhane ke liye */
data class RecentSearch(
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val timestamp: Long
)

/** Ek completed download entry — Downloads tab me dikhane ke liye */
data class DownloadedItem(
    val fileName: String,
    val filePath: String,
    val isAudio: Boolean,
    val sizeLabel: String,
    val timestamp: Long
)

/** Download progress ki state jo UI observe karti hai */
sealed class DownloadState {
    object Idle : DownloadState()
    object FetchingInfo : DownloadState()
    data class InfoFetched(val info: VideoInfo) : DownloadState()
    data class Downloading(val progress: Float, val etaSeconds: Long, val speed: String) : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
