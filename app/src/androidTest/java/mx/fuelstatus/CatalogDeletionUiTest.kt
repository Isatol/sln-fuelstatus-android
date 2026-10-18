package mx.fuelstatus

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.material3.SnackbarHostState
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
class CatalogDeletionUiTest {
    @get:Rule val compose = createComposeRule()
    private val card = PaymentMethod("card", "Tarjeta personal", "visa")

    @Test fun cardSwipeRequiresConfirmationCanBeCanceledRepeatedAndCashIsProtected() {
        var methods by mutableStateOf(listOf(PaymentMethod.Cash, card))
        var deleted: String? = null
        compose.setContent { FuelTheme(Preferences("light", false)) {
            PaymentMethodsScreen(methods, false, remember { SnackbarHostState() }, {}, {},
                listOf(Refuel(vehicleId = "other-car", timestamp = 1000, odometerMeters = 1000, volumeMl = 1000, costCents = 100, paymentMethodId = card.id)),
                listOf(Expense(vehicleId = "another-car", timestamp = 1000, category = "Otros", costCents = 100, paymentMethodId = card.id)),
                onDelete = { method, success -> deleted = method.id; methods = methods.filter { it.id != method.id }; success() })
        } }
        capture("metodos-claro", false)
        compose.onNodeWithTag("payment-cash").performTouchInput { swipeLeft() }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithTag("payment-card").performTouchInput { swipeRight() }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithTag("payment-card").performTouchInput { swipeLeft() }
        compose.onNodeWithText("¿Eliminar tarjeta?").assertIsDisplayed()
        compose.onNodeWithText("Esta tarjeta está asociada a 1 llenado y 1 gasto.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("pasarán a Efectivo", substring = true).assertExists()
        assertNull(deleted)
        capture("tarjeta-asociada-claro", true)
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText(card.name).assertIsDisplayed()
        compose.onNodeWithTag("payment-card").performTouchInput { swipeLeft() }
        compose.onNode(hasText("Eliminar") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(card.id, deleted) }
        compose.onNodeWithTag("payment-card").assertDoesNotExist()
        compose.onNodeWithTag("payment-cash").assertIsDisplayed()
    }

    @Test fun unusedCardSupportsBackDismissalAndBusyStatePreventsSwipe() {
        var busy by mutableStateOf(true)
        var deletes = 0
        compose.setContent { FuelTheme(Preferences("dark", false)) {
            PaymentMethodsScreen(listOf(PaymentMethod.Cash, card), busy, remember { SnackbarHostState() }, {}, {}, emptyList(), emptyList(),
                onDelete = { _, success -> deletes++; success() })
        } }
        compose.onNodeWithTag("payment-card").performTouchInput { swipeLeft() }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.runOnIdle { busy = false }
        compose.onNodeWithTag("payment-card").performTouchInput { swipeLeft() }
        compose.onNodeWithText("No tiene llenados ni gastos asociados.", substring = true).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNode(isDialog()).assertDoesNotExist()
        assertEquals(0, deletes)
        compose.onNodeWithTag("payment-card").performTouchInput { swipeLeft() }
        compose.onNode(hasText("Eliminar") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, deletes)
    }

    @Test fun defaultAndCustomCategoriesCanBeDeletedButOtherCannotWithLargeDarkText() {
        var categories by mutableStateOf(listOf(ExpenseCategory.Defaults.first(),
            ExpenseCategory("Llantas", CategoryIcon.Tire.code, 6), ExpenseCategory.Other))
        var deletes = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                FuelTheme(Preferences("dark", false)) {
                    ExpenseCategoriesScreen(categories, false, remember { SnackbarHostState() }, {}, {},
                        listOf(Expense(vehicleId = "other-car", timestamp = 1000, category = "Servicio", costCents = 12345)),
                        onDelete = { category, success -> deletes++; categories = categories.filter { it.name != category.name }; success() })
                }
            }
        }
        compose.onNodeWithTag("category-Otros").performTouchInput { swipeLeft() }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithTag("category-Servicio").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Esta categoría está asociada a 1 gasto.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("pasarán a Otros", substring = true).assertExists()
        capture("categoria-asociada-oscuro-texto130", true)
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(0, deletes)
        compose.onNodeWithTag("category-Servicio").performTouchInput { swipeLeft() }
        compose.onNode(hasText("Eliminar") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithTag("category-Servicio").assertDoesNotExist()
        compose.onNodeWithTag("category-Llantas").performTouchInput { swipeLeft() }
        compose.onNodeWithText("No tiene gastos asociados.", substring = true).assertIsDisplayed()
        compose.onNode(hasText("Eliminar") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithTag("category-Otros").assertIsDisplayed()
        assertEquals(2, deletes)
        capture("solo-otros-oscuro-texto130", false)
    }

    @Test fun newExpenseFallsBackToOtherWhenServiceWasDeleted() {
        var saved: Expense? = null
        compose.setContent { FuelTheme(Preferences("light", false)) {
            ExpenseEditor(Vehicle("car", "Auto", "Sedán", 2024), null, false, {}, { saved = it }, {},
                categories = listOf(ExpenseCategory.Other))
        } }
        compose.onNodeWithText("Otros").assertIsDisplayed()
        compose.onNodeWithText("Total pagado ($)").performTextInput("123.45")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        val expense = requireNotNull(saved)
        assertEquals("Otros", expense.category)
        assertEquals(12345L, expense.costCents)
    }

    private fun capture(name: String, dialog: Boolean) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-eliminar-$name.png").outputStream().use {
            (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage()
                .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
