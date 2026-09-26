package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.BuildConfig
import com.senda.lecturabiblica.model.Reading
import java.net.URLEncoder

data class BibleTranslation(val id: String, val name: String, val youVersionId: Int)

private val youVersionTranslations = listOf(
    BibleTranslation("RVC", "Reina Valera Contemporánea", 146),
    BibleTranslation("NTV", "Nueva Traducción Viviente", 127),
    BibleTranslation("TLAI", "Traducción al Lenguaje Actual Interconfesional", 178),
    BibleTranslation("PDT", "Palabra de Dios para Todos", 197),
    BibleTranslation("NBV", "Nueva Biblia Viva", 753),
    BibleTranslation("CUSTOM", "Personalizado · mi versión en YouVersion", 0),
)

private val universalTranslations = listOf(
    BibleTranslation("RVC", "Reina Valera Contemporánea", 0),
    BibleTranslation("RVA", "Reina Valera Actualizada", 0),
    BibleTranslation("NVI", "Nueva Versión Internacional", 0),
    BibleTranslation("DHH", "Dios Habla Hoy", 0),
    BibleTranslation("RVR1960", "Reina-Valera 1960 · alternativa", 0),
    BibleTranslation("CUSTOM", "Personalizado · versión de mi app bíblica", 0),
)

val bibleTranslations: List<BibleTranslation> =
    if (BuildConfig.UNIVERSAL_BIBLE) universalTranslations else youVersionTranslations

/** Builds a YouVersion chapter URL and enforces TLAI for deuterocanonical readings. */
fun youVersionUrl(reading: Reading, requestedVersion: String): String {
    val version = if (reading.isDeuterocanonical) "TLAI" else requestedVersion
    if (version == "CUSTOM") return "youversion://bible?reference=${reading.book}.${reading.chapter}.1"
    val translation = requireNotNull(youVersionTranslations.firstOrNull { it.id == version }) {
        "Versión bíblica no admitida: $version"
    }
    return "https://www.bible.com/es/bible/${translation.youVersionId}/${reading.book}.${reading.chapter}.$version"
}

fun bibleGatewayUrl(reading: Reading, requestedVersion: String): String {
    val version = if (reading.isDeuterocanonical) "DHH" else when (requestedVersion) {
        "RVC" -> "RVC"
        "RVA" -> "RVA-2015"
        "NVI" -> "NVI"
        "DHH" -> "DHH"
        else -> "RVR1960"
    }
    val reference = when (reading.book) {
        "TOB" -> "Tobit ${reading.chapter}"
        "LJE" -> "Carta de Jeremías ${reading.chapter}"
        "S3Y" -> "Oración de Azarías ${reading.chapter}"
        "SUS" -> "Susana ${reading.chapter}"
        "BEL" -> "Bel y el dragón ${reading.chapter}"
        else -> reading.label
    }
    val query = URLEncoder.encode(reference, "UTF-8")
    return "https://www.biblegateway.com/passage/?search=$query&version=$version"
}
