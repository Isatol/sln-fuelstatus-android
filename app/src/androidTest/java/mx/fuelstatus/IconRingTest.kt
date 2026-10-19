package mx.fuelstatus

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import mx.fuelstatus.backup.RoomBackupSource
import mx.fuelstatus.data.FuelDatabase
import mx.fuelstatus.data.FuelRepository
import mx.fuelstatus.data.Vehicle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mx.fuelstatus.backup.AppearanceStore
import mx.fuelstatus.domain.IconRingColor
import mx.fuelstatus.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class IconRingTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun ringSelectionIsIndependentOfAppThemeAndDisabledWhileBusy() {
        var preferences by mutableStateOf(Preferences("light", false, "car", "violet"))
        var busy by mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                FuelTheme(preferences) { Surface { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    SettingsScreen(preferences, busy, { preferences = preferences.copy(theme = it) }, {}, {}, {}, {}, {}, {}, {},
                        onIconRingColor = { preferences = preferences.copy(iconRingColor = it) })
                } } }
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Color del aro del icono"))
        ring("Android").assertIsSelected()
        ring("Azul").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals("blue", preferences.iconRingColor)
            assertEquals("violet", preferences.colorPalette)
            assertFalse(preferences.dynamicColor)
        }
        capture("azul-claro-texto130")
        compose.runOnIdle { preferences = preferences.copy(theme = "dark") }
        ring("Gris").performScrollTo().performClick().assertIsSelected()
        capture("gris-oscuro-texto130")
        ring("Android").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { busy = true }
        ring("Android").assertIsNotEnabled()
        capture("android-oscuro-texto130")
    }

    @Test fun preferencePersistsSynchronizesAndRollsBackWhenLauncherFails() {
        val name = "test-icon-ring"
        val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().clear().commit()
        val applied = mutableListOf<String>()
        try {
            val store = AppearanceStore(context, name) {
                applied += it
                check(it != "rose") { "Synthetic launcher failure" }
            }
            store.synchronizeIcon()
            assertEquals(listOf("android"), applied)
            store.update { it.copy(theme = "dark", colorPalette = "violet", iconRingColor = "blue") }
            val previous = store.state.value
            assertEquals(previous, AppearanceStore(context, name).state.value)
            assertThrows(IllegalStateException::class.java) { store.update { it.copy(iconRingColor = "rose") } }
            assertEquals(previous, store.state.value)
            assertEquals(previous, AppearanceStore(context, name).state.value)
            assertEquals(listOf("android", "blue", "rose", "blue"), applied)
            store.update { Preferences() }
            assertEquals("android", applied.last())
            assertEquals(Preferences(), AppearanceStore(context, name).state.value)
        } finally { prefs.edit().clear().commit() }
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test fun eachAliasHasOneLauncherEntrySamePumpAndIdenticalThemedSilhouette() {
        val icons = LauncherIcons(context)
        val pm = context.packageManager
        val previous = IconRingColor.entries.associateWith { pm.getComponentEnabledSetting(icons.component(it)) }
        val main = ComponentName(context, MainActivity::class.java)
        val mainState = pm.getComponentEnabledSetting(main)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val greenIcon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        val mono = pixels(greenIcon.monochrome!!)
        val green = pixels(greenIcon.foreground)
        val ringPixels = mutableSetOf<Int>()
        try {
            for (option in IconRingColor.entries) {
                icons.apply(option.code)
                val entries = pm.queryIntentActivities(launcher, 0)
                assertEquals("$option launcher count", 1, entries.size)
                assertEquals(icons.component(option).className, entries.single().activityInfo.name)
                assertEquals(mainState, pm.getComponentEnabledSetting(main))
                val drawable = entries.single().loadIcon(pm) as AdaptiveIconDrawable
                assertArrayEquals("$option themed silhouette", mono, pixels(drawable.monochrome!!))
                val foreground = pixels(drawable.foreground)
                // Every opaque original pump pixel must remain exactly unchanged.
                green.indices.filter { green[it] == android.graphics.Color.rgb(41, 75, 43) }.forEach {
                    assertEquals("$option pump pixel $it", green[it], foreground[it])
                }
                ringPixels += foreground[27 * 108 + 54]
                icons.apply(option.code) // Idempotent; cannot create another launcher entry.
                assertEquals(1, pm.queryIntentActivities(launcher, 0).size)
            }
            assertTrue("Ring variants must have different colors", ringPixels.size >= 6)
        } finally {
            pm.setComponentEnabledSettings(previous.map { (option, state) ->
                PackageManager.ComponentEnabledSetting(icons.component(option), state, PackageManager.DONT_KILL_APP)
            })
        }
    }

    @Test fun restoreAndResetApplyRingAndLauncherFailurePreservesHistory() = runBlocking {
        val name = "test-icon-ring-restore"
        val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().clear().commit()
        val database = Room.inMemoryDatabaseBuilder(context, FuelDatabase::class.java).build()
        var applied = "android"
        var rejectAndroid = false
        val appearance = AppearanceStore(context, name) {
            check(it != "rose" && !(rejectAndroid && it == "android")) { "Synthetic launcher failure" }
            applied = it
        }
        try {
            val repository = FuelRepository(database)
            repository.initialize()
            val source = RoomBackupSource(repository, appearance)
            val original = source.snapshot("test", 1000)
            val restored = original.copy(vehicles = listOf(Vehicle("restored", "Restaurado", "Sedán", 2024)),
                appearance = Preferences("dark", false, "restored", "violet", "blue"))
            source.restore(restored)
            assertEquals("blue", applied)
            val before = source.snapshot("test", 1000)
            try {
                source.restore(original.copy(appearance = original.appearance.copy(iconRingColor = "rose")))
                fail("Expected launcher failure")
            } catch (_: IllegalStateException) { }
            assertEquals(before, source.snapshot("test", 1000))
            assertEquals("blue", applied)
            rejectAndroid = true
            try { source.reset(); fail("Expected launcher failure") } catch (_: IllegalStateException) { }
            assertEquals(before, source.snapshot("test", 1000))
            rejectAndroid = false
            source.reset()
            assertEquals("android", applied)
            assertEquals(Preferences(), appearance.state.value)
        } finally { database.close(); prefs.edit().clear().commit() }
    }

    private fun pixels(drawable: Drawable): IntArray {
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 108, 108)
        drawable.draw(Canvas(bitmap))
        return IntArray(108 * 108).also { bitmap.getPixels(it, 0, 108, 0, 0, 108, 108); bitmap.recycle() }
    }
    private fun ring(label: String) = compose.onNodeWithContentDescription("Color del aro del icono: $label")
    private fun capture(name: String) {
        compose.waitForIdle()
        File(context.filesDir, "qa-icon-ring-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
