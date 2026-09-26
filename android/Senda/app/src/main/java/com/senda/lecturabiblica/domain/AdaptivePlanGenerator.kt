package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.Testament
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.PlanPreferences
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPace
import com.senda.lecturabiblica.model.ReadingPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

internal object AdaptivePlanGenerator {
    private data class Slot(
        val date: LocalDate,
        val maximum: Int,
        val readings: MutableList<Reading> = mutableListOf(),
    ) {
        val week: LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val hasLong: Boolean get() = readings.any { BibleData.hasAtLeastSixtyVerses(it.book, it.chapter) }
        val capacity: Int get() = if (hasLong && maximum >= 3) 2 else maximum
    }

    private fun LocalDate.isWeekend() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private fun baseReadings(includeDeuterocanon: Boolean): List<Reading> = BibleData.books.flatMap { book ->
        (1..book.chapters).filter { BibleData.allowed(book, it, includeDeuterocanon) }
            .map { Reading(book.id, it) }
    }

    fun totalReadings(includeDeuterocanon: Boolean, options: PlanPreferences): Int {
        val readings = baseReadings(includeDeuterocanon)
        return readings.size +
            readings.count { it.book == "PSA" } * (options.psalmCycles - 1) +
            readings.count { it.book == "PRO" } * (options.proverbCycles - 1) +
            readings.count { it.isGospel } * (options.gospelCycles - 1)
    }

    fun estimateDays(startDate: LocalDate, includeDeuterocanon: Boolean, options: PlanPreferences): Int {
        val needed = totalReadings(includeDeuterocanon, options)
        var capacity = 0
        var days = 0
        while (capacity < needed) {
            val date = startDate.plusDays(days.toLong())
            capacity += if (date.isWeekend()) options.weekendChapters else options.weekdayChapters
            days++
        }
        return days
    }

    fun generate(
        startDate: LocalDate,
        theme: String,
        includeDeuterocanon: Boolean,
        options: PlanPreferences,
        seed: Long,
    ): ReadingPlan {
        require(BibleData.themes.any { it.id == theme })
        val chapters = baseReadings(includeDeuterocanon)
        val minimum = estimateDays(startDate, includeDeuterocanon, options)
        var lastFailure: Throwable? = null
        for (count in minimum..minOf(minimum + 90, totalReadings(includeDeuterocanon, options))) {
            val result = runCatching { build(startDate, theme, includeDeuterocanon, options, seed, chapters, count) }
            result.onSuccess { return it }
            lastFailure = result.exceptionOrNull()
        }
        error("No fue posible distribuir todas las lecturas con esta combinación: ${lastFailure?.message}")
    }

    private fun build(
        startDate: LocalDate,
        theme: String,
        includeDeuterocanon: Boolean,
        options: PlanPreferences,
        seed: Long,
        chapters: List<Reading>,
        count: Int,
    ): ReadingPlan {
        val random = Random(seed)
        val slots = List(count) { offset ->
            val date = startDate.plusDays(offset.toLong())
            Slot(date, if (date.isWeekend()) options.weekendChapters else options.weekdayChapters)
        }
        val gospels = chapters.filter { it.isGospel }
        val psalms = chapters.filter { it.book == "PSA" }
        val proverbs = chapters.filter { it.book == "PRO" }
        val ordinary = chapters.filterNot { it.isGospel || it.book in setOf("PSA", "PRO") }
        val nt = ordinary.filter { BibleData.bookById.getValue(it.book).testament == Testament.NEW }
        val other = ordinary.filterNot { it in nt }

        spread(themed(nt, theme, random), slots, 0, count, theme, random, ordered = true)
        val gospelGap = maxOf(5, ceil(count.toDouble() / (gospels.size * options.gospelCycles)).toInt())
        var priorGospel = -1
        repeat(options.gospelCycles) { cycle ->
            val start = count * cycle / options.gospelCycles
            val end = count * (cycle + 1) / options.gospelCycles
            val order = if (cycle == 1) PlanGenerator.chronologicalGospels(gospels)
                else themed(gospels, theme, random)
            spread(order.mapIndexed { index, reading ->
                reading.copy(cycle = cycle + 1, gospelOrder = if (cycle == 1) index else null)
            }, slots, start, end, theme, random, ordered = true,
                maxGap = gospelGap, previousIndex = priorGospel)
            priorGospel = slots.indexOfLast { slot -> slot.readings.any { it.isGospel } }
        }

        repeat(options.psalmCycles) { cycle ->
            val start = count * cycle / options.psalmCycles
            val end = count * (cycle + 1) / options.psalmCycles
            spread(themed(psalms, theme, random).map { it.copy(cycle = cycle + 1) },
                slots, start, end, theme, random)
        }
        repeat(options.proverbCycles) { cycle ->
            val start = count * cycle / options.proverbCycles
            val end = count * (cycle + 1) / options.proverbCycles
            spread(themed(proverbs, theme, random).map { it.copy(cycle = cycle + 1) },
                slots, start, end, theme, random)
        }

        val remaining = other.toMutableList()
        val long = remaining.filter { BibleData.hasAtLeastSixtyVerses(it.book, it.chapter) }
        remaining.removeAll(long.toSet())
        spread(themed(long, theme, random), slots, 0, count, theme, random)

        require(slots.sumOf { it.capacity - it.readings.size } >= remaining.size) {
            "Capacidad insuficiente: días=$count, opciones=$options, libres=${slots.sumOf { it.capacity - it.readings.size }}, pendientes=${remaining.size}"
        }
        for ((dayIndex, slot) in slots.withIndex()) {
            while (slot.readings.size < slot.capacity && remaining.isNotEmpty()) {
                val index = remaining.indices.maxBy { candidate ->
                    connectionScore(remaining[candidate], slot.readings, theme) +
                        if (slot.readings.isEmpty()) {
                            val previous = slots.getOrNull(dayIndex - 1)?.readings.orEmpty()
                            connectionScore(remaining[candidate], previous, theme) / 3
                        } else 0
                }
                slot.readings += remaining.removeAt(index)
                if (slots.any { it.readings.isEmpty() }) break
            }
        }
        for (slot in slots) {
            while (slot.readings.size < slot.capacity && remaining.isNotEmpty()) {
                val index = remaining.indices.maxBy { connectionScore(remaining[it], slot.readings, theme) }
                slot.readings += remaining.removeAt(index)
            }
        }
        require(remaining.isEmpty()) { "Quedaron capítulos sin programar" }
        slots.filter { it.readings.isEmpty() }.forEach { empty ->
            val donor = slots.asSequence().filter { it.readings.size > 1 }
                .flatMap { slot -> slot.readings.asSequence().filter { reading ->
                    !reading.isGospel && !BibleData.hasAtLeastSixtyVerses(reading.book, reading.chapter)
                }.map { reading -> slot to reading } }
                .minByOrNull { (slot, reading) ->
                    abs(slots.indexOf(slot) - slots.indexOf(empty)) * 3 +
                        connectionScore(reading, slot.readings.filterNot { it === reading }, theme)
                } ?: error("No hay lectura que se pueda desplazar a un día libre")
            donor.first.readings.remove(donor.second)
            empty.readings += donor.second
        }
        require(slots.all { it.readings.isNotEmpty() }) { "El plan contiene días vacíos" }
        val days = slots.map { slot ->
            val readings = slot.readings.sortedWith(compareByDescending<Reading> { it.isGospel }
                .thenByDescending { theme in BibleData.tags(it.book, it.chapter) })
            val pair = readings.indices.asSequence().flatMap { a ->
                ((a + 1)..readings.lastIndex).asSequence().map { b -> readings[a] to readings[b] }
            }.firstOrNull { BibleData.directlyRelated(it.first.key, it.second.key) }
            val common = readings.map { BibleData.tags(it.book, it.chapter) }
                .reduceOrNull { a, b -> a.intersect(b) }.orEmpty()
            val focus = when {
                theme in common -> theme
                common.isNotEmpty() -> common.first()
                else -> theme
            }
            val label = BibleData.themes.first { it.id == focus }.name.lowercase()
            val connection = when {
                pair != null -> "En diálogo: ${pair.first.shortLabel} y ${pair.second.shortLabel}."
                common.isNotEmpty() -> "Hilo de hoy: $label."
                else -> "Enfoque del recorrido: ${BibleData.themes.first { it.id == theme }.name.lowercase()}."
            }
            DayPlan(slot.date, readings, focus, connection)
        }
        val plan = ReadingPlan(
            version = 4, id = UUID.randomUUID().toString(), year = startDate.year,
            theme = theme, includeDeuterocanon = includeDeuterocanon, seed = seed,
            createdAt = LocalDateTime.now().toString(), days = days, startDate = startDate,
            pace = ReadingPace.LEGACY, preferences = options,
        )
        validate(plan)
        return plan
    }

    private fun themed(source: List<Reading>, theme: String, random: Random): List<Reading> =
        source.shuffled(random).sortedByDescending { theme in BibleData.tags(it.book, it.chapter) }

    private fun spread(
        readings: List<Reading>,
        slots: List<Slot>,
        start: Int,
        end: Int,
        theme: String,
        random: Random,
        ordered: Boolean = false,
        maxGap: Int? = null,
        previousIndex: Int? = null,
    ) {
        if (readings.isEmpty()) return
        require(end - start >= readings.size || !ordered) { "Intervalo evangélico insuficiente" }
        var previous = previousIndex ?: start - 1
        readings.forEachIndexed { index, reading ->
            val ideal = start + ((index + .5) * (end - start) / readings.size).toInt()
            val lower = if (ordered) maxOf(start, previous + 1,
                if (maxGap != null && index == readings.lastIndex) end - maxGap else start) else start
            val upper = if (ordered) minOf(end - (readings.size - index),
                if (maxGap != null) previous + maxGap else end - 1) else end - 1
            val isLong = BibleData.hasAtLeastSixtyVerses(reading.book, reading.chapter)
            val candidates = (lower..upper).filter { day ->
                val slot = slots[day]
                val limit = if (isLong && slot.maximum >= 3) 2 else slot.capacity
                slot.readings.size < limit && (!reading.isGospel || slot.readings.none { it.isGospel }) &&
                    (!isLong || slots.none { it.week == slot.week && it.hasLong })
            }
            val chosen = candidates.minByOrNull { day ->
                val slot = slots[day]
                val relationship = connectionScore(reading, slot.readings, theme)
                val weekendBonus = if (isLong && slot.date.isWeekend()) -6 else 0
                abs(day - ideal) * 40 - relationship + weekendBonus +
                    slot.readings.count { it.book == reading.book } * 16 + random.nextInt(3)
            } ?: error("Sin espacio para ${reading.key}")
            slots[chosen].readings += reading
            previous = chosen
        }
    }

    private fun connectionScore(candidate: Reading, current: List<Reading>, theme: String): Int {
        val tags = BibleData.tags(candidate.book, candidate.chapter)
        var score = if (theme in tags) 12 else 0
        current.forEach { reading ->
            score += tags.intersect(BibleData.tags(reading.book, reading.chapter)).size * 12
            if (BibleData.directlyRelated(candidate.key, reading.key)) score += 90
            if (candidate.book == reading.book) score -= 8
        }
        return score
    }

    fun validate(plan: ReadingPlan): PlanGenerator.ValidationResult {
        val options = requireNotNull(plan.preferences)
        require(plan.days.isNotEmpty() && plan.days.first().date == plan.startDate)
        require(plan.days.mapIndexed { index, day -> day.date == plan.startDate.plusDays(index.toLong()) }.all { it })
        val all = plan.days.flatMap { it.readings }
        require(all.size == totalReadings(plan.includeDeuterocanon, options)) { "Cobertura incompleta" }
        val expected = baseReadings(plan.includeDeuterocanon)
        val counts = all.groupingBy { it.key }.eachCount()
        expected.forEach { reading ->
            val target = when {
                reading.isGospel -> options.gospelCycles
                reading.book == "PSA" -> options.psalmCycles
                reading.book == "PRO" -> options.proverbCycles
                else -> 1
            }
            require(counts[reading.key] == target) { "Cobertura: ${reading.key}" }
        }
        val longWeeks = mutableSetOf<LocalDate>()
        plan.days.forEach { day ->
            val max = if (day.date.isWeekend()) options.weekendChapters else options.weekdayChapters
            val long = day.readings.filter { BibleData.hasAtLeastSixtyVerses(it.book, it.chapter) }
            require(long.size <= 1)
            if (long.isNotEmpty()) {
                require(longWeeks.add(day.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))) {
                    "Más de un capítulo extenso en una semana"
                }
            }
            val actualMax = if (long.isNotEmpty() && max >= 3) 2 else max
            require(day.readings.size in 1..actualMax) { "Límite diario" }
            require(day.readings.count { it.isGospel } <= 1) { "Más de un Evangelio al día" }
        }
        for ((book, cycles) in listOf("PSA" to options.psalmCycles, "PRO" to options.proverbCycles)) {
            val sequences = (1..cycles).map { cycle -> all.filter { it.book == book && it.cycle == cycle }.map { it.chapter } }
            require(sequences.all { it.size == (if (book == "PSA") 150 else 31) })
            require(sequences.distinct().size == cycles) { "Orden repetido en $book" }
        }
        (1..options.gospelCycles).forEach { cycle ->
            val ordered = all.filter { it.isGospel && it.cycle == cycle }
            require(ordered.size == 89 && ordered.map { it.key }.toSet().size == 89)
            if (cycle == 2) require(ordered.map { it.gospelOrder } == (0 until 89).toList())
        }
        val ntDays = plan.days.mapIndexedNotNull { index, day ->
            index.takeIf { day.readings.any { reading ->
                !reading.isGospel && BibleData.bookById.getValue(reading.book).testament == Testament.NEW
            } }
        }
        return PlanGenerator.ValidationResult(plan.days.size, all.size, expected.count { !it.isGospel }, ntDays.size)
    }
}
