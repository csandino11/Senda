package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.model.Reading

data class BibleTranslation(val id: String, val name: String, val youVersionId: Int)

val bibleTranslations = listOf(
    BibleTranslation("RVC", "Reina Valera Contemporánea", 146),
    BibleTranslation("NTV", "Nueva Traducción Viviente", 127),
    BibleTranslation("TLAI", "Traducción al Lenguaje Actual Interconfesional", 178),
    BibleTranslation("PDT", "Palabra de Dios para Todos", 197),
    BibleTranslation("NBV", "Nueva Biblia Viva", 753),
)

/** Builds a YouVersion chapter URL and enforces TLAI for deuterocanonical readings. */
fun youVersionUrl(reading: Reading, requestedVersion: String): String {
    val version = if (reading.isDeuterocanonical) "TLAI" else requestedVersion
    val translation = requireNotNull(bibleTranslations.firstOrNull { it.id == version }) {
        "Versión bíblica no admitida: $version"
    }
    return "https://www.bible.com/es/bible/${translation.youVersionId}/${reading.book}.${reading.chapter}.$version"
}
