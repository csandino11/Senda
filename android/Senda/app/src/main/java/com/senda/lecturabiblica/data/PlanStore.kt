package com.senda.lecturabiblica.data

import android.content.Context
import androidx.core.content.edit
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPace
import com.senda.lecturabiblica.model.ReadingPlan
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PlanStore(context: Context) {
    private val preferences = context.getSharedPreferences("senda_local", Context.MODE_PRIVATE)

    fun loadActivePlan(currentYear: Int): ReadingPlan? {
        preferences.getString("active_plan", null)?.let { encoded ->
            runCatching { PlanJson.decode(JSONObject(encoded)) }.getOrNull()?.let { return it }
        }
        val legacy = preferences.getString("plan_$currentYear", null)?.let { encoded ->
            runCatching { PlanJson.decode(JSONObject(encoded)) }.getOrNull()
        } ?: return null
        val legacyCompleted = preferences.getStringSet("completed_$currentYear", emptySet())?.toSet().orEmpty()
        preferences.edit {
            putString("active_plan", PlanJson.encode(legacy).toString())
            putStringSet("active_completed", legacyCompleted)
            remove("plan_$currentYear")
            remove("completed_$currentYear")
        }
        return legacy
    }

    fun savePlan(plan: ReadingPlan) {
        preferences.edit {
            putString("active_plan", PlanJson.encode(plan).toString())
            putStringSet("active_completed", emptySet())
            remove("plan_${plan.year}")
            remove("completed_${plan.year}")
        }
    }

    fun completed(): Set<String> = preferences.getStringSet("active_completed", emptySet())?.toSet().orEmpty()

    fun saveCompleted(completed: Set<String>) {
        preferences.edit { putStringSet("active_completed", completed.toSet()) }
    }

    fun replacePlan(plan: ReadingPlan) {
        preferences.edit {
            putString("active_plan", PlanJson.encode(plan).toString())
            putStringSet("active_completed", emptySet())
            remove("plan_${plan.year}")
            remove("completed_${plan.year}")
        }
    }

    fun clearActivePlan() {
        preferences.edit { remove("active_plan"); remove("active_completed") }
    }

    fun restore(backup: PlanBackupData) {
        preferences.edit {
            putString("active_plan", PlanJson.encode(backup.plan).toString())
            putStringSet("active_completed", backup.completed)
            putString("bible_version", backup.bibleVersion)
            remove("plan_${backup.plan.year}")
            remove("completed_${backup.plan.year}")
        }
    }

    fun themeMode(): String = preferences.getString("theme_mode", "system") ?: "system"
    fun accent(): String = when (val saved = preferences.getString("accent", "bosque") ?: "bosque") {
        "turquesa" -> "fucsia"
        else -> saved
    }
    fun saveAppearance(mode: String, accent: String) {
        preferences.edit { putString("theme_mode", mode); putString("accent", accent) }
    }

    fun bibleVersion(): String = preferences.getString("bible_version", "RVC") ?: "RVC"
    fun saveBibleVersion(version: String) {
        preferences.edit { putString("bible_version", version) }
    }

    fun shouldCheckForUpdate(day: Long): Boolean {
        val lastCheck = preferences.getLong("last_update_check_day", Long.MIN_VALUE)
        val snoozeUntil = preferences.getLong("update_snooze_until_day", Long.MIN_VALUE)
        return lastCheck != day && day >= snoozeUntil
    }

    fun markUpdateChecked(day: Long) {
        preferences.edit { putLong("last_update_check_day", day) }
    }

    fun snoozeUpdates(untilDay: Long) {
        preferences.edit { putLong("update_snooze_until_day", untilDay) }
    }
}

internal object PlanJson {
    fun encode(plan: ReadingPlan) = JSONObject().apply {
        put("version", plan.version); put("id", plan.id); put("year", plan.year); put("theme", plan.theme)
        put("includeDeuterocanon", plan.includeDeuterocanon); put("seed", plan.seed); put("createdAt", plan.createdAt)
        put("startDate", plan.startDate.toString()); put("pace", plan.pace.id)
        put("days", JSONArray().apply {
            plan.days.forEach { day -> put(JSONObject().apply {
                put("date", day.date.toString()); put("focus", day.focus); put("connection", day.connection)
                put("readings", JSONArray().apply {
                    day.readings.forEach { reading -> put(JSONObject().apply {
                        put("book", reading.book); put("chapter", reading.chapter); put("cycle", reading.cycle)
                        reading.gospelOrder?.let { put("gospelOrder", it) }
                    }) }
                })
            }) }
        })
    }

    fun decode(json: JSONObject): ReadingPlan {
        val year = json.getInt("year")
        return ReadingPlan(
            version = json.getInt("version"), id = json.getString("id"), year = year,
            theme = json.getString("theme"), includeDeuterocanon = json.getBoolean("includeDeuterocanon"),
            seed = json.getLong("seed"), createdAt = json.getString("createdAt"),
            startDate = if (json.has("startDate")) {
                LocalDate.parse(json.getString("startDate"))
            } else {
                LocalDate.of(year, 1, 1)
            },
            pace = ReadingPace.fromId(if (json.has("pace")) json.getString("pace") else null),
            days = json.getJSONArray("days").objects().map { day ->
                DayPlan(
                    date = LocalDate.parse(day.getString("date")), focus = day.getString("focus"),
                    connection = day.getString("connection"),
                    readings = day.getJSONArray("readings").objects().map { reading ->
                        Reading(
                            book = reading.getString("book"), chapter = reading.getInt("chapter"),
                            cycle = reading.getInt("cycle"),
                            gospelOrder = if (reading.has("gospelOrder")) reading.getInt("gospelOrder") else null,
                        )
                    },
                )
            },
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
}
