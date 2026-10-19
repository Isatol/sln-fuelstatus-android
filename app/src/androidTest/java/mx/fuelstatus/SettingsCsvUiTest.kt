package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
class SettingsCsvUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsRoutesFuelStatusCsvActionsAndSupportsLargeDarkText() {
        var native = 0
        var exports = 0
        var theme by mutableStateOf("light")
        var scale by mutableFloatStateOf(1f)
        var busy by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                FuelTheme(Preferences(theme, false)) {
                    Surface { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        SettingsScreen(Preferences(theme, false), busy, {}, {}, { native++ }, { exports++ }, {}, {},
                            deleteContent = { DeleteDataCard(busy) {} })
                    } }
                }
            }
        }
        settingsNode("TUS DATOS")
        compose.onAllNodes(hasText("Drive", substring = true, ignoreCase = true)).assertCountEquals(0)
        capture("tus-datos-claro", false)
        compose.onNodeWithText("Trae tu historial de Fuel").assertDoesNotExist()
        compose.onNodeWithText("Importa vehículos y llenados desde tu CSV.", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Importar CSV de Fuel").assertDoesNotExist()
        settingsNode("Importar llenados de FuelStatus").performClick()
        compose.runOnIdle { assertEquals("Importar FuelStatus", 1, native) }
        settingsNode("Exportar llenados a CSV").performClick()
        compose.runOnIdle { assertEquals("Exportar FuelStatus", 1, exports) }
        capture("csv-claro", false)
        settingsNode("Borrar todos los datos")
        compose.onAllNodes(hasText("Drive", substring = true, ignoreCase = true)).assertCountEquals(0)
        capture("borrar-datos-claro", false)
        compose.runOnIdle { theme = "dark"; scale = 1.3f; busy = true }
        settingsNode("Exportar llenados a CSV").assertIsNotEnabled()
        settingsNode("Importar llenados de FuelStatus").assertIsNotEnabled()
        compose.runOnIdle { busy = false }
        settingsNode("Llenados de FuelStatus")
        capture("csv-oscuro-texto130", false)
        settingsNode("Borrar todos los datos")
        compose.onAllNodes(hasText("Drive", substring = true, ignoreCase = true)).assertCountEquals(0)
        capture("borrar-datos-oscuro-texto130", false)
    }

    @Test fun nativePreviewPreservesAssignmentsAndChoosesMatchingOrNewVehiclePerSource() {
        val first = Vehicle("source1", "Auto", "Sedán", 2024, "premium")
        val second = Vehicle("source2", "Camioneta", "SUV", 2025, "diesel")
        val target = first.copy(id = "target", fuelType = "magna")
        val rows = listOf(first, second).map { FuelImportRow(it, Refuel(vehicleId = it.id,
            timestamp = 1767225600000L, odometerMeters = 1000000, volumeMl = 40000, costCents = 100000)) }
        var saved: ImportPlan? = null
        var canceled = false
        compose.setContent { FuelTheme(Preferences("dark", false)) {
            ImportRefuelsDialog(ImportPreview(rows, CsvSource.FuelStatus), listOf(target), false,
                { saved = it }, {}, { canceled = true })
        } }
        compose.onNodeWithText("Importar historial de FuelStatus").assertIsDisplayed()
        compose.onNodeWithText("Se conservarán el combustible y el método de pago", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Método de pago para los registros").assertDoesNotExist()
        capture("importar-fuelstatus-oscuro", true)
        compose.onNodeWithText("Importar llenados").performClick()
        val matched = requireNotNull(saved)
        assertEquals(target.id, matched.targetVehicleId)
        assertEquals(CsvSource.FuelStatus, matched.source)
        assertFalse(matched.useVehicleFuel)
        compose.onNodeWithText(second.label).performScrollTo().performClick()
        compose.onNodeWithText("Crear e importar").performClick()
        val created = requireNotNull(saved)
        assertEquals(second.id, created.sourceVehicleId)
        assertNull(created.targetVehicleId)
        assertEquals("diesel", created.newVehicleFuelType)
        assertFalse(created.useVehicleFuel)
        compose.onNodeWithText("Cancelar").performClick()
        assertTrue(canceled)
    }

    private fun settingsNode(text: String): SemanticsNodeInteraction {
        // Search through the lazy list, including items not laid out yet.
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.waitForIdle()
        return compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun capture(name: String, dialog: Boolean) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-csv-$name.png").outputStream().use {
            (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage()
                .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
