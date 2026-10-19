package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.data.*
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VehicleEditorUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun defaultFuelSelectionKeepsVehicleFieldsAndSupportsLargeDarkText() {
        var saved: Vehicle? = null
        var dark by mutableStateOf(false)
        var scale by mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                FuelTheme(Preferences(if (dark) "dark" else "light", false)) {
                    VehicleEditor(null, emptyList(), false, {}, { saved = it })
                }
            }
        }
        compose.onNodeWithText("Combustible predeterminado").assertIsDisplayed()
        capture("claro")
        compose.onNodeWithText("Nombre / modelo").performTextInput("Auto de prueba")
        compose.onNodeWithText("Tipo (Hatchback, Sedán…)").performTextInput("Sedán")
        compose.onNodeWithText("Año").performTextInput("2026")
        compose.onNodeWithText("Magna · 87 octanos").performScrollTo().performClick()
        compose.onNodeWithText("Premium · 91 octanos").performClick()
        compose.runOnIdle { dark = true; scale = 1.3f }
        compose.onNodeWithText("Combustible predeterminado").performScrollTo().assertIsDisplayed()
        capture("oscuro-texto130")
        compose.onNodeWithText("Guardar vehículo").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Auto de prueba", saved!!.name)
            assertEquals("Sedán", saved!!.type)
            assertEquals(2026, saved!!.year)
            assertEquals(FuelType.Premium.code, saved!!.fuelType)
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-vehiculo-iconos-$name.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
