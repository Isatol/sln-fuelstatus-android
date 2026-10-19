package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.data.*
import mx.fuelstatus.domain.*
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

@RunWith(AndroidJUnit4::class)
class RefuelHistoryTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 10, 16)
    private val zone = ZoneId.of("America/Monterrey")
    private val vehicle = Vehicle("car", "Auto", "Sedán", 2024)
    private val rows = listOf("2026-10-10", "2026-05-10", "2026-01-15", "2025-09-15").mapIndexed { index, date ->
        Refuel("fill-$index", vehicle.id, LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            2200000L - index * 400000L, 40000, 100000, station = "Carga $date")
    }

    @Composable private fun History() {
        FuelTheme(Preferences("light", false)) {
            RefuelsScreen(FuelState(vehicles = listOf(vehicle), refuels = rows, loaded = true), {},
                rememberRefuelHistoryFilter(today), RefuelCalendar(today, zone))
        }
    }

    private fun period(current: String, next: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Periodo: $current"))
        compose.onNodeWithText("Periodo: $current").performClick()
        compose.onNodeWithText(next).performClick()
    }

    private fun count(text: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    @Test fun presetsFilterOnlyTheHistoryAndKeepAllTimeIndicators() {
        compose.setContent { History() }
        compose.onNodeWithText("4 llenados").assertIsDisplayed()
        count("Mostrando 1 de 4 llenados")
        capture("filtro-tres-meses")
        period("3 meses", "6 meses")
        count("Mostrando 2 de 4 llenados")
        period("6 meses", "1 año")
        count("Mostrando 3 de 4 llenados")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("4 llenados").assertIsDisplayed()
    }

    @Test fun customDatesApplyAndSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { History() }
        period("3 meses", "Personalizado")
        capture("calendario-rango", true)
        compose.onNodeWithText("Escribir fechas").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("01012026")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("02022026")
        compose.onNodeWithText("Aplicar filtro").assertIsEnabled().performClick()
        count("Mostrando 1 de 4 llenados")
        compose.onNodeWithText("1 ene 2026 — 2 feb 2026").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Carga 2026-01-15"))
        compose.onNodeWithText("Carga 2026-01-15").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        count("Mostrando 1 de 4 llenados")
        compose.onNodeWithText("Periodo: Personalizado").assertIsDisplayed()
        compose.onNodeWithText("1 ene 2026 — 2 feb 2026").assertIsDisplayed()
        capture("filtro-personalizado")
    }

    @Test fun invalidDatesDisableApplyAndCancelKeepsPreviousFilter() {
        compose.setContent { History() }
        period("3 meses", "Personalizado")
        compose.onNodeWithText("Escribir fechas").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("32322026")
        compose.onNodeWithText("Aplicar filtro").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        count("Mostrando 1 de 4 llenados")
        compose.onNodeWithText("Periodo: 3 meses").assertIsDisplayed()
    }

    @Test fun emptyCustomRangeShowsExplanationWithoutDeletingHistory() {
        compose.setContent { History() }
        period("3 meses", "Personalizado")
        compose.onNodeWithText("Escribir fechas").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("01012020")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("02022020")
        compose.onNodeWithText("Aplicar filtro").performClick()
        count("Mostrando 0 de 4 llenados")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("No hay llenados en este periodo"))
        compose.onNodeWithText("No hay llenados en este periodo").assertIsDisplayed()
        period("Personalizado", "1 año")
        count("Mostrando 3 de 4 llenados")
    }

    @Test fun refuelFormStillSavesInputsAndSwitchesWithIcons() {
        var saved: Refuel? = null
        var theme by mutableStateOf("light")
        compose.setContent { FuelTheme(Preferences(theme, false)) {
            RefuelEditor(vehicle, emptyList(), null, false, {}, { saved = it }, {})
        } }
        capture("formulario-inicio", true)
        compose.onNodeWithText("Odómetro (km)").performScrollTo().performTextInput("50000")
        compose.onNodeWithContentDescription("Litros").performScrollTo().performTextInput("40")
        compose.onNodeWithContentDescription("Total pagado ($)").performScrollTo().performTextInput("1000")
        compose.onAllNodes(isToggleable())[0].performScrollTo().performClick().assertIsOff()
        compose.onAllNodes(isToggleable())[1].performScrollTo().performClick().assertIsOff()
        compose.onAllNodes(isToggleable())[2].performScrollTo().performClick().assertIsOn()
        capture("formulario-opciones", true)
        compose.runOnIdle { theme = "dark" }
        compose.onNodeWithText("Gasolinera (opcional)").performScrollTo().performTextInput("Estación de prueba")
        compose.onNodeWithText("Notas (opcional)").performScrollTo().performTextInput("Nota de prueba")
        capture("formulario-notas-oscuro", true)
        compose.onNodeWithText("Guardar llenado").performScrollTo().performClick()
        val fill = requireNotNull(saved)
        assertEquals(50000000L, fill.odometerMeters)
        assertEquals(40000L, fill.volumeMl); assertEquals(100000L, fill.costCents)
        assertFalse(fill.fullTank); assertFalse(fill.included); assertTrue(fill.missedPrevious)
        assertEquals("Estación de prueba", fill.station); assertEquals("Nota de prueba", fill.notes)
    }

    private fun capture(name: String, dialog: Boolean = false) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-llenados-$name.png").outputStream().use {
            (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage()
                .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
