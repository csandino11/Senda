package com.senda.lecturabiblica.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.net.toUri
import com.senda.lecturabiblica.model.Reading

data class UniversalBibleApp(val packageName: String, val name: String, val deepLink: String? = null)

val universalBibleApps = listOf(
    UniversalBibleApp("com.csnmedia.android.bg", "Bible Gateway", "gateway"),
    UniversalBibleApp("com.logos.androidlogos", "Logos Bible", "logos"),
    UniversalBibleApp("biblereader.olivetree", "Olive Tree Bible"),
    UniversalBibleApp("org.blueletterbible.blb", "Blue Letter Bible"),
    UniversalBibleApp("com.faithcomesbyhearing.android.bibleis", "Bible.is"),
    UniversalBibleApp("net.bible.android.activity", "AndBible"),
    UniversalBibleApp("lmontt.cl", "Santa Biblia Reina Valera"),
    UniversalBibleApp("com.appgp.bibliacatolicaenespanol", "Biblia Católica"),
    UniversalBibleApp("la.santa.biblia.catolica.espanol", "La Santa Biblia Católica"),
    UniversalBibleApp("net.esword.esword", "e-Sword"),
)

@Suppress("DEPRECATION")
fun installedUniversalBibleApps(context: Context): List<UniversalBibleApp> = universalBibleApps.filter { app ->
    runCatching { context.packageManager.getPackageInfo(app.packageName, 0) }.isSuccess
}

fun openUniversalBible(context: Context, reading: Reading, version: String, preferredPackage: String) {
    val installed = installedUniversalBibleApps(context)
    val candidates = if (preferredPackage == "AUTO") installed else
        installed.sortedBy { if (it.packageName == preferredPackage) 0 else 1 }
    for (app in candidates) {
        val url = when (app.deepLink) {
            "gateway" -> bibleGatewayUrl(reading, version)
            "logos" -> "https://ref.ly/${reading.book}${reading.chapter}.1"
            else -> null
        }
        if (url != null) {
            val opened = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).setPackage(app.packageName))
            }.isSuccess
            if (opened) return
        }
        val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launch != null && runCatching { context.startActivity(launch) }.isSuccess) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Lectura de Senda", reading.label))
            Toast.makeText(context, "${reading.label} copiado. Búscalo en ${app.name}.", Toast.LENGTH_LONG).show()
            return
        }
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, bibleGatewayUrl(reading, version).toUri()))
}
