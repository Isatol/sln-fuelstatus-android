package mx.fuelstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.fuelstatus.Preferences
import mx.fuelstatus.domain.ThemePalette

@Composable
internal fun ThemeColorPicker(preferences: Preferences, enabled: Boolean,
    onDynamic: (Boolean) -> Unit, onPalette: (String) -> Unit) {
    val dark = themeIsDark(preferences)
    val androidColors = androidColorScheme(dark)
    Text("Colores de la app", fontWeight = FontWeight.Bold)
    Text("Material You usa la paleta elegida en Android. También puedes elegir una paleta de FuelStatus.",
        style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().testTag("app-color-picker"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaletteOption("Android", androidColors, preferences.dynamicColor, enabled) { onDynamic(true) }
        ThemePalette.entries.forEach { palette ->
            PaletteOption(palette.label, fixedColorScheme(palette, dark),
                !preferences.dynamicColor && ThemePalette.fromCode(preferences.colorPalette) == palette, enabled) {
                onPalette(palette.code)
            }
        }
    }
    Text(if (preferences.dynamicColor) "Colores de Android · se actualizan con la paleta del sistema."
        else "Paleta ${ThemePalette.fromCode(preferences.colorPalette).label} · solo para FuelStatus.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaletteOption(label: String, colors: ColorScheme, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clip(MaterialTheme.shapes.medium)
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(72.dp).border(3.dp,
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).padding(7.dp)) {
            Column(Modifier.fillMaxSize().clip(CircleShape)) {
                Box(Modifier.fillMaxWidth().weight(1f).background(colors.primary))
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Box(Modifier.fillMaxHeight().weight(1f).background(colors.secondaryContainer))
                    Box(Modifier.fillMaxHeight().weight(1f).background(colors.tertiaryContainer))
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
