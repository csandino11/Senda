package com.senda.lecturabiblica.data

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class AppUpdate(val version: String, val downloadUrl: String)

class UpdateRepository(private val context: Context) {
    private val latestReleaseApi = "https://api.github.com/repos/csandino11/Senda/releases/latest"

    fun findUpdate(): AppUpdate? {
        val connection = URL(latestReleaseApi).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 7_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Senda-Android")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "GitHub no respondió correctamente." }
            val body = connection.inputStream.bufferedReader().use { reader ->
                val text = reader.readText()
                require(text.length <= 300_000) { "La respuesta de actualización es demasiado grande." }
                text
            }
            val json = JSONObject(body)
            val latestVersion = json.getString("tag_name").removePrefix("v")
            if (!isNewerVersion(currentVersion(), latestVersion)) return null
            val assets = json.getJSONArray("assets")
            val downloadUrl = (0 until assets.length())
                .map(assets::getJSONObject)
                .firstOrNull { it.getString("name").endsWith(".apk", ignoreCase = true) }
                ?.getString("browser_download_url")
                ?: return null
            require(isTrustedDownload(downloadUrl)) { "GitHub devolvió un enlace de descarga no válido." }
            AppUpdate(latestVersion, downloadUrl)
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun currentVersion(): String = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        .orEmpty()

    fun enqueueDownload(update: AppUpdate): Long {
        require(isTrustedDownload(update.downloadUrl)) { "El enlace de descarga no es válido." }
        val request = DownloadManager.Request(update.downloadUrl.toUri())
            .setTitle("Senda ${update.version}")
            .setDescription("Descargando la actualización de Senda")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Senda-${update.version}.apk")
        val manager = context.getSystemService(DownloadManager::class.java)
        return manager.enqueue(request)
    }

    private fun isTrustedDownload(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.startsWith("/csandino11/Senda/releases/download/") && uri.path.endsWith(".apk")
    }
}

internal fun isNewerVersion(current: String, candidate: String): Boolean {
    fun parts(value: String) = value.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val currentParts = parts(current)
    val candidateParts = parts(candidate)
    val length = maxOf(currentParts.size, candidateParts.size)
    return (0 until length).firstNotNullOfOrNull { index ->
        val left = candidateParts.getOrElse(index) { 0 }
        val right = currentParts.getOrElse(index) { 0 }
        when {
            left > right -> true
            left < right -> false
            else -> null
        }
    } ?: false
}
