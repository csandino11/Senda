package com.senda.lecturabiblica

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senda.lecturabiblica.data.PlanStore
import com.senda.lecturabiblica.domain.PlanGenerator
import com.senda.lecturabiblica.domain.bibleTranslations
import com.senda.lecturabiblica.model.DayStatus
import com.senda.lecturabiblica.model.ProgressStats
import com.senda.lecturabiblica.model.ReadingPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class AppUiState(
    val plan: ReadingPlan? = null,
    val completed: Set<String> = emptySet(),
    val loading: Boolean = true,
    val generating: Boolean = false,
    val error: String? = null,
    val themeMode: String = "system",
    val accent: String = "bosque",
    val bibleVersion: String = "RVC",
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PlanStore(application)
    private val year = LocalDate.now().year
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val plan = store.loadPlan(year)
            val validPlan = plan?.takeIf { runCatching { PlanGenerator.validate(it) }.isSuccess }
            mutableState.value = AppUiState(
                plan = validPlan, completed = if (validPlan == null) emptySet() else store.completed(year),
                loading = false, themeMode = store.themeMode(), accent = store.accent(),
                bibleVersion = store.bibleVersion().takeIf { saved -> bibleTranslations.any { it.id == saved } } ?: "RVC",
            )
        }
    }

    fun generate(theme: String, includeDeuterocanon: Boolean, replace: Boolean, bibleVersion: String) {
        if (mutableState.value.generating) return
        mutableState.update { it.copy(generating = true, error = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) { PlanGenerator.generate(year, theme, includeDeuterocanon) }
            }.onSuccess { plan ->
                withContext(Dispatchers.IO) {
                    if (replace) store.replacePlan(plan) else store.savePlan(plan)
                    store.saveBibleVersion(bibleVersion)
                }
                mutableState.update { it.copy(plan = plan, completed = emptySet(), generating = false, bibleVersion = bibleVersion) }
            }.onFailure { cause ->
                mutableState.update { it.copy(generating = false, error = cause.message ?: "No se pudo generar el plan.") }
            }
        }
    }

    fun toggleReading(date: LocalDate, index: Int) {
        val plan = mutableState.value.plan ?: return
        val key = completionKey(date, index)
        val updated = mutableState.value.completed.toMutableSet().apply {
            if (!add(key)) remove(key)
        }.toSet()
        mutableState.update { it.copy(completed = updated) }
        store.saveCompleted(plan.year, updated)
    }

    fun isComplete(date: LocalDate, index: Int): Boolean = completionKey(date, index) in mutableState.value.completed

    fun dayStatus(date: LocalDate): DayStatus {
        val day = mutableState.value.plan?.days?.firstOrNull { it.date == date } ?: return DayStatus.NONE
        val count = day.readings.indices.count { completionKey(date, it) in mutableState.value.completed }
        return when { count == 0 -> DayStatus.NONE; count == day.readings.size -> DayStatus.COMPLETE; else -> DayStatus.PARTIAL }
    }

    fun stats(): ProgressStats {
        val plan = mutableState.value.plan ?: return ProgressStats(0, 0, 0, 0, 0)
        var completeDays = 0; var partialDays = 0; var unreadDays = 0; var read = 0
        plan.days.forEach { day ->
            val count = day.readings.indices.count { completionKey(day.date, it) in mutableState.value.completed }
            read += count
            when { count == 0 -> unreadDays++; count == day.readings.size -> completeDays++; else -> partialDays++ }
        }
        return ProgressStats(read, plan.days.sumOf { it.readings.size }, completeDays, partialDays, unreadDays)
    }

    fun setAppearance(mode: String = mutableState.value.themeMode, accent: String = mutableState.value.accent) {
        store.saveAppearance(mode, accent)
        mutableState.update { it.copy(themeMode = mode, accent = accent) }
    }

    fun setBibleVersion(version: String) {
        if (bibleTranslations.none { it.id == version }) return
        store.saveBibleVersion(version)
        mutableState.update { it.copy(bibleVersion = version) }
    }

    fun clearError() = mutableState.update { it.copy(error = null) }

    private fun completionKey(date: LocalDate, index: Int) = "$date#$index"
}
