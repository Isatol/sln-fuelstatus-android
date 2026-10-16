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
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Nuevo llenado").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Nuevo llenado").performClick()
        compose.onNodeWithText("Odómetro (km)").performTextInput("55555")
        compose.onNodeWithText("Litros").performTextInput("40")
        compose.onNodeWithText("Total pagado ($)").performTextInput("1000")
        compose.onNodeWithText("Guardar llenado").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Llenado guardado").fetchSemanticsNodes().isNotEmpty() }
        capture("llenados")
        compose.onNodeWithContentDescription("Gastos").performClick()
        compose.onNodeWithContentDescription("Nuevo gasto").assertIsDisplayed()
        capture("gastos")
        compose.onNodeWithContentDescription("Estadísticas").performClick()
        compose.onNodeWithText("Cómo calculamos").performScrollTo().assertIsDisplayed()
        capture("estadisticas")
        compose.onNodeWithContentDescription("Vehículos").performClick()
        compose.onNodeWithText("Mazda 3").assertIsDisplayed()
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
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "qa-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
