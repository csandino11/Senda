package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.PlanBackupCodec
import com.senda.lecturabiblica.data.PlanBackupData
import com.senda.lecturabiblica.data.PlanJson
import com.senda.lecturabiblica.data.isNewerVersion
import com.senda.lecturabiblica.data.updateFromReleaseJson
import com.senda.lecturabiblica.data.updateFromReleaseLocation
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPace
import com.senda.lecturabiblica.model.endDate
import com.senda.lecturabiblica.ui.accentPalettes
import com.senda.lecturabiblica.ui.themeBackgrounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.DayOfWeek

class PlanGeneratorTest {
    @Test
    fun everyThemePaceAndCanonOptionProducesACompleteValidPlan() {
        val start = java.time.LocalDate.of(2026, 9, 16)
        BibleData.themes.forEachIndexed { index, theme ->
            ReadingPace.selectable.forEachIndexed { paceIndex, pace ->
                listOf(false, true).forEach { extra ->
                    val plan = PlanGenerator.generate(
                        start,
                        theme.id,
                        extra,
                        pace,
                        index * 100L + paceIndex * 10L + if (extra) 1 else 0,
                    )
                    val result = PlanGenerator.validate(plan)
                    assertEquals(pace.durationDays(extra), result.days)
                    assertEquals(if (extra) 1497 else 1407, result.readings)
                    assertEquals(start.plusDays(result.days.toLong() - 1), plan.endDate)
                    assertTrue(plan.days.all { day ->
                        day.readings.size <= if (day.date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                            pace.weekendMaximum
                        } else {
                            pace.weekdayMaximum
                        }
                    })
                    assertTrue(plan.days.all { it.readings.isNotEmpty() })
                    val longPlacements = plan.days.flatMap { day ->
                        day.readings.filter { BibleData.isLongChapter(it.book, it.chapter) }
                            .map { day.date to it }
                    }
                    val longWeekendCount = longPlacements.count { (date) ->
                        date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    }
                    assertTrue(longWeekendCount >= longPlacements.size * 3 / 4)
                    assertFalse(plan.days.flatMap { it.readings }.any { it.key in setOf("GEN.10", "GEN.36", "JDG.5", "EZK.40") })
                    if (!extra) assertFalse(plan.days.flatMap { it.readings }.any { it.isDeuterocanonical })
                }
            }
        }
    }

    @Test
    fun everyStartingWeekdayHasEnoughCapacityForEveryPaceAndCanon() {
        val monday = java.time.LocalDate.of(2026, 9, 14)
        (0L..6L).forEach { offset ->
            ReadingPace.selectable.forEach { pace ->
                listOf(false, true).forEach { extra ->
                    val plan = PlanGenerator.generate(
                        monday.plusDays(offset),
                        "faith",
                        extra,
                        pace,
                        9_000L + offset * 100 + pace.ordinal * 10 + if (extra) 1 else 0,
                    )
                    assertEquals(pace.durationDays(extra), PlanGenerator.validate(plan).days)
                }
            }
        }
    }

    @Test
    fun seedsCreateUniquePlans() {
        val start = java.time.LocalDate.of(2026, 9, 16)
        val one = PlanGenerator.generate(start, "faith", true, ReadingPace.MODERATE, 1)
        val two = PlanGenerator.generate(start, "faith", true, ReadingPace.MODERATE, 2)
        assertNotEquals(one.days, two.days)
    }

    @Test
    fun explicitCyclesHaveExactCoverage() {
        val plan = PlanGenerator.generate(
            java.time.LocalDate.of(2026, 9, 16), "wisdom", true, ReadingPace.INTENSIVE, 77,
        )
        val readings = plan.days.flatMap { it.readings }
        assertEquals(300, readings.count { it.book == "PSA" })
        assertEquals(124, readings.count { it.book == "PRO" })
        assertEquals(178, readings.count { it.isGospel })
        assertEquals(2, readings.filter { it.book == "PSA" }.map { it.cycle }.distinct().size)
        assertEquals(4, readings.filter { it.book == "PRO" }.map { it.cycle }.distinct().size)
    }

    @Test
    fun youVersionLinksUseTheRequestedSpanishEdition() {
        val genesis = Reading("GEN", 1)
        assertEquals("https://www.bible.com/es/bible/146/GEN.1.RVC", youVersionUrl(genesis, "RVC"))
        assertEquals("https://www.bible.com/es/bible/127/GEN.1.NTV", youVersionUrl(genesis, "NTV"))
        assertEquals("https://www.bible.com/es/bible/178/GEN.1.TLAI", youVersionUrl(genesis, "TLAI"))
        assertEquals("https://www.bible.com/es/bible/197/GEN.1.PDT", youVersionUrl(genesis, "PDT"))
        assertEquals("https://www.bible.com/es/bible/753/GEN.1.NBV", youVersionUrl(genesis, "NBV"))
    }

    @Test
    fun deuterocanonicalLinksAlwaysUseTlai() {
        bibleTranslations.forEach { requested ->
            assertEquals(
                "https://www.bible.com/es/bible/178/TOB.1.TLAI",
                youVersionUrl(Reading("TOB", 1), requested.id),
            )
        }
    }

    @Test
    fun backupRoundTripPreservesPlanProgressAndVersionInACompactFile() {
        val plan = PlanGenerator.generate(
            java.time.LocalDate.of(2026, 9, 16), "hope", true, ReadingPace.MODERATE, 120L,
        )
        val completed = setOf("2026-09-16#0", "2026-09-16#1", "2027-03-14#2")
        val encoded = PlanBackupCodec.encode(PlanBackupData(plan, completed, "NBV"))
        val restored = PlanBackupCodec.decode(ByteArrayInputStream(encoded))

        assertEquals(plan, restored.plan)
        assertEquals(completed, restored.completed)
        assertEquals("NBV", restored.bibleVersion)
        assertTrue("El respaldo debería pesar menos de 100 KiB", encoded.size < 100 * 1024)
    }

    @Test
    fun semanticVersionComparisonOnlyAcceptsNewerReleases() {
        assertTrue(isNewerVersion("1.6.0", "1.7.0"))
        assertTrue(isNewerVersion("1.9.9", "2.0.0"))
        assertFalse(isNewerVersion("1.2.0", "1.2.0"))
        assertFalse(isNewerVersion("1.2.0", "1.1.9"))
    }

    @Test
    fun updateDiscoveryAcceptsTheApiAndPublicFallbackOnlyForNewerVersions() {
        val url = "https://github.com/csandino11/Senda/releases/download/v1.7.0/Senda-1.7.0.apk"
        val json = """{"tag_name":"v1.7.0","assets":[{"name":"Senda-1.7.0.apk","browser_download_url":"$url"}]}"""

        assertEquals("1.7.0", updateFromReleaseJson("1.6.0", json)?.version)
        assertEquals(url, updateFromReleaseLocation("1.6.0", "/csandino11/Senda/releases/tag/v1.7.0")?.downloadUrl)
        assertEquals(null, updateFromReleaseJson("1.7.0", json))
        assertEquals(null, updateFromReleaseLocation("1.7.0", "https://github.com/csandino11/Senda/releases/tag/v1.7.0"))
    }

    @Test
    fun everyThemeHasOneDynamicBackgroundAndThePaletteHasNoVioletDuplicate() {
        assertEquals(BibleData.themes.map { it.id }.toSet(), themeBackgrounds.keys)
        assertEquals(
            listOf("Bosque", "Cielo", "Lumbre", "Añil", "Mango", "Fucsia"),
            accentPalettes.map { it.name },
        )
    }

    @Test
    fun invalidBackupIsRejectedBeforeItCanReplaceLocalProgress() {
        try {
            PlanBackupCodec.decode(ByteArrayInputStream("no es un respaldo de Senda".toByteArray()))
            fail("Un archivo sin la firma de Senda debe rechazarse")
        } catch (_: IllegalArgumentException) {
            // Resultado esperado.
        }
    }

    @Test
    fun plansFromPreviousVersionsKeepTheirAnnualCompatibilityDefaults() {
        val current = PlanGenerator.generate(
            java.time.LocalDate.of(2026, 9, 16), "faith", false, ReadingPace.INTENSIVE, 33L,
        )
        val legacyJson = PlanJson.encode(current).apply {
            put("version", 2)
            remove("startDate")
            remove("pace")
        }
        val restored = PlanJson.decode(legacyJson)

        assertEquals(ReadingPace.LEGACY, restored.pace)
        assertEquals(java.time.LocalDate.of(2026, 1, 1), restored.startDate)
    }
}
