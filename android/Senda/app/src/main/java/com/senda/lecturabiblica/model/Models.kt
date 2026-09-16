package com.senda.lecturabiblica.model

import com.senda.lecturabiblica.data.BibleData
import java.time.LocalDate

data class Reading(
    val book: String,
    val chapter: Int,
    val cycle: Int = 1,
    val gospelOrder: Int? = null,
) {
    val key: String get() = "$book.$chapter"
    val label: String get() = "${BibleData.bookById.getValue(book).name} $chapter"
    val shortLabel: String get() = "${BibleData.bookById.getValue(book).shortName} $chapter"
    val isGospel: Boolean get() = book in BibleData.gospelIds
    val isDeuterocanonical: Boolean get() = BibleData.bookById.getValue(book).deuterocanonical
}

data class DayPlan(
    val date: LocalDate,
    val readings: List<Reading>,
    val focus: String,
    val connection: String,
)

data class ReadingPlan(
    val version: Int = 3,
    val id: String,
    val year: Int,
    val theme: String,
    val includeDeuterocanon: Boolean,
    val seed: Long,
    val createdAt: String,
    val days: List<DayPlan>,
    val startDate: LocalDate = LocalDate.of(year, 1, 1),
    val pace: ReadingPace = ReadingPace.LEGACY,
)

enum class ReadingPace(
    val id: String,
    val label: String,
    val information: String,
    val weekdayMaximum: Int,
    val weekendMaximum: Int,
    private val daysWithoutDeuterocanon: Int,
    private val daysWithDeuterocanon: Int,
) {
    SOFT("soft", "Suave", "Lectura ligera. Al menos 3 lecturas diarias", 3, 2, 520, 558),
    MODERATE("moderate", "Moderado", "Mayor ritmo. 4 lecturas diarias.", 4, 3, 380, 408),
    INTENSIVE("intensive", "Intensivo", "Al máximo. Hasta 5 lecturas diarias.", 5, 4, 300, 322),
    LEGACY("legacy", "Anual", "Plan anual creado con una versión anterior.", 5, 4, 365, 366),
    ;

    fun durationDays(includeDeuterocanon: Boolean): Int =
        if (includeDeuterocanon) daysWithDeuterocanon else daysWithoutDeuterocanon

    companion object {
        val selectable = listOf(SOFT, MODERATE, INTENSIVE)
        fun fromId(id: String?): ReadingPace = entries.firstOrNull { it.id == id } ?: LEGACY
    }
}

val ReadingPlan.endDate: LocalDate get() = days.last().date

enum class DayStatus { NONE, PARTIAL, COMPLETE }

data class ProgressStats(
    val completedReadings: Int,
    val totalReadings: Int,
    val completeDays: Int,
    val partialDays: Int,
    val unreadDays: Int,
) {
    val readDays: Int get() = completeDays + partialDays
    val fraction: Float get() = if (totalReadings == 0) 0f else completedReadings.toFloat() / totalReadings
    val percent: Int get() = (fraction * 100).toInt().coerceIn(0, 100)
}
