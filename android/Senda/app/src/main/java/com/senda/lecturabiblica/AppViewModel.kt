package com.senda.lecturabiblica

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senda.lecturabiblica.data.AppUpdate
import com.senda.lecturabiblica.data.PlanBackupCodec
import com.senda.lecturabiblica.data.PlanBackupData
import com.senda.lecturabiblica.data.PlanBackupFiles
import com.senda.lecturabiblica.data.PlanStore
import com.senda.lecturabiblica.data.SavedPlanBackup
import com.senda.lecturabiblica.data.UpdateRepository
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
    val savingBackup: Boolean = false,
    val savedBackup: SavedPlanBackup? = null,
    val pendingRestore: Uri? = null,
    val restoringBackup: Boolean = false,
    val notice: String? = null,
    val availableUpdate: AppUpdate? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PlanStore(application)
    private val updates = UpdateRepository(application)
    private val year = LocalDate.now().year
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val plan = store.loadPlan(year)
            val validPlan = plan?.takeIf { runCatching { PlanGenerator.validate(it) }.isSuccess }
            mutableState.update { current ->
                AppUiState(
                    plan = validPlan, completed = if (validPlan == null) emptySet() else store.completed(year),
                    loading = false, themeMode = store.themeMode(), accent = store.accent(),
                    bibleVersion = store.bibleVersion().takeIf { saved -> bibleTranslations.any { it.id == saved } } ?: "RVC",
                    pendingRestore = current.pendingRestore,
                )
            }
            checkForUpdates()
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

    fun savePlanBackup() {
        val snapshot = mutableState.value
        val plan = snapshot.plan ?: return
        if (snapshot.savingBackup) return
        mutableState.update { it.copy(savingBackup = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = PlanBackupCodec.encode(PlanBackupData(plan, snapshot.completed, snapshot.bibleVersion))
                PlanBackupFiles.saveToDownloads(getApplication(), bytes, plan.year)
            }.onSuccess { saved ->
                mutableState.update { it.copy(savingBackup = false, savedBackup = saved) }
            }.onFailure { cause ->
                mutableState.update {
                    it.copy(savingBackup = false, error = cause.message ?: "No se pudo guardar el plan en Descargas.")
                }
            }
        }
    }

    fun dismissSavedBackup() = mutableState.update { it.copy(savedBackup = null) }

    fun requestRestore(uri: Uri) {
        mutableState.update { it.copy(pendingRestore = uri, error = null) }
    }

    fun cancelRestore() = mutableState.update { it.copy(pendingRestore = null) }

    fun restorePendingBackup() {
        val uri = mutableState.value.pendingRestore ?: return
        if (mutableState.value.restoringBackup) return
        mutableState.update { it.copy(restoringBackup = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val input = checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)) {
                    "No se pudo abrir el archivo seleccionado."
                }
                val backup = input.use(PlanBackupCodec::decode)
                require(backup.plan.year == year) {
                    "El respaldo corresponde al año ${backup.plan.year}; esta instalación utiliza el plan de $year."
                }
                PlanGenerator.validate(backup.plan)
                require(bibleTranslations.any { it.id == backup.bibleVersion }) {
                    "El respaldo contiene una traducción bíblica no compatible."
                }
                val validKeys = backup.plan.days.flatMap { day ->
                    day.readings.indices.map { index -> completionKey(day.date, index) }
                }.toSet()
                require(backup.completed.all(validKeys::contains)) { "El progreso del respaldo está dañado." }
                store.restore(backup)
                backup
            }.onSuccess { backup ->
                mutableState.update {
                    it.copy(
                        plan = backup.plan,
                        completed = backup.completed,
                        bibleVersion = backup.bibleVersion,
                        pendingRestore = null,
                        restoringBackup = false,
                        notice = "Plan y progreso restaurados correctamente.",
                    )
                }
            }.onFailure { cause ->
                mutableState.update {
                    it.copy(
                        pendingRestore = null,
                        restoringBackup = false,
                        error = cause.message ?: "No se pudo restaurar el respaldo.",
                    )
                }
            }
        }
    }

    fun dismissNotice() = mutableState.update { it.copy(notice = null) }

    fun ignoreUpdate() {
        store.snoozeUpdates(LocalDate.now().plusDays(1).toEpochDay())
        mutableState.update { it.copy(availableUpdate = null) }
    }

    fun postponeUpdate() {
        store.snoozeUpdates(LocalDate.now().plusDays(5).toEpochDay())
        mutableState.update { it.copy(availableUpdate = null) }
    }

    fun downloadUpdate() {
        val update = mutableState.value.availableUpdate ?: return
        runCatching { updates.enqueueDownload(update) }
            .onSuccess {
                store.snoozeUpdates(LocalDate.now().plusDays(1).toEpochDay())
                mutableState.update {
                    it.copy(
                        availableUpdate = null,
                        notice = "La actualización se está descargando en la carpeta Descargas.",
                    )
                }
            }
            .onFailure { cause ->
                mutableState.update {
                    it.copy(error = cause.message ?: "No se pudo iniciar la descarga de la actualización.")
                }
            }
    }

    fun showError(message: String) = mutableState.update { it.copy(error = message) }

    fun clearError() = mutableState.update { it.copy(error = null) }

    private fun checkForUpdates() {
        val day = LocalDate.now().toEpochDay()
        if (!store.shouldCheckForUpdate(day)) return
        store.markUpdateChecked(day)
        runCatching(updates::findUpdate).getOrNull()?.let { update ->
            mutableState.update { it.copy(availableUpdate = update) }
        }
    }

    private fun completionKey(date: LocalDate, index: Int) = "$date#$index"
}
