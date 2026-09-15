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
    val version: Int = 2,
    val id: String,
    val year: Int,
    val theme: String,
    val includeDeuterocanon: Boolean,
    val seed: Long,
    val createdAt: String,
    val days: List<DayPlan>,
)

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
