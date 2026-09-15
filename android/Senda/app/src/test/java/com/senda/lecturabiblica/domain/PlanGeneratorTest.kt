package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.PlanBackupCodec
import com.senda.lecturabiblica.data.PlanBackupData
import com.senda.lecturabiblica.data.isNewerVersion
import com.senda.lecturabiblica.model.Reading
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
    fun everyThemeAndCanonOptionProducesACompleteValidYear() {
        listOf(2026, 2028).forEach { year ->
            BibleData.themes.forEachIndexed { index, theme ->
                listOf(false, true).forEach { extra ->
                    val plan = PlanGenerator.generate(year, theme.id, extra, year * 100L + index + if (extra) 50 else 0)
                    val result = PlanGenerator.validate(plan)
                    assertEquals(if (year == 2028) 366 else 365, result.days)
                    assertTrue(plan.days.all { it.readings.count { reading -> reading.isGospel } == 1 })
                    assertTrue(plan.days.all { day ->
                        day.readings.size <= if (day.date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) 4 else 5
                    })
                    assertTrue(plan.days.all { it.readings.size >= 3 })
                    assertFalse(plan.days.flatMap { it.readings }.any { it.key in setOf("GEN.10", "GEN.36", "JDG.5", "EZK.40") })
                    if (!extra) assertFalse(plan.days.flatMap { it.readings }.any { it.isDeuterocanonical })
                }
            }
        }
    }

    @Test
    fun seedsCreateUniquePlans() {
        val one = PlanGenerator.generate(2026, "faith", true, 1)
        val two = PlanGenerator.generate(2026, "faith", true, 2)
        assertNotEquals(one.days, two.days)
    }

    @Test
    fun explicitCyclesHaveExactCoverage() {
        val plan = PlanGenerator.generate(2026, "wisdom", true, 77)
        val readings = plan.days.flatMap { it.readings }
        assertEquals(300, readings.count { it.book == "PSA" })
        assertEquals(124, readings.count { it.book == "PRO" })
        assertEquals(365, readings.count { it.isGospel })
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
        val plan = PlanGenerator.generate(2026, "hope", true, 120L)
        val completed = setOf("2026-01-01#0", "2026-01-01#1", "2026-03-14#2")
        val encoded = PlanBackupCodec.encode(PlanBackupData(plan, completed, "NBV"))
        val restored = PlanBackupCodec.decode(ByteArrayInputStream(encoded))

        assertEquals(plan, restored.plan)
        assertEquals(completed, restored.completed)
        assertEquals("NBV", restored.bibleVersion)
        assertTrue("El respaldo debería pesar menos de 100 KiB", encoded.size < 100 * 1024)
    }

    @Test
    fun semanticVersionComparisonOnlyAcceptsNewerReleases() {
        assertTrue(isNewerVersion("1.1.0", "1.2.0"))
        assertTrue(isNewerVersion("1.9.9", "2.0.0"))
        assertFalse(isNewerVersion("1.2.0", "1.2.0"))
        assertFalse(isNewerVersion("1.2.0", "1.1.9"))
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
}
