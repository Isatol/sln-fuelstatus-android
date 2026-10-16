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
        compose.onNodeWithContentDescription("Llenados").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Nuevo llenado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Nuevo llenado").performClick()
        compose.onNodeWithText("Combustible: Premium · 91 octanos").assertIsDisplayed().performClick()
        compose.onNodeWithText("Magna · 87 octanos").performClick()
        compose.onNodeWithText("Odómetro (km)").performTextInput("55555")
        compose.onNodeWithText("Litros").performTextInput("40")
        compose.onNodeWithText("Total pagado ($)").performTextInput("1000")
        compose.onNodeWithText("Guardar llenado").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Llenado guardado").fetchSemanticsNodes().isNotEmpty() }
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
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-$name.png").outputStream().use {
            val target = if (name == "confirmacion-borrar") compose.onNode(isDialog()) else compose.onRoot()
            target.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
