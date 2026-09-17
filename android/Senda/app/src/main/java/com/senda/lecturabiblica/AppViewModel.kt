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
import com.senda.lecturabiblica.model.ReadingPace
import com.senda.lecturabiblica.model.ReadingPlan
import com.senda.lecturabiblica.model.endDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

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
    val currentDate: LocalDate = LocalDate.now(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PlanStore(application)
    private val updates = UpdateRepository(application)
    private val updateCheckRunning = AtomicBoolean(false)
    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val plan = store.loadActivePlan(today.year)
            val validPlan = plan?.takeIf { runCatching { PlanGenerator.validate(it) }.isSuccess }
                ?.takeIf { today <= it.endDate }
            if (plan != null && validPlan == null && today > plan.endDate) store.clearActivePlan()
            mutableState.update { current ->
                AppUiState(
                    plan = validPlan, completed = if (validPlan == null) emptySet() else store.completed(),
                    loading = false, themeMode = store.themeMode(), accent = store.accent(),
                    bibleVersion = store.bibleVersion().takeIf { saved -> bibleTranslations.any { it.id == saved } } ?: "RVC",
                    pendingRestore = current.pendingRestore,
                    currentDate = today,
                )
            }
            checkForUpdates()
        }
    }

    fun generate(
        theme: String,
        includeDeuterocanon: Boolean,
        pace: ReadingPace,
        replace: Boolean,
        bibleVersion: String,
    ) {
        if (mutableState.value.generating) return
        mutableState.update { it.copy(generating = true, error = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    PlanGenerator.generate(LocalDate.now(), theme, includeDeuterocanon, pace)
                }
            }.onSuccess { plan ->
                withContext(Dispatchers.IO) {
                    if (replace) store.replacePlan(plan) else store.savePlan(plan)
                    store.saveBibleVersion(bibleVersion)
                }
                mutableState.update {
                    it.copy(
                        plan = plan,
                        completed = emptySet(),
                        generating = false,
                        bibleVersion = bibleVersion,
                        currentDate = plan.startDate,
                    )
                }
            }.onFailure { cause ->
                mutableState.update { it.copy(generating = false, error = cause.message ?: "No se pudo generar el plan.") }
            }
        }
    }

    fun markReadingComplete(date: LocalDate, index: Int) {
        if (mutableState.value.plan == null) return
        val key = completionKey(date, index)
        if (key in mutableState.value.completed) return
        val updated = mutableState.value.completed + key
        mutableState.update { it.copy(completed = updated) }
        store.saveCompleted(updated)
    }

    fun markReadingForReread(date: LocalDate, index: Int) {
        if (mutableState.value.plan == null) return
        val key = completionKey(date, index)
        if (key !in mutableState.value.completed) return
        val updated = mutableState.value.completed - key
        mutableState.update { it.copy(completed = updated) }
        store.saveCompleted(updated)
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
                PlanBackupFiles.saveToDownloads(getApplication(), bytes, plan)
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
                PlanGenerator.validate(backup.plan)
                require(LocalDate.now() <= backup.plan.endDate) {
                    "Este plan finalizó el ${backup.plan.endDate} y ya no puede restaurarse como plan activo."
                }
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

    fun onAppForeground() {
        val today = LocalDate.now()
        val snapshot = mutableState.value
        if (snapshot.loading) return
        if (today == snapshot.currentDate) {
            viewModelScope.launch(Dispatchers.IO) { checkForUpdates() }
            return
        }
        val expired = snapshot.plan?.let { today > it.endDate } == true
        mutableState.update {
            it.copy(
                currentDate = today,
                plan = if (expired) null else it.plan,
                completed = if (expired) emptySet() else it.completed,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (expired) store.clearActivePlan()
            checkForUpdates()
        }
    }

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
        if (!updateCheckRunning.compareAndSet(false, true)) return
        try {
            runCatching(updates::findUpdate).onSuccess { update ->
                store.markUpdateChecked(day)
                if (update != null) mutableState.update { it.copy(availableUpdate = update) }
            }
        } finally {
            updateCheckRunning.set(false)
        }
    }

    private fun completionKey(date: LocalDate, index: Int) = "$date#$index"
}
