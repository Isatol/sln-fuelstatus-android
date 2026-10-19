package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.backup.AppearanceStore
import mx.fuelstatus.domain.ThemePalette
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ThemePaletteTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paletteSelectionAppliesImmediatelyAndAndroidCanBeSelectedAgain() {
        var preferences by mutableStateOf(Preferences("light"))
        var busy by mutableStateOf(false)
        var scale by mutableFloatStateOf(1f)
        lateinit var actual: ColorScheme
        lateinit var android: ColorScheme
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                val context = LocalContext.current
                android = if (preferences.theme == "dark") dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                FuelTheme(preferences) {
                    actual = MaterialTheme.colorScheme
                    Surface { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        SettingsScreen(preferences, busy, { preferences = preferences.copy(theme = it) },
                            { preferences = preferences.copy(dynamicColor = it) }, {}, {}, {}, {}, {},
                            onColorPalette = { preferences = preferences.copy(dynamicColor = false, colorPalette = it) })
                    } }
                }
            }
        }
        palette("Android").assertIsSelected()
        compose.runOnIdle { assertScheme(android, actual) }
        capture("android-claro")
        palette("Azul").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {
            assertFalse(preferences.dynamicColor)
            assertScheme(fixedColorScheme(ThemePalette.Blue, false), actual)
        }
        capture("azul-claro")
        compose.onNodeWithText("Oscuro").performClick()
        compose.runOnIdle { assertScheme(fixedColorScheme(ThemePalette.Blue, true), actual) }
        compose.runOnIdle { scale = 1.3f }
        palette("Gris").performScrollTo().performClick().assertIsSelected()
        capture("gris-oscuro-texto130")
        palette("Android").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertScheme(android, actual); busy = true }
        compose.onNodeWithText("Claro").assertIsNotEnabled()
        palette("Android").assertIsNotEnabled()
        capture("android-oscuro-texto130")
    }

    @Test fun appearancePersistsPaletteAndMigratesExistingPreferencesWithoutChangingOtherSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-theme-palette"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            prefs.edit().clear().putString("theme", "dark").putBoolean("dynamic", false).putString("vehicle", "car").commit()
            val store = AppearanceStore(context, name)
            assertEquals(Preferences("dark", false, "car"), store.state.value)
            store.update { it.copy(colorPalette = "rose") }
            assertEquals(Preferences("dark", false, "car", "rose"), AppearanceStore(context, name).state.value)
            store.update { it.copy(dynamicColor = true) }
            assertEquals(Preferences("dark", true, "car", "rose"), AppearanceStore(context, name).state.value)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun everyFixedPaletteHasReadableTextInLightAndDarkModes() {
        fun contrast(a: Color, b: Color): Float {
            val x = a.luminance(); val y = b.luminance()
            return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
        }
        for (palette in ThemePalette.entries) for (dark in listOf(false, true)) {
            val c = fixedColorScheme(palette, dark)
            val pairs = listOf(c.primary to c.onPrimary, c.primaryContainer to c.onPrimaryContainer,
                c.secondary to c.onSecondary, c.secondaryContainer to c.onSecondaryContainer,
                c.tertiary to c.onTertiary, c.tertiaryContainer to c.onTertiaryContainer,
                c.background to c.onBackground, c.surface to c.onSurface,
                c.surfaceContainer to c.onSurface, c.surfaceContainerLow to c.onSurface,
                c.surfaceVariant to c.onSurfaceVariant, c.error to c.onError)
            pairs.forEach { (background, text) ->
                assertTrue("$palette dark=$dark contrast=${contrast(background, text)}", contrast(background, text) >= 4.5f)
            }
        }
    }

    private fun palette(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("app-color-picker")))

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-palette-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertScheme(expected: ColorScheme, actual: ColorScheme) {
        assertEquals(expected.primary, actual.primary)
        assertEquals(expected.primaryContainer, actual.primaryContainer)
        assertEquals(expected.secondaryContainer, actual.secondaryContainer)
        assertEquals(expected.tertiaryContainer, actual.tertiaryContainer)
        assertEquals(expected.surface, actual.surface)
        assertEquals(expected.onSurface, actual.onSurface)
    }
}
