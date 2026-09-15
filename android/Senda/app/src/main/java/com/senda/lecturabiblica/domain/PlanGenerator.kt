package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.Testament
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

object PlanGenerator {
    fun generate(
        year: Int,
        theme: String,
        includeDeuterocanon: Boolean,
        seed: Long = kotlin.random.Random.nextLong(),
    ): ReadingPlan {
        require(year in 1900..2400)
        require(BibleData.themes.any { it.id == theme })

        val random = Random(seed)
        val dates = generateSequence(LocalDate.of(year, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == year }.toList()
        val slots = dates.map { MutableDay(it) }
        val split = dates.indexOfFirst { it.monthValue == 7 }
        val all = BibleData.books.flatMap { book ->
            (1..book.chapters).filter { BibleData.allowed(book, it, includeDeuterocanon) }
                .map { Reading(book.id, it) }
        }
        val psalms = all.filter { it.book == "PSA" }
        val proverbs = all.filter { it.book == "PRO" }
        val gospels = all.filter { it.isGospel }
        val ordinary = all.filterNot { it.book == "PSA" || it.book == "PRO" || it.isGospel }.toMutableList()

        // A Gospel chapter is present every day. Each semester has its requested
        // identity: a thematic shuffled sequence, then a ministry chronology.
        val firstGospels = repeatedToLength(
            source = { weighted(gospels, theme, random) }, length = split,
        ).map { it.copy(cycle = 1) }
        val chronological = chronologicalGospels(gospels)
        val secondGospels = List(slots.size - split) { index ->
            chronological[index % chronological.size].copy(cycle = 2, gospelOrder = index % chronological.size)
        }
        firstGospels.forEachIndexed { index, reading -> slots[index].readings += reading }
        secondGospels.forEachIndexed { index, reading -> slots[index + split].readings += reading }

        val firstPsalmOrder = weighted(psalms, theme, random)
        val secondPsalmOrder = distinctWeighted(psalms, theme, random, setOf(signature(firstPsalmOrder)))
        spread(firstPsalmOrder.map { it.copy(cycle = 1) }, slots, 0, split)
        spread(secondPsalmOrder.map { it.copy(cycle = 2) }, slots, split, slots.size)

        val proverbSignatures = mutableSetOf<String>()
        repeat(4) { quarter ->
            val order = distinctWeighted(proverbs, theme, random, proverbSignatures)
            proverbSignatures += signature(order)
            val startMonth = quarter * 3 + 1
            val start = dates.indexOfFirst { it.monthValue == startMonth }
            val end = if (quarter == 3) dates.size else dates.indexOfFirst { it.monthValue == startMonth + 3 }
            spread(order.map { it.copy(cycle = quarter + 1) }, slots, start, end)
        }

        val totalReadings = slots.size + psalms.size * 2 + proverbs.size * 4 + ordinary.size
        val targets = slots.map { day ->
            maxOf(day.readings.size, if (day.date.isWeekend()) 3 else 4)
        }.toMutableList()
        var capacityNeeded = totalReadings - targets.sum()
        require(capacityNeeded >= 0) { "El calendario base excede las lecturas disponibles." }
        val weekdays = slots.indices.filterNot { slots[it].date.isWeekend() }.shuffled(random)
        val weekends = slots.indices.filter { slots[it].date.isWeekend() }.shuffled(random)
        for (index in weekdays + weekends) {
            if (capacityNeeded == 0) break
            val maximum = if (slots[index].date.isWeekend()) 4 else 5
            if (targets[index] < maximum) {
                targets[index]++
                capacityNeeded--
            }
        }
        require(capacityNeeded == 0) { "Las reglas diarias no ofrecen suficiente capacidad." }

        // The non-Gospel New Testament is deliberately spread from January to
        // December before filling the other slots.
        val newTestament = ordinary.filter {
            BibleData.bookById.getValue(it.book).testament == Testament.NEW
        }.toMutableList()
        ordinary.removeAll(newTestament.toSet())
        val orderedNt = weighted(newTestament, theme, random).toMutableList()
        val ntDays = evenlySpacedIndices(orderedNt.size, slots.size)
        ntDays.forEach { preferred ->
            val dayIndex = nearestWithCapacity(preferred, slots, targets)
            slots[dayIndex].readings += takeBest(orderedNt, slots[dayIndex].readings, theme, random)
        }
        ordinary += orderedNt

        val pool = weighted(ordinary, theme, random).toMutableList()
        while (pool.isNotEmpty()) {
            val dayIndex = slots.indices
                .filter { slots[it].readings.size < targets[it] }
                .minWithOrNull(compareBy<Int> { slots[it].readings.size.toDouble() / targets[it] }
                    .thenBy { random.nextDouble() })
                ?: error("No quedan espacios para ${pool.size} lecturas.")
            slots[dayIndex].readings += takeBest(pool, slots[dayIndex].readings, theme, random)
        }

        val days = slots.map { day ->
            val counts = BibleData.themes.associate { option ->
                option.id to day.readings.count { option.id in BibleData.tags(it.book, it.chapter) }
            }
            val focus = counts.maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }
                .thenBy { if (it.key == theme) 1 else 0 })?.key ?: theme
            val directPair = day.readings.indices.asSequence().flatMap { a ->
                ((a + 1)..day.readings.lastIndex).asSequence().map { b -> day.readings[a] to day.readings[b] }
            }.firstOrNull { BibleData.directlyRelated(it.first.key, it.second.key) }
            val connection = directPair?.let { "En diálogo: ${it.first.shortLabel} y ${it.second.shortLabel}." }
                ?: "Un hilo común: ${BibleData.themes.first { it.id == focus }.name.lowercase()}."
            val gospel = day.readings.filter { it.isGospel }
            val rest = day.readings.filterNot { it.isGospel }.shuffled(random)
            DayPlan(day.date, gospel + rest, focus, connection)
        }

        val plan = ReadingPlan(
            id = UUID.randomUUID().toString(), year = year, theme = theme,
            includeDeuterocanon = includeDeuterocanon, seed = seed,
            createdAt = LocalDateTime.now().toString(), days = days,
        )
        validate(plan)
        return plan
    }

    fun validate(plan: ReadingPlan): ValidationResult {
        val errors = mutableListOf<String>()
        val expectedDates = generateSequence(LocalDate.of(plan.year, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == plan.year }.toList()
        if (plan.days.map { it.date } != expectedDates) errors += "Calendario incompleto"
        plan.days.forEach { day ->
            val maximum = if (day.date.isWeekend()) 4 else 5
            if (day.readings.size !in 2..maximum) errors += "Límite diario"
            if (day.readings.count { it.isGospel } != 1) errors += "Evangelio diario"
            day.readings.forEach { reading ->
                val book = BibleData.bookById[reading.book]
                if (book == null || !BibleData.allowed(book, reading.chapter, plan.includeDeuterocanon)) {
                    errors += "Lectura excluida: ${reading.key}"
                }
            }
        }
        val firstHalf = plan.days.filter { it.date.monthValue <= 6 }
        val secondHalf = plan.days.filter { it.date.monthValue >= 7 }
        if (firstHalf.any { day -> day.readings.filter { it.isGospel }.any { it.cycle != 1 } } ||
            secondHalf.any { day -> day.readings.filter { it.isGospel }.any { it.cycle != 2 } }) errors += "Ciclo evangélico"
        BibleData.gospelIds.forEach { id ->
            if (firstHalf.none { day -> day.readings.any { it.book == id } } ||
                secondHalf.none { day -> day.readings.any { it.book == id } }) errors += "Cobertura evangélica"
        }

        val expected = mutableMapOf<String, Int>()
        BibleData.books.forEach { book ->
            (1..book.chapters).filter { BibleData.allowed(book, it, plan.includeDeuterocanon) }.forEach { chapter ->
                if (book.id !in BibleData.gospelIds) {
                    expected["${book.id}.$chapter"] = when (book.id) { "PSA" -> 2; "PRO" -> 4; else -> 1 }
                }
            }
        }
        val actual = plan.days.flatMap { it.readings }.filterNot { it.isGospel }.groupingBy { it.key }.eachCount()
        expected.forEach { (key, count) -> if (actual[key] != count) errors += "Cobertura: $key" }

        val psalmSequences = (1..2).map { cycle -> plan.days.flatMap { it.readings }.filter { it.book == "PSA" && it.cycle == cycle }.map { it.chapter } }
        val proverbSequences = (1..4).map { cycle -> plan.days.flatMap { it.readings }.filter { it.book == "PRO" && it.cycle == cycle }.map { it.chapter } }
        if (psalmSequences.distinct().size != 2) errors += "Orden repetido de Salmos"
        if (proverbSequences.distinct().size != 4) errors += "Orden repetido de Proverbios"
        if (psalmSequences.any { it.size != 150 || it.toSet().size != 150 }) errors += "Cobertura de Salmos"
        if (proverbSequences.any { it.size != 31 || it.toSet().size != 31 }) errors += "Cobertura de Proverbios"

        val ntDays = plan.days.mapIndexedNotNull { index, day ->
            index.takeIf { day.readings.any { r -> !r.isGospel && BibleData.bookById.getValue(r.book).testament == Testament.NEW } }
        }
        if (ntDays.zipWithNext().any { (a, b) -> b - a > 4 } || ntDays.firstOrNull() ?: 99 > 3 ||
            plan.days.lastIndex - (ntDays.lastOrNull() ?: -99) > 3) errors += "Distribución del Nuevo Testamento"

        require(errors.isEmpty()) { errors.distinct().joinToString("; ") }
        return ValidationResult(plan.days.size, plan.days.sumOf { it.readings.size }, expected.size, ntDays.size)
    }

    data class ValidationResult(val days: Int, val readings: Int, val uniqueNonGospelChapters: Int, val daysWithNewTestament: Int)
    private data class MutableDay(val date: LocalDate, val readings: MutableList<Reading> = mutableListOf())

    private fun LocalDate.isWeekend() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private fun weighted(source: List<Reading>, theme: String, random: Random): List<Reading> =
        source.shuffled(random).sortedByDescending { if (theme in BibleData.tags(it.book, it.chapter)) 1 else 0 }

    private fun distinctWeighted(source: List<Reading>, theme: String, random: Random, previous: Set<String>): List<Reading> {
        repeat(20) {
            val candidate = weighted(source, theme, random)
            if (signature(candidate) !in previous) return candidate
        }
        return weighted(source, theme, random).let { it.drop(1) + it.first() }
    }

    private fun signature(readings: List<Reading>) = readings.joinToString(",") { it.chapter.toString() }

    private fun repeatedToLength(source: () -> List<Reading>, length: Int): List<Reading> {
        val result = mutableListOf<Reading>()
        while (result.size < length) result += source()
        return result.take(length)
    }

    private fun chronologicalGospels(gospels: List<Reading>): List<Reading> {
        val stage = mapOf(
            "MAT" to listOf(0,0,1,3,3,3,3,3,3,4,3,3,3,4,4,4,4,4,5,5,6,7,7,7,7,8,10,11),
            "MRK" to listOf(1,3,3,3,3,4,4,4,4,5,6,7,7,8,10,11),
            "LUK" to listOf(0,0,1,1,3,3,3,3,4,5,5,5,5,5,5,5,5,5,6,7,7,8,10,11),
            "JHN" to listOf(1,2,2,2,3,4,5,5,5,5,5,6,8,8,8,8,8,9,10,11,11),
        )
        val bookOrder = listOf("LUK", "MAT", "MRK", "JHN")
        return gospels.sortedWith(compareBy<Reading> { stage.getValue(it.book)[it.chapter - 1] }
            .thenBy { it.chapter }.thenBy { bookOrder.indexOf(it.book) })
    }

    private fun spread(readings: List<Reading>, days: List<MutableDay>, start: Int, end: Int) {
        val length = end - start
        readings.forEachIndexed { index, reading ->
            val ideal = start + floor((index + .45) * length / readings.size).toInt().coerceIn(0, length - 1)
            val selected = (start until end).minWithOrNull(compareBy<Int> {
                kotlin.math.abs(it - ideal) * 10 + days[it].readings.count { r -> r.book == reading.book } * 100
            }.thenBy { days[it].readings.size }) ?: ideal
            days[selected].readings += reading
        }
    }

    private fun evenlySpacedIndices(count: Int, dayCount: Int): List<Int> =
        List(count) { floor((it + .5) * dayCount / count).toInt().coerceIn(0, dayCount - 1) }

    private fun nearestWithCapacity(preferred: Int, days: List<MutableDay>, targets: List<Int>): Int =
        days.indices.filter { days[it].readings.size < targets[it] }
            .minWithOrNull(compareBy<Int> { kotlin.math.abs(it - preferred) }
                .thenBy { days[it].readings.size }) ?: error("No hay capacidad disponible")

    private fun takeBest(
        pool: MutableList<Reading>,
        existing: List<Reading>,
        theme: String,
        random: Random,
    ): Reading {
        val index = pool.indices.maxByOrNull { index ->
            val candidate = pool[index]
            var score = if (theme in BibleData.tags(candidate.book, candidate.chapter)) 4.0 else 0.0
            existing.forEach { current ->
                score += BibleData.tags(candidate.book, candidate.chapter)
                    .intersect(BibleData.tags(current.book, current.chapter)).size * 2.5
                if (BibleData.directlyRelated(candidate.key, current.key)) score += 30
                if (candidate.book == current.book) score -= 8
            }
            score + random.nextDouble()
        } ?: error("No quedan lecturas")
        return pool.removeAt(index)
    }
}
