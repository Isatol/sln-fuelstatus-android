package mx.fuelstatus

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigateCreateRefuelAndChangeTheme() {
        compose.onNodeWithContentDescription("Vehículos").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Añadir vehículo").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Añadir vehículo").performClick()
        compose.onNodeWithText("Nombre / modelo").performTextInput("Mazda 3")
        compose.onNodeWithText("Tipo (Hatchback, Sedán…)").performTextInput("Hatchback")
        compose.onNodeWithText("Año").performTextInput("2025")
        compose.onNodeWithText("Combustible: Magna · 87 octanos").performScrollTo().performClick()
        compose.onNodeWithText("Premium · 91 octanos").performClick()
        compose.onNodeWithText("Guardar vehículo").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Vehículo guardado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Mis métodos de pago"))
        compose.onNodeWithText("Mis métodos de pago").performClick()
        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        addCard("Visa personal", "VISA")
        addCard("Tarjeta gasolina", "Mastercard")
        addCard("Amex viajes", "American Express")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Visa personal").assertIsDisplayed()
        compose.onNodeWithText("Tarjeta gasolina").assertIsDisplayed()
        compose.onNodeWithText("Amex viajes").assertIsDisplayed()
        capture("metodos-pago")
        compose.onNodeWithContentDescription("Volver a Ajustes").performClick()
        compose.onNodeWithContentDescription("Llenados").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Nuevo llenado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Nuevo llenado").performClick()
        compose.onNodeWithText("Combustible: Premium · 91 octanos").assertIsDisplayed().performClick()
        compose.onNodeWithText("Magna · 87 octanos").performClick()
        compose.onNodeWithText("Efectivo").assertIsDisplayed().performClick()
        compose.onNodeWithText("Visa personal").performClick()
        capture("nuevo-llenado-pago")
        compose.onNodeWithText("Odómetro (km)").performTextInput("55555")
        compose.onNodeWithText("Litros").performTextInput("40")
        compose.onNodeWithText("Total pagado ($)").performTextInput("1000")
        compose.onNodeWithText("Guardar llenado").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Llenado guardado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Visa personal"))
        compose.onNodeWithText("Visa personal").assertIsDisplayed()
        compose.onNodeWithText("Magna · 87 octanos").assertIsDisplayed()
        capture("llenados")
        compose.onNodeWithContentDescription("Gastos").performClick()
        compose.onNodeWithContentDescription("Nuevo gasto").assertIsDisplayed()
        capture("gastos")
        compose.onNodeWithContentDescription("Estadísticas").performClick()
        compose.onNodeWithText("Cómo calculamos").performScrollTo().assertIsDisplayed()
        capture("estadisticas")
        compose.onNodeWithContentDescription("Vehículos").performClick()
        compose.onNodeWithText("Mazda 3").assertIsDisplayed()
        compose.onNodeWithText("Premium · 91 octanos").assertIsDisplayed()
        capture("vehiculos")
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.onNodeWithText("Oscuro").assertIsSelected()
        capture("ajustes-oscuro")
        compose.onNodeWithText("Claro").performClick()
        compose.onNodeWithText("Claro").assertIsSelected()
        compose.onNodeWithText("Sistema").performClick()
        compose.onNodeWithText("Conectar Google Drive").performScrollTo().assertIsDisplayed()
        capture("ajustes-drive")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Borrar todos los datos"))
        capture("ajustes-borrar")
        compose.onNodeWithText("Borrar todos los datos").performClick()
        compose.onNodeWithText("¿Borrar todos los datos?").assertIsDisplayed()
        capture("confirmacion-borrar")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Borrar todos los datos").performClick()
        compose.onNodeWithText("Sí, borrar datos").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Datos eliminados").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Vehículos").performClick()
        compose.onNodeWithText("Mazda 3").assertDoesNotExist()
        compose.onNodeWithContentDescription("Añadir vehículo").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Mazda 3").assertDoesNotExist()
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Mis métodos de pago"))
        compose.onNodeWithText("Mis métodos de pago").performClick()
        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        compose.onNodeWithText("Visa personal").assertDoesNotExist()
        compose.onNodeWithText("Tarjeta gasolina").assertDoesNotExist()
        compose.onNodeWithText("Amex viajes").assertDoesNotExist()
    }

    private fun addCard(name: String, brand: String) {
        compose.onNodeWithText("Agregar tarjeta").performClick()
        if (brand == "VISA") {
            compose.onNodeWithText("Guardar tarjeta").performClick()
            compose.onNodeWithText("Escribe un nombre de hasta 80 caracteres.").assertIsDisplayed()
        }
        compose.onNodeWithText("Nombre de la tarjeta").performTextInput(name)
        if (brand != "VISA") {
            compose.onNodeWithText("VISA").performClick()
            compose.onNodeWithText(brand).performClick()
        }
        compose.onNodeWithText("Guardar tarjeta").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Volver a Ajustes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(name).assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-$name.png").outputStream().use {
            val target = if (name in listOf("confirmacion-borrar", "nuevo-llenado-pago")) compose.onNode(isDialog()) else compose.onRoot()
            target.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
