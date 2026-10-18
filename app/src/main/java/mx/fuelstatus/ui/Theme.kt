package mx.fuelstatus.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import mx.fuelstatus.Preferences
import mx.fuelstatus.domain.ThemePalette

private val Light = paletteColorScheme(ThemePalette.Green, false).copy(
    primary = Color(0xFF43672F), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EDB5), onPrimaryContainer = Color(0xFF102006),
    secondaryContainer = Color(0xFFDEE7D1), onSecondaryContainer = Color(0xFF182212),
    tertiaryContainer = Color(0xFFC2EBE3), onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF7F9F2), surface = Color(0xFFF7F9F2),
    surfaceContainer = Color(0xFFEBEEE3), surfaceContainerLow = Color(0xFFF1F4E9),
)
private val Dark = paletteColorScheme(ThemePalette.Green, true).copy(
    primary = Color(0xFFB4D697), onPrimary = Color(0xFF203611),
    primaryContainer = Color(0xFF304F20), onPrimaryContainer = Color(0xFFD3EDB5),
    secondaryContainer = Color(0xFF3F4A35), tertiaryContainer = Color(0xFF204E47),
    background = Color(0xFF11150E), surface = Color(0xFF11150E),
)

internal fun fixedColorScheme(palette: ThemePalette, dark: Boolean): ColorScheme =
    if (palette == ThemePalette.Green) { if (dark) Dark else Light } else paletteColorScheme(palette, dark)

@Composable
internal fun themeIsDark(preferences: Preferences): Boolean = when (preferences.theme) {
    "dark" -> true; "light" -> false; else -> isSystemInDarkTheme()
}

@Composable
internal fun androidColorScheme(dark: Boolean): ColorScheme {
    // Observe resource configuration changes, including a new system color overlay.
    LocalConfiguration.current
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FuelTheme(preferences: Preferences, content: @Composable () -> Unit) {
    val dark = themeIsDark(preferences)
    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        context.activity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val colors = if (preferences.dynamicColor) {
        androidColorScheme(dark)
    } else fixedColorScheme(ThemePalette.fromCode(preferences.colorPalette), dark)
    MaterialExpressiveTheme(colorScheme = colors, content = content)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
