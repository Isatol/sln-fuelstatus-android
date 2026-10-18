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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

@RunWith(AndroidJUnit4::class)
class ProjectionsUiTest {
    @get:Rule val compose = createComposeRule()
    private val car = Vehicle("car", "Auto", "Sedán", 2024)
    private val other = car.copy(id = "other", name = "Otro auto")
    private val zone = ZoneId.of("America/Monterrey")
    private val today = LocalDate.of(2026, 10, 17)
    private fun timestamp(day: LocalDate) = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun rows(recentLiters: Double = 44.0) = (0..6).map { i ->
        val liters = if (i > 3) recentLiters else 40.0
        Refuel("r$i", car.id, timestamp(LocalDate.of(2026, 10, 4).plusDays(i * 2L)),
            1000000L + i * 400000L, (liters * 1000).toLong(), (liters * (if (i > 3) 27.5 else 25.0) * 100).toLong(),
            fuelType = FuelType.Magna.code)
    }
    private fun scroll(text: String) = compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    private fun displayed(text: String) { scroll(text); compose.onNodeWithText(text).assertIsDisplayed() }

    @Test fun forecastsShowTheirBasisAndTheSixNewCardsWithObservedValues() {
        val expense = Expense("expense", car.id, timestamp(today.minusDays(3)), "Servicio", 30000)
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car), refuels = rows(), expenses = listOf(expense)), RefuelCalendar(today, zone))
        } } }
        displayed("Próximo llenado estimado")
        displayed("18 oct 2026")
        capture("proxima-carga-claro")
        displayed("Alcance de una carga habitual")
        displayed("Si mantienes tu consumo reciente, una carga de 44.0 L podría rendirte aproximadamente 400.0 km: unos 40.0 km menos que con tu rendimiento anterior.")
        capture("alcance-claro")
        displayed("Costo reciente / 100 km"); displayed(money(302.5))
        displayed("Tendencia del precio / L"); displayed("+10.0 %")
        capture("costo-precio-claro")
        displayed("Ahorro potencial")
        displayed("Si recuperas tu rendimiento anterior, podrías ahorrar unos 5.0 L por cada 500 km, equivalentes a ${money(137.5)}.")
        displayed("Gasto estimado del mes"); displayed("≈ ${money(15860.0)}")
        capture("gasto-mensual-claro")
    }

    @Test fun improvementsEditsAndVehicleSelectionRecalculateTheCardsInDarkTheme() {
        var records by mutableStateOf(rows())
        var selected by mutableStateOf(car.id)
        compose.setContent { FuelTheme(Preferences("dark", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car, other), refuels = records,
                preferences = Preferences(vehicleId = selected)), RefuelCalendar(today, zone))
        } } }
        displayed("Si recuperas tu rendimiento anterior, podrías ahorrar unos 5.0 L por cada 500 km, equivalentes a ${money(137.5)}.")
        compose.runOnIdle { records = rows(32.0) }
        displayed("Ya mejoras tu consumo")
        displayed("Si mantienes tu rendimiento reciente, usarías unos 10.0 L menos por cada 500 km que antes, equivalentes a ${money(275.0)}.")
        displayed("Si mantienes tu consumo reciente, una carga de 32.0 L podría rendirte aproximadamente 400.0 km: unos 80.0 km más que con tu rendimiento anterior.")
        capture("mejora-oscuro")
        compose.runOnIdle { records = rows(40.0) }
        displayed("La diferencia para 500 km es menor a 0.1 L. Mantener tu rendimiento conservaría un consumo similar.")
        compose.runOnIdle { records = records.take(3) }
        displayed("Falta 1 intervalo completo")
        displayed("Necesitas tres intervalos completos")
        compose.runOnIdle { selected = other.id }
        displayed("Faltan 3 intervalos completos")
        displayed("Faltan precios comparables")
        displayed("Sin registros de este mes")
        capture("sin-datos-oscuro")
    }

    @Test fun partialRefuelsAndTheCurrentDateUpdateTheForecastWithoutMovingAnExpiredWindow() {
        var calendar by mutableStateOf(RefuelCalendar(today, zone))
        val data = rows() + rows().last().copy(id = "partial", timestamp = timestamp(today),
            odometerMeters = 3500000, fullTank = false, volumeMl = 10000, costCents = 27500)
        compose.setContent { FuelTheme(Preferences("light", false)) { Surface(Modifier.fillMaxSize()) {
            StatisticsScreen(FuelState(vehicles = listOf(car), refuels = data), calendar)
        } } }
        displayed("18 oct 2026")
        displayed("Basada en 6 intervalos completos. Fecha habitual: 18 oct 2026. Hay una carga parcial posterior; la fecha sigue siendo orientativa.")
        compose.runOnIdle { calendar = RefuelCalendar(today.plusDays(2), zone) }
        displayed("Actualiza tu historial")
        displayed("El rango estimado terminaba el 18 oct 2026. Registra tus cargas recientes para actualizar la previsión.")
        capture("fecha-vencida")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-proyecciones-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
