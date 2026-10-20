package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mx.fuelstatus.backup.LocalBackupStore
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalDataResetUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsAndConfirmedResetStayLocalAndEmptyAfterReopening() {
        val graph = (compose.activity.application as FuelApplication).graph
        compose.onNodeWithContentDescription("Vehículos").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Añadir vehículo").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val vehicle = Vehicle("local-reset-test", "Auto de prueba local", "Sedán", 2024)
            graph.repository.save(vehicle)
            graph.repository.save(Refuel(vehicleId = vehicle.id, timestamp = 1000, odometerMeters = 1000000, volumeMl = 40000, costCents = 100000))
            graph.repository.save(Expense(vehicleId = vehicle.id, timestamp = 1000, category = "Servicio", costCents = 50000))
            graph.repository.addPaymentMethod(PaymentMethod("local-card", "Tarjeta de prueba", "visa"))
            graph.repository.addExpenseCategory("Categoría de prueba", "tire_repair")
            graph.appearance.update { Preferences("dark", false, vehicle.id) }
            graph.backup.engine.afterMutation()
        }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Auto de prueba local").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Borrar todos los datos"))
        compose.onAllNodes(hasText("Drive", substring = true, ignoreCase = true)).assertCountEquals(0)
        compose.onNodeWithText("FuelStatus · 1.0.0", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Borrar todos los datos").performClick()
        compose.onAllNodes(hasText("Drive", substring = true, ignoreCase = true)).assertCountEquals(0)
        File(compose.activity.filesDir, "qa-confirmacion-borrar-local.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Cancelar").performClick()
        runBlocking { assertTrue(graph.repository.dao.allVehicles().any { it.id == "local-reset-test" }) }
        compose.onNodeWithText("Borrar todos los datos").performClick()
        compose.onNodeWithText("Sí, borrar datos").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Datos eliminados").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Sin vehículos").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            assertTrue(graph.repository.dao.allVehicles().isEmpty())
            assertTrue(graph.repository.dao.allRefuels().isEmpty())
            assertTrue(graph.repository.dao.allExpenses().isEmpty())
            assertEquals(listOf(PaymentMethod.Cash), graph.repository.dao.allPaymentMethods())
            assertEquals(ExpenseCategory.Defaults, graph.repository.dao.allExpenseCategories())
            assertEquals(Preferences(), graph.appearance.state.value)
            val copy = LocalBackupStore(File(compose.activity.filesDir, "backups")).latest()!!.document
            assertTrue(copy.vehicles.isEmpty()); assertTrue(copy.expenses.isEmpty())
        }
    }
}
