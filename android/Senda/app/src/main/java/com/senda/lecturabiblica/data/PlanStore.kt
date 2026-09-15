package com.senda.lecturabiblica.data

import android.content.Context
import androidx.core.content.edit
import com.senda.lecturabiblica.model.DayPlan
import com.senda.lecturabiblica.model.Reading
import com.senda.lecturabiblica.model.ReadingPlan
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PlanStore(context: Context) {
    private val preferences = context.getSharedPreferences("senda_local", Context.MODE_PRIVATE)

    fun loadPlan(year: Int): ReadingPlan? = preferences.getString("plan_$year", null)?.let { encoded ->
        runCatching { decodePlan(JSONObject(encoded)) }.getOrNull()
    }

    fun savePlan(plan: ReadingPlan) {
        preferences.edit { putString("plan_${plan.year}", encodePlan(plan).toString()) }
    }

    fun completed(year: Int): Set<String> = preferences.getStringSet("completed_$year", emptySet())?.toSet().orEmpty()

    fun saveCompleted(year: Int, completed: Set<String>) {
        preferences.edit { putStringSet("completed_$year", completed.toSet()) }
    }

    fun replacePlan(plan: ReadingPlan) {
        preferences.edit {
            putString("plan_${plan.year}", encodePlan(plan).toString())
            putStringSet("completed_${plan.year}", emptySet())
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

    private fun encodePlan(plan: ReadingPlan) = JSONObject().apply {
        put("version", plan.version); put("id", plan.id); put("year", plan.year); put("theme", plan.theme)
        put("includeDeuterocanon", plan.includeDeuterocanon); put("seed", plan.seed); put("createdAt", plan.createdAt)
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

    private fun decodePlan(json: JSONObject): ReadingPlan = ReadingPlan(
        version = json.getInt("version"), id = json.getString("id"), year = json.getInt("year"),
        theme = json.getString("theme"), includeDeuterocanon = json.getBoolean("includeDeuterocanon"),
        seed = json.getLong("seed"), createdAt = json.getString("createdAt"),
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

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
}
