package com.example.ytdownloader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Recent searches aur completed downloads ki list ko device par persist karta hai
 * (SharedPreferences + JSON) — image 1 (recent search list) aur image 4
 * (downloads tab) ke liye data source.
 */
class AppStorage(context: Context) {

    private val prefs = context.getSharedPreferences("ytdown_prefs", Context.MODE_PRIVATE)

    fun getRecentSearches(): List<RecentSearch> {
        val raw = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RecentSearch(
                url = o.getString("url"),
                title = o.getString("title"),
                thumbnailUrl = o.optString("thumb", null.toString()).ifBlank { null },
                timestamp = o.getLong("ts")
            )
        }.sortedByDescending { it.timestamp }
    }

    fun addRecentSearch(item: RecentSearch) {
        val current = getRecentSearches().filterNot { it.url == item.url }.toMutableList()
        current.add(0, item)
        val arr = JSONArray()
        current.take(20).forEach { r ->
            arr.put(JSONObject().apply {
                put("url", r.url)
                put("title", r.title)
                put("thumb", r.thumbnailUrl ?: "")
                put("ts", r.timestamp)
            })
        }
        prefs.edit().putString(KEY_RECENT, arr.toString()).apply()
    }

    fun getDownloads(): List<DownloadedItem> {
        val raw = prefs.getString(KEY_DOWNLOADS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DownloadedItem(
                fileName = o.getString("name"),
                filePath = o.getString("path"),
                isAudio = o.getBoolean("audio"),
                sizeLabel = o.optString("size", ""),
                timestamp = o.getLong("ts")
            )
        }.sortedByDescending { it.timestamp }
    }

    fun addDownload(item: DownloadedItem) {
        val current = getDownloads().toMutableList()
        current.add(0, item)
        val arr = JSONArray()
        current.forEach { d ->
            arr.put(JSONObject().apply {
                put("name", d.fileName)
                put("path", d.filePath)
                put("audio", d.isAudio)
                put("size", d.sizeLabel)
                put("ts", d.timestamp)
            })
        }
        prefs.edit().putString(KEY_DOWNLOADS, arr.toString()).apply()
    }

    companion object {
        private const val KEY_RECENT = "recent_searches"
        private const val KEY_DOWNLOADS = "downloads"
    }
}
