package mx.fuelstatus

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import mx.fuelstatus.backup.GoogleDriveAuthorization
import mx.fuelstatus.ui.FuelApp

class MainActivity : ComponentActivity() {
    private val model: FuelViewModel by viewModels()
    private val authorization = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) model.driveAuthorizationCanceled()
        else try {
            val access = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
            model.driveAuthorized(access.accessToken ?: error("Google no devolvió acceso a Drive."))
        } catch (e: Exception) { model.driveAuthorizationFailed(e) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FuelApp(model, ::connectDrive) }
    }
    override fun onStart() { super.onStart(); model.onForeground() }
    private fun connectDrive() {
        lifecycleScope.launch {
            model.beginDriveAuthorization()
            try {
                val email = model.backupState.value.email
                val result = Identity.getAuthorizationClient(this@MainActivity)
                    .authorize(GoogleDriveAuthorization.request(email.takeIf { it.isNotBlank() }, email.isBlank())).await()
                if (result.hasResolution()) authorization.launch(IntentSenderRequest.Builder(requireNotNull(result.pendingIntent)).build())
                else model.driveAuthorized(result.accessToken ?: error("Google no devolvió acceso a Drive."))
            } catch (e: CancellationException) { model.driveAuthorizationCanceled(); throw e }
            catch (e: Exception) { model.driveAuthorizationFailed(e) }
        }
    }
}
