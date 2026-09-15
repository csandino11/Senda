package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.model.Reading

private val youVersionIds = mapOf(
    "RVC" to 146,
    "NTV" to 127,
    "TLAI" to 178,
)

/** Builds a YouVersion chapter URL and enforces TLAI for deuterocanonical readings. */
fun youVersionUrl(reading: Reading, requestedVersion: String): String {
    val version = if (reading.isDeuterocanonical) "TLAI" else requestedVersion
    val versionId = requireNotNull(youVersionIds[version]) { "Versión bíblica no admitida: $version" }
    return "https://www.bible.com/es/bible/$versionId/${reading.book}.${reading.chapter}.$version"
}
