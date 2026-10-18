package mx.fuelstatus

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import mx.fuelstatus.domain.IconRingColor

/** Only launcher aliases change; MainActivity remains enabled for existing explicit shortcuts. */
class LauncherIcons(context: Context) {
    private val manager = context.packageManager
    private val packageName = context.packageName

    fun component(color: IconRingColor) = ComponentName(packageName, "$packageName.launcher.${color.name}Icon")

    @Synchronized fun apply(code: String) {
        val selected = IconRingColor.fromCode(code)
        val previous = IconRingColor.entries.associateWith { manager.getComponentEnabledSetting(component(it)) }
        fun enabled(color: IconRingColor) = when (previous.getValue(color)) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> color == IconRingColor.Android
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> false
        }
        val changes = IconRingColor.entries.filter { enabled(it) != (it == selected) }
        if (changes.isEmpty()) return
        fun state(color: IconRingColor) = if (color == selected) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                manager.setComponentEnabledSettings(changes.map {
                    PackageManager.ComponentEnabledSetting(component(it), state(it), PackageManager.DONT_KILL_APP)
                })
            } else {
                // On Android 12/12L, always enable the destination before disabling the old icon.
                (listOf(selected) + changes.filter { it != selected }).distinct().forEach {
                    if (it in changes) manager.setComponentEnabledSetting(component(it), state(it), PackageManager.DONT_KILL_APP)
                }
            }
        } catch (failure: Exception) {
            // Restore prior component states if an older platform fails partway through a switch.
            if (Build.VERSION.SDK_INT < 33) {
                changes.sortedBy { if (enabled(it)) 0 else 1 }.forEach {
                    runCatching { manager.setComponentEnabledSetting(component(it), previous.getValue(it), PackageManager.DONT_KILL_APP) }
                        .onFailure(failure::addSuppressed)
                }
            }
            throw IllegalStateException("No se pudo cambiar el color del aro del icono. Inténtalo de nuevo.", failure)
        }
    }
}
