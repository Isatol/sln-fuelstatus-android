package mx.fuelstatus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R
import mx.fuelstatus.domain.IconRingColor

@Composable
internal fun IconRingCard(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTitle(R.drawable.ic_palette, "Color del aro del icono")
            Text("Personaliza el icono del cajón de aplicaciones.", style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().testTag("icon-ring-picker"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconRingColor.entries.forEach { option ->
                    val active = option == IconRingColor.fromCode(selected)
                    Column(Modifier.width(96.dp).clip(MaterialTheme.shapes.medium)
                        .selectable(active, enabled, Role.RadioButton) { onSelect(option.code) }
                        .semantics { contentDescription = "Color del aro del icono: ${option.label}" }
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(76.dp).border(3.dp,
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).padding(7.dp)) {
                            IconRingPreview(option)
                        }
                        Text(option.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Text(if (selected == IconRingColor.Android.code) "Android · el aro sigue la paleta del sistema."
                else "${IconRingColor.fromCode(selected).label} · color del aro independiente del tema de la app.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("En el escritorio, los iconos temáticos conservan los colores que aplica Android. El launcher puede tardar en mostrar el cambio o requerir que agregues de nuevo el acceso al escritorio.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun IconRingPreview(option: IconRingColor, modifier: Modifier = Modifier) {
    // Observe system palette overlay changes independently of the app's selected theme.
    LocalConfiguration.current
    val foreground = when (option) {
        IconRingColor.Android -> R.drawable.ic_fuel_ring_android
        IconRingColor.Green -> R.drawable.ic_fuel
        IconRingColor.Blue -> R.drawable.ic_fuel_ring_blue
        IconRingColor.Violet -> R.drawable.ic_fuel_ring_violet
        IconRingColor.Rose -> R.drawable.ic_fuel_ring_rose
        IconRingColor.Orange -> R.drawable.ic_fuel_ring_orange
        IconRingColor.Gray -> R.drawable.ic_fuel_ring_gray
    }
    BoxWithConstraints(modifier.fillMaxSize().clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        // Render the exact 108 dp launcher foreground through its central 72 dp viewport.
        Image(painterResource(foreground), contentDescription = null, modifier = Modifier.requiredSize(maxWidth * 1.5f))
    }
}
