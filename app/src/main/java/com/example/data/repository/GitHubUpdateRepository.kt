package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val version: String,
    val releaseUrl: String,
    val downloadUrl: String?,
)

class GitHubUpdateRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdate(installedVersion: String): AppUpdate? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastCheck = preferences.getLong(KEY_LAST_CHECK, 0L)
        if (now - lastCheck < CHECK_INTERVAL_MS) return@withContext null

        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Bhai-Paise-Android")
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            preferences.edit().putLong(KEY_LAST_CHECK, now).apply()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(body)
            val latestVersion = release.optString("tag_name")
                .trim()
                .removePrefix("v")
                .removePrefix("V")
            if (latestVersion.isBlank() || compareVersions(latestVersion, installedVersion) <= 0) {
                return@withContext null
            }

            val assets = release.optJSONArray("assets")
            var downloadUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    if (asset.optString("name").equals("Bhai-Paise.apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }
            AppUpdate(
                version = latestVersion,
                releaseUrl = release.optString("html_url"),
                downloadUrl = downloadUrl,
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val rightParts = right.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val comparison = (leftParts.getOrNull(index) ?: 0).compareTo(rightParts.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val PREFS_NAME = "release_update_check"
        const val KEY_LAST_CHECK = "last_check_at"
        const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/Shankha2003/bhai-paise/releases/latest"
    }
}
