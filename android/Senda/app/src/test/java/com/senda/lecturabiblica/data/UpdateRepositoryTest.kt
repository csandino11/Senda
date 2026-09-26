package com.senda.lecturabiblica.data

import com.senda.lecturabiblica.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    private val release = JSONObject().apply {
        put("tag_name", "v2.0.0")
        put("body", "# Cambios\n- Uno\n- Dos\n- Tres\n- Cuatro\n- Cinco\n- Seis")
        put("assets", JSONArray().apply {
            listOf("YouVersion", "Universal").forEach { edition ->
                put(JSONObject().apply {
                    put("name", "Senda-2.0.0-$edition.apk")
                    put("browser_download_url", "https://github.com/csandino11/Senda/releases/download/v2.0.0/Senda-2.0.0-$edition.apk")
                })
            }
        })
    }.toString()

    @Test fun choosesItsOwnEditionAndLimitsChangelog() {
        val update = requireNotNull(updateFromReleaseJson("1.7.1", release))
        val name = if (BuildConfig.UNIVERSAL_BIBLE) "Universal" else "YouVersion"
        assertTrue(update.downloadUrl.endsWith("$name.apk"))
        assertEquals(listOf("Uno", "Dos", "Tres", "Cuatro", "Cinco"), update.changelog)
        assertNull(updateFromReleaseJson("2.0.0", release))
    }
}
