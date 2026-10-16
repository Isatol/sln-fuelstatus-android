package mx.fuelstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.fuelstatus.backup.SyncState
import mx.fuelstatus.ui.DriveBackupCard
import mx.fuelstatus.ui.FuelTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DriveSettingsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun connectButtonAndLocalBackupDateAreAvailableWithoutAccount() {
        var connected = false
        compose.setContent { FuelTheme(Preferences()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DriveBackupCard(SyncState(lastLocalAt = 1_770_000_000_000), false, { connected = true }, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Copia local:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Conectar Google Drive").performScrollTo().performClick()
        assertTrue(connected)
    }
    @Test fun failedSyncShowsActualErrorDetailAndUnconfirmedCloudDate() {
        compose.setContent { FuelTheme(Preferences()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DriveBackupCard(SyncState(email = "test@example.com", folderId = "folder", phase = "error",
                    lastLocalAt = 1_770_000_000_000, errorTitle = "Google Drive rechazó el permiso de acceso",
                    errorDetail = "HTTP 403 · insufficientPermissions\nPermiso insuficiente", errorAt = 1_770_000_000_000), false, {}, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Última copia en Drive: Nunca").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver detalle del error").performScrollTo().performClick()
        compose.onNodeWithText("HTTP 403", substring = true).assertIsDisplayed()
    }
}
