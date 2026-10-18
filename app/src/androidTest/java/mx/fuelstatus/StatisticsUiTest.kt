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
        compose.onNodeWithText("Tu consumo aumentó un 8.0 % (tu rendimiento bajó de 10.0 a 9.3 km/L) respecto al periodo anterior.").assertIsDisplayed()
        scroll("En los 1,200.0 km del periodo reciente, equivale a unos 9.6 L adicionales respecto a mantener el consumo anterior.")
        compose.onNodeWithText("En los 1,200.0 km del periodo reciente, equivale a unos 9.6 L adicionales respecto a mantener el consumo anterior.").assertIsDisplayed()
        scroll("Recorriste prácticamente la misma distancia.")
        compose.onNodeWithText("Recorriste prácticamente la misma distancia.").assertIsDisplayed()
        scroll("Periodo reciente")
        compose.onNodeWithText("Periodo reciente").assertIsDisplayed()
        val recentDates = "${date(rows()[3].timestamp)} — ${date(rows()[6].timestamp)}"
        scroll(recentDates); compose.onNodeWithText(recentDates).assertIsDisplayed()
        scroll("${date(rows()[0].timestamp)} — ${date(rows()[3].timestamp)}")
        compose.onNodeWithText("Periodo anterior").assertIsDisplayed()
        capture("tendencia-claro")
        compose.runOnIdle { records = records.map { if (it.id in listOf("fill-4", "fill-5", "fill-6")) it.copy(volumeMl = 32000) else it } }
        scroll("Consumo en mejora")
        compose.onNodeWithText("Tu consumo disminuyó un 20.0 % (tu rendimiento subió de 10.0 a 12.5 km/L) respecto al periodo anterior.").assertIsDisplayed()
        scroll("En los 1,200.0 km del periodo reciente, equivale a unos 24.0 L menos respecto a mantener el consumo anterior.")
        compose.onNodeWithText("En los 1,200.0 km del periodo reciente, equivale a unos 24.0 L menos respecto a mantener el consumo anterior.").assertIsDisplayed()
        compose.runOnIdle { records = records.dropLast(1) }
        scroll("Aún no hay suficientes datos")
        compose.onNodeWithText("Periodo reciente").assertDoesNotExist()
        compose.onNodeWithText("Distancia recorrida").assertDoesNotExist()
    }

    @Test fun distanceAndFuelEquivalenceCompareTheSameWindowsWithDifferentDistances() {
        var records by mutableStateOf(rows(35200).mapIndexed { i, row ->
            row.copy(odometerMeters = if (i <= 3) 1000000L + i * 500000L else 2500000L + (i - 3) * 400000L)
        })
        var theme by mutableStateOf("light")
        compose.setContent { FuelTheme(Preferences(theme, false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car), refuels = records))
        } } }
        val summary = "Tu consumo aumentó un 10.0 % (tu rendimiento bajó de 12.5 a 11.4 km/L) respecto al periodo anterior."
        scroll(summary); compose.onNodeWithText(summary).assertIsDisplayed()
        capture("equivalencia-claro")
        scroll("Recorriste 300.0 km menos.")
        compose.onNodeWithText("Recorriste 300.0 km menos.").assertIsDisplayed()
        scroll("1,200.0 km frente a 1,500.0 km en el periodo anterior: un 20.0 % menos.")
        compose.onNodeWithText("1,200.0 km frente a 1,500.0 km en el periodo anterior: un 20.0 % menos.").assertIsDisplayed()
        capture("distancia-menos-claro")
        compose.runOnIdle {
            theme = "dark"
            records = records.mapIndexed { i, row -> if (i <= 3) row else row.copy(
                odometerMeters = 2500000L + (i - 3) * 600000L, volumeMl = 52800) }
        }
        scroll("En los 1,800.0 km del periodo reciente, equivale a unos 14.4 L adicionales respecto a mantener el consumo anterior.")
        compose.onNodeWithText("En los 1,800.0 km del periodo reciente, equivale a unos 14.4 L adicionales respecto a mantener el consumo anterior.").assertIsDisplayed()
        scroll("Recorriste 300.0 km más."); compose.onNodeWithText("Recorriste 300.0 km más.").assertIsDisplayed()
        scroll("1,800.0 km frente a 1,500.0 km en el periodo anterior: un 20.0 % más.")
        compose.onNodeWithText("1,800.0 km frente a 1,500.0 km en el periodo anterior: un 20.0 % más.").assertIsDisplayed()
        capture("distancia-mas-oscuro")
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
        scroll("400.0 km")
        compose.onNodeWithText("400.0 km").assertIsDisplayed()
        scroll("Frecuencia de carga")
        scroll("Cada 5.0 días")
        compose.onNodeWithText("Cada 5.0 días").assertIsDisplayed()
        scroll("5 días entre cargas.")
        compose.onNodeWithText("5 días entre cargas.").assertIsDisplayed()
        capture("medias-oscuro")
        scroll("Costo / km"); compose.onNodeWithText("Precio medio / L").assertExists()
        scroll("Litros registrados"); compose.onNodeWithText("Km evaluados").assertExists()
        scroll("Combustible + otros gastos")
        capture("metricas-oscuro")
        scroll("Últimos intervalos"); capture("intervalos-oscuro")
        scroll("Últimos intervalos"); compose.onNodeWithText("Últimos intervalos").assertIsDisplayed()
        compose.onNodeWithText("Cómo calculamos").assertDoesNotExist()
        compose.runOnIdle { selected = "other" }
        scroll("Distancia media entre cargas")
        scroll("100.0 km")
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
        scroll("En los 1,200.0 km del periodo reciente, la diferencia estimada de combustible es menor a 0.1 L respecto a mantener el consumo anterior.")
        compose.onNodeWithText("En los 1,200.0 km del periodo reciente, la diferencia estimada de combustible es menor a 0.1 L respecto a mantener el consumo anterior.").assertIsDisplayed()
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
