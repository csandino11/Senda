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
    private val latestReleasePage = "https://github.com/csandino11/Senda/releases/latest"

    fun findUpdate(): AppUpdate? = runCatching(::findFromApi)
        .getOrElse { findFromLatestReleaseRedirect() }

    private fun findFromApi(): AppUpdate? {
        val connection = URL(latestReleaseApi).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Senda-Android")
            connection.setRequestProperty("Cache-Control", "no-cache")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "GitHub no respondió correctamente." }
            val body = connection.inputStream.bufferedReader().use { reader ->
                val text = reader.readText()
                require(text.length <= 300_000) { "La respuesta de actualización es demasiado grande." }
                text
            }
            updateFromReleaseJson(currentVersion(), body)
        } finally {
            connection.disconnect()
        }
    }

    private fun findFromLatestReleaseRedirect(): AppUpdate? {
        val connection = URL(latestReleasePage).openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Senda-Android")
            connection.setRequestProperty("Cache-Control", "no-cache")
            val response = connection.responseCode
            check(response in 300..399) { "GitHub no devolvió la versión más reciente." }
            val location = connection.getHeaderField("Location")
                ?: error("GitHub no indicó la versión más reciente.")
            updateFromReleaseLocation(currentVersion(), location)
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

}

internal fun updateFromReleaseJson(currentVersion: String, body: String): AppUpdate? {
    val json = JSONObject(body)
    val latestVersion = json.getString("tag_name").removePrefix("v")
    if (!isNewerVersion(currentVersion, latestVersion)) return null
    val assets = json.getJSONArray("assets")
    val downloadUrl = (0 until assets.length())
        .map(assets::getJSONObject)
        .firstOrNull { it.getString("name").endsWith(".apk", ignoreCase = true) }
        ?.getString("browser_download_url")
        ?: error("La versión más reciente todavía no tiene un APK disponible.")
    require(isTrustedDownload(downloadUrl)) { "GitHub devolvió un enlace de descarga no válido." }
    return AppUpdate(latestVersion, downloadUrl)
}

internal fun updateFromReleaseLocation(currentVersion: String, location: String): AppUpdate? {
    val releaseUri = URI("https://github.com/csandino11/Senda/releases/latest").resolve(location)
    require(releaseUri.scheme == "https" && releaseUri.host == "github.com") {
        "GitHub devolvió una ubicación no válida."
    }
    val prefix = "/csandino11/Senda/releases/tag/"
    require(releaseUri.path.startsWith(prefix)) { "GitHub devolvió una etiqueta no válida." }
    val latestVersion = releaseUri.path.removePrefix(prefix).removePrefix("v")
    require(latestVersion.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][A-Za-z0-9.-]+)?"))) {
        "GitHub devolvió una versión no válida."
    }
    if (!isNewerVersion(currentVersion, latestVersion)) return null
    val downloadUrl = "https://github.com/csandino11/Senda/releases/download/" +
        "v$latestVersion/Senda-$latestVersion.apk"
    require(isTrustedDownload(downloadUrl)) { "El enlace alternativo de descarga no es válido." }
    return AppUpdate(latestVersion, downloadUrl)
}

private fun isTrustedDownload(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme == "https" && uri.host == "github.com" &&
        uri.path.startsWith("/csandino11/Senda/releases/download/") && uri.path.endsWith(".apk")
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
