package mx.fuelstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.fuelstatus.backup.SyncState
import mx.fuelstatus.data.*
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataDialogsTest {
    @get:Rule val compose = createComposeRule()
    private fun preview(): ImportPreview {
        val v = Vehicle("source", "Auto del archivo", "Sedán", 2024)
        return ImportPreview(listOf(FuelImportRow(v, Refuel("fill", v.id, 1000, 1000000, 40000, 100000))), CsvSource.FuelStatus)
    }
    @Test fun driveDeletionIsUncheckedByDefaultAndRequiresConfirmation() {
        var requested: Boolean? = null
        compose.setContent { FuelTheme(Preferences()) { Column(Modifier.verticalScroll(rememberScrollState())) {
            DeleteDataCard(SyncState(email = "test@example.com"), false) { requested = it }
        } } }
        compose.onNodeWithText("También borrar el historial de Drive").assertIsOff()
        compose.onNodeWithText("Borrar todos los datos").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        assertNull(requested)
        compose.onNodeWithText("También borrar el historial de Drive").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithText("Borrar todos los datos").performScrollTo().performClick()
        compose.onNodeWithText("Sí, borrar datos").performClick()
        assertEquals(true, requested)
    }
    @Test fun localDeletionDoesNotOfferDriveCheckboxWithoutSession() {
        var requested: Boolean? = null
        compose.setContent { FuelTheme(Preferences()) { DeleteDataCard(SyncState(), false) { requested = it } } }
        compose.onNodeWithText("También borrar el historial de Drive").assertDoesNotExist()
        compose.onNodeWithText("Borrar todos los datos").performClick()
        compose.onNodeWithText("Sí, borrar datos").performClick()
        assertEquals(false, requested)
    }
    @Test fun importChoosesMatchingVehicleAndPreservesCsvAssignments() {
        var plan: ImportPlan? = null
        val target = preview().vehicles.single().copy(id = "target", fuelType = "premium")
        compose.setContent { FuelTheme(Preferences()) { ImportRefuelsDialog(preview(), listOf(target), false, { plan = it }, {}, {}) } }
        compose.onNodeWithText("Método de pago para los registros").assertDoesNotExist()
        compose.onNodeWithText("Importar llenados").performClick()
        assertEquals("target", plan!!.targetVehicleId)
        assertEquals(CsvSource.FuelStatus, plan!!.source)
        assertFalse(plan!!.useVehicleFuel)
    }
    @Test fun noVehiclesOffersCreationBeforeImportOrCreationFromFile() {
        var plan: ImportPlan? = null
        var create = false
        compose.setContent { FuelTheme(Preferences()) { ImportRefuelsDialog(preview(), emptyList(), false, { plan = it }, { create = true }, {}) } }
        compose.onNodeWithText("Crear un vehículo primero").performScrollTo().performClick()
        assertTrue(create)
        compose.onNodeWithText("Crear e importar").performClick()
        assertNull(plan!!.targetVehicleId)
        assertFalse(plan!!.useVehicleFuel)
        assertEquals("magna", plan!!.newVehicleFuelType)
        assertEquals(PaymentMethod.Cash.id, plan!!.paymentMethodId)
    }

    @Test fun importCanChooseAnotherExistingVehicle() {
        var plan: ImportPlan? = null
        val target = Vehicle("target", "Mi auto", "Sedán", 2024)
        compose.setContent { FuelTheme(Preferences()) {
            ImportRefuelsDialog(preview(), listOf(target), false, { plan = it }, {}, {})
        } }
        compose.onNodeWithText(target.label).performScrollTo().performClick()
        compose.onNodeWithText("Importar llenados").performClick()
        assertEquals(target.id, plan!!.targetVehicleId)
        assertEquals(CsvSource.FuelStatus, plan!!.source)
        assertFalse(plan!!.useVehicleFuel)
    }
}
