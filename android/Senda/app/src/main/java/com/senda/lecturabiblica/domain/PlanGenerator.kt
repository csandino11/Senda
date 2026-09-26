package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.data.BibleData
import com.senda.lecturabiblica.data.Testament
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.PlanPreferences
import com.senda.lecturabiblica.model.ReadingPace
import com.senda.lecturabiblica.model.ReadingPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

object PlanGenerator {
    fun generate(
        startDate: LocalDate,
        theme: String,
        includeDeuterocanon: Boolean,
        preferences: PlanPreferences,
        seed: Long = Random.nextLong(),
    ): ReadingPlan = AdaptivePlanGenerator.generate(startDate, theme, includeDeuterocanon, preferences, seed)

    fun estimateDays(startDate: LocalDate, includeDeuterocanon: Boolean, preferences: PlanPreferences): Int =
        AdaptivePlanGenerator.estimateDays(startDate, includeDeuterocanon, preferences)

    fun generate(
        startDate: LocalDate,
        theme: String,
        includeDeuterocanon: Boolean,
        pace: ReadingPace,
        seed: Long = Random.nextLong(),
    ): ReadingPlan {
        require(BibleData.themes.any { it.id == theme })
        require(pace in ReadingPace.selectable)

        val random = Random(seed)
        val dates = List(pace.durationDays(includeDeuterocanon)) { startDate.plusDays(it.toLong()) }
        val slots = dates.map(::MutableDay)
        val split = slots.size / 2
        val all = BibleData.books.flatMap { book ->
            (1..book.chapters)
                .filter { BibleData.allowed(book, it, includeDeuterocanon) }
                .map { Reading(book.id, it) }
        }
        val psalms = all.filter { it.book == "PSA" }
        val proverbs = all.filter { it.book == "PRO" }
        val gospels = all.filter { it.isGospel }
        val ordinary = all.filterNot { it.book == "PSA" || it.book == "PRO" || it.isGospel }.toMutableList()
        val totalReadings = ordinary.size + psalms.size * 2 + proverbs.size * 4 + gospels.size * 2

        val targets = dates.map { if (it.isWeekend()) pace.weekendMaximum else pace.weekdayMaximum }.toMutableList()
        val psalm119Days = listOf(
            reservedWeekend(dates, 0, split),
            reservedWeekend(dates, split, dates.size),
        )
        if (pace != ReadingPace.SOFT) psalm119Days.forEach { targets[it]-- }
        reduceCapacityEvenly(targets, dates, totalReadings, psalm119Days.toSet())

        val firstPsalmOrder = weighted(psalms, theme, random)
        val secondPsalmOrder = distinctWeighted(psalms, theme, random, setOf(signature(firstPsalmOrder)))
        placeReservedPsalm(firstPsalmOrder, 1, psalm119Days[0], slots)
        placeReservedPsalm(secondPsalmOrder, 2, psalm119Days[1], slots)

        // El Nuevo Testamento no evangélico se reserva antes de llenar los demás
        // ciclos, para que mantenga una presencia verdaderamente uniforme.
        val longNewTestament = ordinary.filter {
            BibleData.bookById.getValue(it.book).testament == Testament.NEW &&
                BibleData.isLongChapter(it.book, it.chapter)
        }
        ordinary.removeAll(longNewTestament.toSet())
        spread(weighted(longNewTestament, theme, random), slots, targets, 0, slots.size)
        val newTestament = ordinary.filter {
            BibleData.bookById.getValue(it.book).testament == Testament.NEW
        }.toMutableList()
        ordinary.removeAll(newTestament.toSet())
        val orderedNt = weighted(newTestament, theme, random).toMutableList()
        evenlySpacedIndices(orderedNt.size, slots.size).forEach { preferred ->
            val dayIndex = nearestWithCapacity(preferred, slots, targets)
            slots[dayIndex].readings += takeBest(orderedNt, slots[dayIndex].readings, theme, random)
        }

        spreadOrdered(weighted(gospels, theme, random).map { it.copy(cycle = 1) }, slots, targets, 0, split)
        spreadOrdered(
            chronologicalGospels(gospels).mapIndexed { index, reading ->
                reading.copy(cycle = 2, gospelOrder = index)
            },
            slots, targets, split, slots.size,
        )

        spread(
            firstPsalmOrder.filterNot { it.chapter == 119 }.map { it.copy(cycle = 1) },
            slots, targets, 0, split,
        )
        spread(
            secondPsalmOrder.filterNot { it.chapter == 119 }.map { it.copy(cycle = 2) },
            slots, targets, split, slots.size,
        )

        val proverbSignatures = mutableSetOf<String>()
        repeat(4) { quarter ->
            val order = distinctWeighted(proverbs, theme, random, proverbSignatures)
            proverbSignatures += signature(order)
            val start = floor(quarter * slots.size / 4.0).toInt()
            val end = floor((quarter + 1) * slots.size / 4.0).toInt()
            spread(order.map { it.copy(cycle = quarter + 1) }, slots, targets, start, end)
        }

        val longOrdinary = ordinary.filter { BibleData.isLongChapter(it.book, it.chapter) }
        ordinary.removeAll(longOrdinary.toSet())
        spread(weighted(longOrdinary, theme, random), slots, targets, 0, slots.size)

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
            id = UUID.randomUUID().toString(),
            year = startDate.year,
            theme = theme,
            includeDeuterocanon = includeDeuterocanon,
            seed = seed,
            createdAt = LocalDateTime.now().toString(),
            days = days,
            startDate = startDate,
            pace = pace,
        )
        validate(plan)
        return plan
    }

    fun validate(plan: ReadingPlan): ValidationResult =
        when {
            plan.version >= 4 && plan.preferences != null -> AdaptivePlanGenerator.validate(plan)
            plan.version < 3 || plan.pace == ReadingPace.LEGACY -> validateLegacy(plan)
            else -> validateFlexible(plan)
        }

    private fun validateFlexible(plan: ReadingPlan): ValidationResult {
        val errors = mutableListOf<String>()
        val expectedDates = List(plan.pace.durationDays(plan.includeDeuterocanon)) {
            plan.startDate.plusDays(it.toLong())
        }
        if (plan.days.map { it.date } != expectedDates) errors += "Calendario incompleto"
        plan.days.forEach { day ->
            val maximum = if (day.date.isWeekend()) plan.pace.weekendMaximum else plan.pace.weekdayMaximum
            if (day.readings.size !in 1..maximum) errors += "Límite diario"
            day.readings.forEach { reading ->
                val book = BibleData.bookById[reading.book]
                if (book == null || !BibleData.allowed(book, reading.chapter, plan.includeDeuterocanon)) {
                    errors += "Lectura excluida: ${reading.key}"
                }
            }
            if (day.readings.any { it.book == "PSA" && it.chapter == 119 }) {
                val expectedMaximum = when (plan.pace) {
                    ReadingPace.MODERATE -> 2
                    ReadingPace.INTENSIVE -> 3
                    else -> plan.pace.weekendMaximum
                }
                if (!day.date.isWeekend() || day.readings.size > expectedMaximum) errors += "Carga de Salmo 119"
            }
        }
        validateCyclesAndCoverage(plan, errors, exactGospelCycles = true)
        validateNewTestamentDistribution(plan, errors)
        require(errors.isEmpty()) { errors.distinct().joinToString("; ") }
        val expected = expectedNonGospelCoverage(plan)
        val ntDays = newTestamentDays(plan)
        return ValidationResult(plan.days.size, plan.days.sumOf { it.readings.size }, expected.size, ntDays.size)
    }

    private fun validateLegacy(plan: ReadingPlan): ValidationResult {
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
            secondHalf.any { day -> day.readings.filter { it.isGospel }.any { it.cycle != 2 } }
        ) errors += "Ciclo evangélico"
        BibleData.gospelIds.forEach { id ->
            if (firstHalf.none { day -> day.readings.any { it.book == id } } ||
                secondHalf.none { day -> day.readings.any { it.book == id } }
            ) errors += "Cobertura evangélica"
        }
        validateCyclesAndCoverage(plan, errors, exactGospelCycles = false)
        validateNewTestamentDistribution(plan, errors)
        require(errors.isEmpty()) { errors.distinct().joinToString("; ") }
        val expected = expectedNonGospelCoverage(plan)
        val ntDays = newTestamentDays(plan)
        return ValidationResult(plan.days.size, plan.days.sumOf { it.readings.size }, expected.size, ntDays.size)
    }

    private fun validateCyclesAndCoverage(plan: ReadingPlan, errors: MutableList<String>, exactGospelCycles: Boolean) {
        val expected = expectedNonGospelCoverage(plan)
        val actual = plan.days.flatMap { it.readings }.filterNot { it.isGospel }.groupingBy { it.key }.eachCount()
        expected.forEach { (key, count) -> if (actual[key] != count) errors += "Cobertura: $key" }

        val psalmSequences = (1..2).map { cycle ->
            plan.days.flatMap { it.readings }.filter { it.book == "PSA" && it.cycle == cycle }.map { it.chapter }
        }
        val proverbSequences = (1..4).map { cycle ->
            plan.days.flatMap { it.readings }.filter { it.book == "PRO" && it.cycle == cycle }.map { it.chapter }
        }
        if (psalmSequences.distinct().size != 2) errors += "Orden repetido de Salmos"
        if (proverbSequences.distinct().size != 4) errors += "Orden repetido de Proverbios"
        if (psalmSequences.any { it.size != 150 || it.toSet().size != 150 }) errors += "Cobertura de Salmos"
        if (proverbSequences.any { it.size != 31 || it.toSet().size != 31 }) errors += "Cobertura de Proverbios"

        if (exactGospelCycles) {
            val split = plan.days.size / 2
            val first = plan.days.take(split).flatMap { it.readings }.filter { it.isGospel }
            val second = plan.days.drop(split).flatMap { it.readings }.filter { it.isGospel }
            if (first.size != 89 || first.any { it.cycle != 1 } || first.map { it.key }.toSet().size != 89) {
                errors += "Primer ciclo evangélico"
            }
            if (second.size != 89 || second.any { it.cycle != 2 } || second.map { it.key }.toSet().size != 89) {
                errors += "Segundo ciclo evangélico"
            }
            if (second.map { it.gospelOrder } != (0 until 89).map { it }) errors += "Orden cronológico evangélico"
        }
    }

    private fun expectedNonGospelCoverage(plan: ReadingPlan): Map<String, Int> = buildMap {
        BibleData.books.forEach { book ->
            (1..book.chapters)
                .filter { BibleData.allowed(book, it, plan.includeDeuterocanon) }
                .forEach { chapter ->
                    if (book.id !in BibleData.gospelIds) {
                        put("${book.id}.$chapter", when (book.id) { "PSA" -> 2; "PRO" -> 4; else -> 1 })
                    }
                }
        }
    }

    private fun validateNewTestamentDistribution(plan: ReadingPlan, errors: MutableList<String>) {
        val ntDays = newTestamentDays(plan)
        if (ntDays.zipWithNext().any { (a, b) -> b - a > 4 } || ntDays.firstOrNull() ?: 99 > 3 ||
            plan.days.lastIndex - (ntDays.lastOrNull() ?: -99) > 3
        ) errors += "Distribución del Nuevo Testamento"
    }

    private fun newTestamentDays(plan: ReadingPlan): List<Int> = plan.days.mapIndexedNotNull { index, day ->
        index.takeIf {
            day.readings.any { reading ->
                !reading.isGospel && BibleData.bookById.getValue(reading.book).testament == Testament.NEW
            }
        }
    }

    data class ValidationResult(
        val days: Int,
        val readings: Int,
        val uniqueNonGospelChapters: Int,
        val daysWithNewTestament: Int,
    )

    private data class MutableDay(val date: LocalDate, val readings: MutableList<Reading> = mutableListOf())

    private fun LocalDate.isWeekend() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private fun weighted(source: List<Reading>, theme: String, random: Random): List<Reading> =
        source.shuffled(random).sortedByDescending { if (theme in BibleData.tags(it.book, it.chapter)) 1 else 0 }

    private fun distinctWeighted(
        source: List<Reading>,
        theme: String,
        random: Random,
        previous: Set<String>,
    ): List<Reading> {
        repeat(20) {
            val candidate = weighted(source, theme, random)
            if (signature(candidate) !in previous) return candidate
        }
        return weighted(source, theme, random).let { it.drop(1) + it.first() }
    }

    private fun signature(readings: List<Reading>) = readings.joinToString(",") { it.chapter.toString() }

    private fun placeReservedPsalm(order: List<Reading>, cycle: Int, index: Int, slots: List<MutableDay>) {
        slots[index].readings += order.first { it.chapter == 119 }.copy(cycle = cycle)
    }

    private fun reservedWeekend(dates: List<LocalDate>, start: Int, end: Int): Int {
        val middle = (start + end - 1) / 2
        return (start until end).filter { dates[it].isWeekend() }
            .minByOrNull { kotlin.math.abs(it - middle) }
            ?: error("El recorrido no contiene fines de semana.")
    }

    private fun reduceCapacityEvenly(
        targets: MutableList<Int>,
        dates: List<LocalDate>,
        totalReadings: Int,
        protected: Set<Int>,
    ) {
        var reductions = targets.sum() - totalReadings
        require(reductions >= 0) { "El ritmo elegido no ofrece suficiente capacidad." }
        val candidates = dates.indices
            .filter { it !in protected && targets[it] > 2 }
            .sortedBy { index -> (index * 997) % dates.size }
        for (index in candidates) {
            if (reductions == 0) break
            targets[index]--
            reductions--
        }
        require(reductions == 0) { "No fue posible equilibrar la carga diaria." }
    }

    private fun spread(
        readings: List<Reading>,
        days: List<MutableDay>,
        targets: List<Int>,
        start: Int,
        end: Int,
    ) {
        val length = end - start
        readings.forEachIndexed { index, reading ->
            val ideal = start + floor((index + .45) * length / readings.size).toInt().coerceIn(0, length - 1)
            val available = (start until end).filter { days[it].readings.size < targets[it] }
            val weekend = available.filter { days[it].date.isWeekend() }
            val candidates = if (BibleData.isLongChapter(reading.book, reading.chapter) && weekend.isNotEmpty()) {
                weekend
            } else {
                available
            }
            val selected = candidates.minWithOrNull(compareBy<Int> {
                kotlin.math.abs(it - ideal) * 10 +
                    days[it].readings.count { current -> current.book == reading.book } * 100 +
                    days[it].readings.count { current -> BibleData.isLongChapter(current.book, current.chapter) } * 500
            }.thenBy { days[it].readings.size }) ?: error("No hay capacidad para ${reading.key}")
            days[selected].readings += reading
        }
    }

    private fun spreadOrdered(
        readings: List<Reading>,
        days: List<MutableDay>,
        targets: List<Int>,
        start: Int,
        end: Int,
    ) {
        val length = end - start
        var previous = start - 1
        readings.forEachIndexed { index, reading ->
            val remaining = readings.lastIndex - index
            val lower = previous + 1
            val upper = end - remaining - 1
            val ideal = (start + floor((index + .5) * length / readings.size).toInt()).coerceIn(lower, upper)
            val available = (lower..upper).filter { days[it].readings.size < targets[it] }
            val weekend = available.filter { days[it].date.isWeekend() }
            val candidates = if (BibleData.isLongChapter(reading.book, reading.chapter) && weekend.isNotEmpty()) {
                weekend
            } else {
                available
            }
            val selected = candidates.minWithOrNull(compareBy<Int> { kotlin.math.abs(it - ideal) }
                .thenBy { days[it].readings.size }) ?: error("No hay espacio ordenado para ${reading.key}")
            days[selected].readings += reading
            previous = selected
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

    internal fun chronologicalGospels(gospels: List<Reading>): List<Reading> {
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
}
