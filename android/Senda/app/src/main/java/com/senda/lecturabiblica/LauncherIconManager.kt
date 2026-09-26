package com.senda.lecturabiblica

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object LauncherIconManager {
    private val aliases = mapOf(
        "bosque" to "LauncherBosque",
        "cielo" to "LauncherCielo",
        "lumbre" to "LauncherLumbre",
        "anil" to "LauncherAnil",
        "mango" to "LauncherMango",
        "fucsia" to "LauncherFucsia",
    )

    fun apply(context: Context, accent: String) {
        val chosen = aliases[accent] ?: aliases.getValue("cielo")
        val manager = context.packageManager
        val prefix = "com.senda.lecturabiblica"
        aliases.values.forEach { alias ->
            val component = ComponentName(context.packageName, "$prefix.$alias")
            val state = if (alias == chosen) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (manager.getComponentEnabledSetting(component) != state) {
                manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
