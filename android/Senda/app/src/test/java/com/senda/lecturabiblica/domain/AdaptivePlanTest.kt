package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.PlanJson
import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.Testament
import com.senda.lecturabiblica.model.PlanPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.ceil

class AdaptivePlanTest {
    @Test
    fun selectedChapterLimitsAndRepetitionsProduceExactCoverage() {
        val date = LocalDate.of(2026, 9, 25)
        val settings = listOf(
            PlanPreferences(1, 1, false, 1, false),
            PlanPreferences(3, 2, true, 3, true),
            PlanPreferences(4, 3, true, 4, true),
        )
        settings.forEachIndexed { index, preferences ->
            listOf(false, true).forEach { deuterocanon ->
                val plan = PlanGenerator.generate(date, "faith", deuterocanon, preferences, index * 10L + if (deuterocanon) 1 else 0)
                assertTrue(plan.days.size - PlanGenerator.estimateDays(date, deuterocanon, preferences) <= 40)
                assertEquals(AdaptivePlanGenerator.totalReadings(deuterocanon, preferences), plan.days.sumOf { it.readings.size })
                assertEquals(preferences, PlanJson.decode(PlanJson.encode(plan)).preferences)
                assertTrue(plan.days.none { it.readings.isEmpty() })
                val underfilled = plan.days.count { day ->
                    val max = if (day.date.dayOfWeek.value >= 6) preferences.weekendChapters else preferences.weekdayChapters
                    day.readings.size < max && day.readings.none { BibleData.hasAtLeastSixtyVerses(it.book, it.chapter) }
                }
                val gospelIndices = plan.days.indices.filter { index -> plan.days[index].readings.any { it.isGospel } }
                val ntIndices = plan.days.indices.filter { index -> plan.days[index].readings.any {
                    !it.isGospel && BibleData.bookById.getValue(it.book).testament == Testament.NEW
                } }
                val gospelLimit = maxOf(5, ceil(plan.days.size.toDouble() / gospelIndices.size).toInt())
                val ntLimit = maxOf(7, ceil(plan.days.size.toDouble() / ntIndices.size).toInt())
                assertTrue("Intervalo evangélico excesivo", gospelIndices.zipWithNext { a, b -> b - a }.all { it <= gospelLimit })
                assertTrue("Intervalo del Nuevo Testamento excesivo", ntIndices.zipWithNext { a, b -> b - a }.all { it <= ntLimit })
                assertTrue("Demasiados días por debajo de lo solicitado", underfilled <= maxOf(1, plan.days.size / 30))
            }
        }
    }

    @Test fun asymmetricCadencesGenerateWithoutOmissions() {
        val date = LocalDate.of(2026, 12, 31)
        val cases = listOf(
            PlanPreferences(1, 3, false, 2, true),
            PlanPreferences(4, 1, true, 1, false),
            PlanPreferences(2, 3, true, 4, false),
            PlanPreferences(3, 1, false, 3, true),
        )
        cases.forEachIndexed { index, settings ->
            val plan = PlanGenerator.generate(date, BibleData.themes[index].id, index % 2 == 0, settings, 78L + index)
            assertEquals(AdaptivePlanGenerator.totalReadings(plan.includeDeuterocanon, settings),
                plan.days.sumOf { it.readings.size })
            val gospelIndices = plan.days.indices.filter { day -> plan.days[day].readings.any { it.isGospel } }
            val limit = maxOf(5, ceil(plan.days.size.toDouble() / gospelIndices.size).toInt())
            assertTrue(gospelIndices.zipWithNext { a, b -> b - a }.all { it <= limit })
        }
    }
}
