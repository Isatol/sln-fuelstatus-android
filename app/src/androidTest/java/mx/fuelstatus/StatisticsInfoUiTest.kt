package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.data.*
import mx.fuelstatus.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

@RunWith(AndroidJUnit4::class)
class StatisticsInfoUiTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("America/Monterrey")
    private val today = LocalDate.of(2026, 10, 18)
    private val vehicle = Vehicle("car", "Auto", "Sedán", 2024)
    private val rows = (0..6).map { i -> Refuel("r$i", vehicle.id,
        today.minusDays((6 - i) * 2L).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
        1000000L + i * 400000L, if (i > 3) 44000 else 40000, 100000, fuelType = FuelType.Magna.code) }

    private fun open(title: String) {
        val label = "Información de $title"
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(label))
        compose.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNode(isDialog()).assertExists()
        compose.onNode(hasText(title) and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    @Test fun eachOfTheSixteenCardsOpensItsOwnExplanationAndReleasesTheScreenOnClose() {
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(vehicle), refuels = rows), RefuelCalendar(today, zone))
        } } }
        val explanations = listOf(
            "Rendimiento" to "kilómetros totales ÷ litros totales",
            "Tendencia de consumo" to "consumo reciente ÷ consumo anterior",
            "Próximo llenado estimado" to "mediana de días",
            "Alcance de una carga habitual" to "Alcance estimado = litros habituales",
            "Costo reciente / 100 km" to "Costo por 100 km = costo total",
            "Tendencia del precio / L" to "Precio de cada grupo = importe total",
            "Ahorro potencial" to "Diferencia en litros =",
            "Gasto estimado del mes" to "días restantes del mes",
            "Distancia media entre cargas" to "número de pares válidos",
            "Frecuencia de carga" to "Dos cargas el mismo día aportan cero días",
            "Costo / km" to "Costo por km = costo total",
            "Precio medio / L" to "Precio medio por litro = importe total",
            "Litros registrados" to "combustible registrado como comprado",
            "Km evaluados" to "No sumamos lecturas del odómetro",
            "Combustible + otros gastos" to "Total = combustible registrado incluido",
            "Últimos intervalos" to "últimos ocho intervalos completos"
        )
        for ((title, explanation) in explanations) {
            open(title)
            compose.onNode(hasText(explanation, substring = true) and hasAnyAncestor(isDialog())).assertExists()
            if (title == "Próximo llenado estimado") capture("modal-proxima-carga-claro")
            compose.onNodeWithText("Entendido").performClick()
            compose.onNode(isDialog()).assertDoesNotExist()
        }
        compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp() }
        compose.onNodeWithText("Cómo calculamos").assertDoesNotExist()
        capture("final-sin-bloque-largo")
    }

    @Test fun longExplanationScrollsSurvivesRestorationAndClosesWithBackInDarkTheme() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { FuelTheme(Preferences("dark", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(vehicle), refuels = rows), RefuelCalendar(today, zone))
        } } }
        open("Tendencia de consumo")
        val lastParagraph = hasText("Distancia recorrida compara los kilómetros", substring = true) and hasAnyAncestor(isDialog())
        compose.onNode(lastParagraph).performScrollTo().assertIsDisplayed()
        capture("modal-tendencia-oscuro-final")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(isDialog()).assertExists()
        compose.onNode(lastParagraph).performScrollTo().assertIsDisplayed()
        Espresso.pressBack()
        compose.onNode(isDialog()).assertDoesNotExist()
        open("Costo reciente / 100 km")
        compose.onNodeWithText("Entendido").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        capture("tarjetas-dobles-oscuro")
    }

    @Test fun informationIsAvailableWhenCalculationsAndForecastsHaveInsufficientData() {
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(vehicle)), RefuelCalendar(today, zone))
        } } }
        for (title in listOf("Rendimiento", "Próximo llenado estimado", "Gasto estimado del mes", "Frecuencia de carga")) {
            open(title)
            compose.onNodeWithText("Se usan los registros del vehículo seleccionado.").assertExists()
            compose.onNodeWithText("Entendido").performClick()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val target = if (compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()) compose.onNode(isDialog()) else compose.onRoot()
        File(context.filesDir, "qa-info-estadisticas-$name.png").outputStream().use {
            target.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
