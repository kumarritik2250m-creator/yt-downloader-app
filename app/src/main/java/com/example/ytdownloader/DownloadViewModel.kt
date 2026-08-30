package com.example.ytdownloader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(app: Application) : AndroidViewModel(app) {

    private val downloadManager = DownloadManager()
    private val storage = AppStorage(app)

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _recentSearches = MutableStateFlow(storage.getRecentSearches())
    val recentSearches: StateFlow<List<RecentSearch>> = _recentSearches.asStateFlow()

    private val _downloads = MutableStateFlow(storage.getDownloads())
    val downloads: StateFlow<List<DownloadedItem>> = _downloads.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
    }

    /** Video info fetch karo taaki user format select kar sake (image 2) */
    fun fetchInfo(overrideUrl: String? = null) {
        val currentUrl = (overrideUrl ?: _url.value).trim()
        if (currentUrl.isBlank()) {
            _state.value = DownloadState.Error("Pehle YouTube URL daalein")
            return
        }
        _url.value = currentUrl

        viewModelScope.launch {
            _state.value = DownloadState.FetchingInfo
            val result = downloadManager.fetchVideoInfo(currentUrl)
            result.onSuccess { info ->
                _state.value = DownloadState.InfoFetched(info)
                storage.addRecentSearch(
                    RecentSearch(
                        url = currentUrl,
                        title = info.title,
                        thumbnailUrl = info.thumbnailUrl,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _recentSearches.value = storage.getRecentSearches()
            }.onFailure { e ->
                _state.value = DownloadState.Error(e.message ?: "Video info fetch nahi ho payi")
            }
        }
    }

    /** Select kiya hua format download karo — file device ke public storage me save hogi */
    fun startDownload(title: String, formatId: String?, audioOnly: Boolean) {
        val currentUrl = _url.value.trim()

        viewModelScope.launch {
            _state.value = DownloadState.Downloading(0f, 0, "")
            val result = downloadManager.download(
                url = currentUrl,
                title = title,
                formatId = formatId,
                audioOnly = audioOnly
            ) { progress, eta ->
                _state.value = DownloadState.Downloading(progress, eta, "")
            }

            result.onSuccess { path ->
                _state.value = DownloadState.Completed(path)
                storage.addDownload(
                    DownloadedItem(
                        fileName = title,
                        filePath = path,
                        isAudio = audioOnly,
                        sizeLabel = "",
                        timestamp = System.currentTimeMillis()
                    )
                )
                _downloads.value = storage.getDownloads()
            }.onFailure { e ->
                _state.value = DownloadState.Error(e.message ?: "Download fail ho gaya")
            }
        }
    }

    fun reset() {
        _state.value = DownloadState.Idle
        _url.value = ""
    }
}
