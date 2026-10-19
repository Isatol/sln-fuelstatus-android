package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
class CategoryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchableIconCatalogSupportsEmptyResultsAliasesAndLargeDarkText() {
        var selected: String? = null
        var theme by mutableStateOf("light")
        var scale by mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                FuelTheme(Preferences(theme, false)) { CategoryIconPicker(CategoryIcon.Tag.code, { selected = it }, {}) }
            }
        }
        capture("catalogo-claro", true)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("handyman"))
        compose.onNodeWithText("handyman").assertIsDisplayed()
        compose.runOnIdle { theme = "dark"; scale = 1.3f }
        compose.onNodeWithText("Buscar icono").performTextInput("sin coincidencias")
        compose.onNodeWithText("No hay iconos para esa búsqueda.").assertIsDisplayed()
        compose.onNodeWithText("Buscar icono").performTextReplacement("bateria")
        compose.onNodeWithText("battery_charging_full").assertIsDisplayed()
        capture("catalogo-oscuro-texto130", true)
        compose.onNodeWithText("battery_charging_full").performClick()
        assertEquals(CategoryIcon.Battery.code, selected)
    }

    @Test fun formRejectsDuplicateNamesAndPreservesChosenIconWhenPickerIsCanceled() {
        var saved: Pair<String, String>? = null
        compose.setContent { FuelTheme(Preferences("light", false)) {
            AddExpenseCategoryScreen(ExpenseCategory.Defaults, false, remember { SnackbarHostState() }, {}, { name, icon -> saved = name to icon })
        } }
        compose.onNodeWithText("Nombre de la categoría").performTextInput(" servicio ")
        compose.onNodeWithText("Guardar categoría").performClick()
        compose.onNodeWithText("Ya existe una categoría con ese nombre.").assertIsDisplayed()
        assertNull(saved)
        compose.onNodeWithText("Nombre de la categoría").performTextReplacement(" Refacciones ")
        compose.onNodeWithText("sell").performScrollTo().performClick()
        compose.onNodeWithText("Buscar icono").performTextInput("credit_card")
        compose.onNode(hasText("credit_card") and !hasSetTextAction()).performClick()
        compose.onNode(hasText("credit_card") and !hasSetTextAction()).performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Guardar categoría").performClick()
        assertEquals("Refacciones" to CategoryIcon.CreditCard.code, saved)
    }

    @Test fun categoryListSupportsDarkThemeAndLargeText() {
        val custom = ExpenseCategory("Refacciones y accesorios del vehículo", CategoryIcon.Part.code, 6)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                FuelTheme(Preferences("dark", false)) { ExpenseCategoriesScreen(
                    ExpenseCategory.Defaults.dropLast(1) + custom + ExpenseCategory.Defaults.last(), false, remember { SnackbarHostState() }, {}, {}, emptyList(), { _, _ -> }) }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(custom.name))
        compose.onNodeWithText(custom.name).assertIsDisplayed()
        compose.onNodeWithText("Agregar categoría").assertIsDisplayed()
        capture("lista-oscuro-texto130", false)
    }

    private fun capture(name: String, dialog: Boolean) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-categorias-$name.png").outputStream().use {
            (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage()
                .asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
