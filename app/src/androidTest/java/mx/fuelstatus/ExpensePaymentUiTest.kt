package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
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

@RunWith(AndroidJUnit4::class)
class ExpensePaymentUiTest {
    @get:Rule val compose = createComposeRule()
    private val vehicle = Vehicle("car", "Auto", "Sedán", 2024)
    private val card = PaymentMethod("card", "Mastercard personal", "mastercard")

    @Test fun newExpenseDefaultsToCashAndCanSaveChosenCard() {
        var saved: Expense? = null
        compose.setContent { FuelTheme(Preferences("light", false)) {
            ExpenseEditor(vehicle, null, false, {}, { saved = it }, {}, paymentMethods = listOf(PaymentMethod.Cash, card))
        } }
        compose.onNodeWithText("Efectivo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Total pagado ($)").performTextInput("123.45")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertEquals(PaymentMethod.Cash.id, saved!!.paymentMethodId)
        compose.onNodeWithText("Efectivo").performScrollTo().performClick()
        compose.onNodeWithText(card.name).performClick()
        capture("nuevo-claro")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertEquals(card.id, saved!!.paymentMethodId)
        assertEquals(12345L, saved!!.costCents)
    }

    @Test fun historicalExpenseStaysUnspecifiedUntilUserChoosesMethod() {
        var saved: Expense? = null
        val existing = Expense("expense", vehicle.id, 1000, "Servicio", 12345, "Original")
        compose.setContent { FuelTheme(Preferences("dark", false)) {
            ExpenseEditor(vehicle, existing, false, {}, { saved = it }, {}, paymentMethods = listOf(PaymentMethod.Cash, card))
        } }
        compose.onNodeWithText("Sin especificar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Notas (opcional)").performTextReplacement("Nota editada")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertNull(saved!!.paymentMethodId)
        assertEquals("Nota editada", saved!!.notes)
        compose.onNodeWithText("Sin especificar").performScrollTo().performClick()
        compose.onNodeWithText(card.name).performClick()
        capture("editar-oscuro")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertEquals(card.id, saved!!.paymentMethodId)
    }

    @Test fun editingAssignedExpenseCanKeepOrExplicitlyClearItsMethod() {
        var saved: Expense? = null
        val theme = mutableStateOf("light")
        val existing = Expense("expense", vehicle.id, 1000, "Peaje", 15000, paymentMethodId = card.id)
        compose.setContent { FuelTheme(Preferences(theme.value, false)) {
            ExpenseEditor(vehicle, existing, false, {}, { saved = it }, {}, paymentMethods = listOf(PaymentMethod.Cash, card))
        } }
        compose.onNodeWithText(card.name).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertEquals(card.id, saved!!.paymentMethodId)
        compose.onNodeWithText(card.name).performScrollTo().performClick()
        compose.onNodeWithText("Sin especificar").performClick()
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        assertNull(saved!!.paymentMethodId)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-gasto-pago-$name.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
