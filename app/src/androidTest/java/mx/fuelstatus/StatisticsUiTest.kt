package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.data.*
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

@RunWith(AndroidJUnit4::class)
class StatisticsUiTest {
    @get:Rule val compose = createComposeRule()
    private val car = Vehicle("car", "Auto", "Sedán", 2024)
    private val other = car.copy(id = "other", name = "Otro auto")
    private fun rows(recentMl: Long = 43200) = (0..6).map { i ->
        Refuel("fill-$i", car.id, LocalDate.of(2026, 9, 1).plusDays(i * 5L).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            1000000L + i * 400000L, if (i > 3) recentMl else 40000, 100000)
    }
    private fun scroll(text: String) = compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))

    @Test fun trendUsesReadableDatesAndRecalculatesAfterEditingAndDeletingFills() {
        var records by mutableStateOf(rows())
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car), refuels = records))
        } } }
        scroll("Consumo en aumento")
        compose.onNodeWithText("Consumiste un 8.0 % más combustible por kilómetro que en el periodo anterior.").assertIsDisplayed()
        compose.onNodeWithText("Periodo reciente").assertIsDisplayed()
        val recentDates = "${date(rows()[3].timestamp)} — ${date(rows()[6].timestamp)}"
        scroll(recentDates); compose.onNodeWithText(recentDates).assertIsDisplayed()
        scroll("${date(rows()[0].timestamp)} — ${date(rows()[3].timestamp)}")
        compose.onNodeWithText("Periodo anterior").assertIsDisplayed()
        capture("tendencia-claro")
        compose.runOnIdle { records = records.map { if (it.id in listOf("fill-4", "fill-5", "fill-6")) it.copy(volumeMl = 32000) else it } }
        scroll("Consumo en mejora")
        compose.onNodeWithText("Consumiste un 20.0 % menos combustible por kilómetro que en el periodo anterior.").assertIsDisplayed()
        compose.runOnIdle { records = records.dropLast(1) }
        scroll("Aún no hay suficientes datos")
        compose.onNodeWithText("Periodo reciente").assertDoesNotExist()
    }

    @Test fun averagesHaveCorrectValuesAndFollowTheSelectedVehicleInDarkTheme() {
        var selected by mutableStateOf("car")
        val otherRows = listOf(rows()[0].copy(id = "b0", vehicleId = other.id),
            rows()[1].copy(id = "b1", vehicleId = other.id, odometerMeters = 1100000))
        compose.setContent { FuelTheme(Preferences("dark", false)) {
            Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car, other), refuels = rows() + otherRows, preferences = Preferences(vehicleId = selected)))
            }
        } }
        scroll("Distancia media entre cargas")
        compose.onNodeWithText("400.0 km").assertIsDisplayed()
        scroll("Frecuencia de carga")
        compose.onNodeWithText("Cada 5.0 días").assertIsDisplayed()
        compose.onNodeWithText("5 días entre cargas.").assertIsDisplayed()
        capture("medias-oscuro")
        scroll("Costo / km"); compose.onNodeWithText("Precio medio / L").assertExists()
        scroll("Litros registrados"); compose.onNodeWithText("Km evaluados").assertExists()
        scroll("Combustible + otros gastos")
        capture("metricas-oscuro")
        scroll("Últimos intervalos"); capture("intervalos-oscuro")
        scroll("Cómo calculamos"); compose.onNodeWithText("Cómo calculamos").assertIsDisplayed()
        compose.runOnIdle { selected = "other" }
        scroll("Distancia media entre cargas")
        compose.onNodeWithText("100.0 km").assertIsDisplayed()
        scroll("Aún no hay suficientes datos")
        compose.onNodeWithText("Consumo en aumento").assertDoesNotExist()
    }

    @Test fun emptyHistoryShowsExplanationsAndStableAndMarkedStatusesAreAvailable() {
        var records by mutableStateOf(emptyList<Refuel>())
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car), refuels = records))
        } } }
        scroll("Aún no hay suficientes datos")
        scroll("Distancia media entre cargas")
        compose.onAllNodesWithText("Necesitas dos cargas consecutivas válidas.").assertCountEquals(2)
        capture("sin-datos")
        compose.runOnIdle { records = rows(40000) }
        scroll("Consumo estable"); compose.onNodeWithText("Consumo estable").assertIsDisplayed()
        compose.runOnIdle { records = rows(48000) }
        scroll("Aumento importante del consumo"); compose.onNodeWithText("Aumento importante del consumo").assertIsDisplayed()
        capture("aumento-importante")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-estadisticas-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
