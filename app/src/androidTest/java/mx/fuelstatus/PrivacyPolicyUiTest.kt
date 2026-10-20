package mx.fuelstatus

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PrivacyPolicyUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsOpensOfflinePolicyAndBothBackActionsReturnToSettings() {
        val appearance = (compose.activity.application as FuelApplication).graph.appearance
        val original = appearance.state.value
        try {
            appearance.update { it.copy(theme = "light") }
            compose.onNodeWithContentDescription("Ajustes").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Política de privacidad"))
            compose.onNodeWithText("Política de privacidad").performClick()
            assertPolicyLoaded("claro")
            compose.onNodeWithContentDescription("Volver a Ajustes").performClick()
            compose.onNodeWithContentDescription("Ajustes").assertIsDisplayed()
            compose.onNodeWithText("Política de privacidad").assertIsDisplayed()

            appearance.update { it.copy(theme = "dark") }
            compose.onNodeWithText("Política de privacidad").performClick()
            assertPolicyLoaded("oscuro")
            pressBack()
            compose.onNodeWithContentDescription("Ajustes").assertIsDisplayed()
            compose.onNodeWithText("Política de privacidad").assertIsDisplayed()
        } finally {
            appearance.update { original }
        }
    }

    private fun assertPolicyLoaded(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.waitUntil(20000) {
            var loaded = false
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<PrivacyPolicyActivity>().singleOrNull()
                val web = activity?.window?.decorView?.let(::findWebView)
                loaded = web?.progress == 100 && web.title == "Política de privacidad de FuelStatus" && web.canScrollVertically(1)
                if (loaded) {
                    assertFalse(web!!.settings.javaScriptEnabled)
                    assertFalse(web.settings.allowFileAccess)
                    assertFalse(web.settings.allowContentAccess)
                    assertTrue(web.settings.blockNetworkLoads)
                }
            }
            loaded
        }
        compose.onNodeWithContentDescription("Volver a Ajustes").assertIsDisplayed()
        File(instrumentation.targetContext.filesDir, "qa-privacidad-$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findWebView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
