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
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Categorías de gastos"))
        compose.onNodeWithText("Categorías de gastos").performClick()
        compose.onNodeWithText("Servicio").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Otros"))
        capture("categorias-lista")
        compose.onNodeWithText("Agregar categoría").performClick()
        compose.onNodeWithText("Guardar categoría").performClick()
        compose.onNodeWithText("Escribe un nombre de hasta 50 caracteres.").assertIsDisplayed()
        compose.onNodeWithText("Nombre de la categoría").performTextInput("Llantas")
        compose.onNodeWithText("Etiqueta").performScrollTo().performClick()
        capture("categorias-iconos")
        compose.onNodeWithText("Buscar icono").performTextInput("neumático")
        compose.onNodeWithText("Llanta").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Nombre de la categoría").assertTextContains("Llantas")
        compose.onNodeWithText("Llanta").assertIsDisplayed()
        compose.onNodeWithText("Guardar categoría").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Volver a Ajustes").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Llantas"))
        compose.onNodeWithText("Llantas").assertIsDisplayed()
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
        compose.onNodeWithContentDescription("Nuevo gasto").performClick()
        compose.onNodeWithText("Servicio").assertIsDisplayed().performClick()
        compose.onNodeWithText("Llantas").performScrollTo().performClick()
        compose.onNodeWithText("Efectivo").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Tarjeta gasolina").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Tarjeta gasolina") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Total pagado ($)").performTextInput("2500")
        capture("nuevo-gasto-categoria")
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Gasto guardado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Tarjeta gasolina").assertIsDisplayed()
        compose.onNodeWithText("Llantas").assertIsDisplayed().performClick()
        compose.onNode(hasText("Tarjeta gasolina") and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.onNodeWithText("Efectivo").performClick()
        compose.onNode(hasText("Llantas") and hasAnyAncestor(isDialog())).assertIsDisplayed().performClick()
        compose.onNodeWithText("Otros").performScrollTo().performClick()
        compose.onNodeWithText("Guardar gasto").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Editar gasto").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Otros").assertIsDisplayed()
        compose.onNodeWithText("Efectivo").assertIsDisplayed()
        capture("gastos")
        compose.onNodeWithContentDescription("Estadísticas").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Cómo calculamos"))
        compose.onNodeWithText("Cómo calculamos").assertIsDisplayed()
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
        // Snackbar overlays the bottom of the list; wait until queued expense messages clear before tapping there.
        var messageClearedAt = 0L
        compose.waitUntil(30000) {
            val now = android.os.SystemClock.uptimeMillis()
            if (compose.onAllNodesWithText("Gasto guardado").fetchSemanticsNodes().isNotEmpty()) {
                messageClearedAt = 0L
                false
            } else {
                if (messageClearedAt == 0L) messageClearedAt = now
                now - messageClearedAt >= 500
            }
        }
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
        compose.onNodeWithContentDescription("Volver a Ajustes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Categorías de gastos"))
        compose.onNodeWithText("Categorías de gastos").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Otros"))
        compose.onNodeWithText("Llantas").assertDoesNotExist()
        compose.onNodeWithText("Otros").assertIsDisplayed()
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
            val target = if (name in listOf("confirmacion-borrar", "nuevo-llenado-pago", "categorias-iconos", "nuevo-gasto-categoria")) compose.onNode(isDialog()) else compose.onRoot()
            target.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
