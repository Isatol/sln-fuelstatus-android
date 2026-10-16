package mx.fuelstatus.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mx.fuelstatus.backup.SyncState
import mx.fuelstatus.backup.RestorePreview
import java.text.DateFormat
import java.util.Date

fun backupDate(timestamp: Long): String = if (timestamp > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp)) else "Nunca"

@Composable
fun DriveBackupCard(state: SyncState, busy: Boolean, onConnect: () -> Unit, onSync: () -> Unit,
    onDisconnect: () -> Unit, onReview: () -> Unit, onUseLocal: () -> Unit) {
    var details by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val enabled = !busy && !state.running
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Copias y Google Drive", style = MaterialTheme.typography.titleLarge)
            Text("Tu historial se guarda primero en este teléfono. Drive conserva una copia de vehículos, llenados, gastos y apariencia.", style = MaterialTheme.typography.bodyMedium)
            Text("Copia local: ${backupDate(state.lastLocalAt)}", style = MaterialTheme.typography.bodySmall)
            if (state.localError.isNotBlank()) Text(state.localError, color = MaterialTheme.colorScheme.error)
            if (state.connected) {
                HorizontalDivider()
                Text(state.email, style = MaterialTheme.typography.titleSmall)
                Text(when (state.phase) {
                    "synced" -> "Al día"
                    "offline" -> "Sin conexión · copia pendiente"
                    "uploading" -> "Subiendo copia a Drive…"
                    "connecting" -> "Conectando con Google…"
                    "restoring" -> "Restaurando historial…"
                    "auth_required" -> "Se necesita autorización"
                    "review" -> "Hay un historial en Drive para revisar"
                    "error" -> "No se pudo sincronizar"
                    else -> "Copia pendiente de subir"
                }, color = MaterialTheme.colorScheme.primary)
                Text("Última copia en Drive: ${backupDate(state.lastCloudAt)}", style = MaterialTheme.typography.bodySmall)
                if (state.lastAttemptAt > 0) Text("Último intento: ${backupDate(state.lastAttemptAt)}", style = MaterialTheme.typography.bodySmall)
                if (state.folderId.isNotBlank()) TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/${Uri.encode(state.folderId)}")))
                }) { Text("Abrir carpeta FuelStatus en Drive") }
                if (state.needsChoice) {
                    Text("Drive contiene otro historial. Revisa la copia antes de continuar; tus registros locales siguen guardados.")
                    Button(onClick = onReview, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Revisar copia de Drive") }
                    OutlinedButton(onClick = { confirmation = "local" }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Usar historial de este teléfono") }
                } else {
                    Button(onClick = onSync, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Sincronizar ahora") }
                    OutlinedButton(onClick = onReview, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Restaurar desde Drive") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onConnect, enabled = enabled) { Text("Reconectar") }
                    TextButton(onClick = { confirmation = "disconnect" }, enabled = enabled) { Text("Desconectar") }
                }
                Text("Se conservan las cinco copias más recientes. Si no hay conexión, se reintenta al volver a abrir la app y también en segundo plano.", style = MaterialTheme.typography.bodySmall)
            } else {
                Button(onClick = onConnect, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(if (state.running) "Conectando…" else "Conectar Google Drive") }
            }
            if (state.errorTitle.isNotBlank()) {
                Text(state.errorTitle, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { details = true }) { Text("Ver detalle del error") }
            }
        }
    }
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text(state.errorTitle) },
        text = { SelectionContainer { Text("${backupDate(state.errorAt)}\n\n${state.errorDetail}", modifier = Modifier.verticalScroll(rememberScrollState())) } },
        confirmButton = { TextButton(onClick = { details = false }) { Text("Cerrar") } })
    confirmation?.let { action -> AlertDialog(onDismissRequest = { confirmation = null },
        title = { Text(if (action == "local") "Usar este historial" else "Desconectar Drive") },
        text = { Text(if (action == "local") "Se subirá una nueva copia con el historial de este teléfono. Las versiones anteriores quedarán sujetas al límite de cinco copias. No se combinarán registros automáticamente."
            else "Las copias existentes de Drive y tus datos locales se conservan. Se detienen las subidas automáticas hasta que vuelvas a conectar.") },
        confirmButton = { TextButton(onClick = { confirmation = null; if (action == "local") onUseLocal() else onDisconnect() }) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancelar") } }) }
}

@Composable
fun RestoreBackupDialog(preview: RestorePreview, busy: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Restaurar historial") },
        text = { Text("Copia del ${backupDate(preview.document.createdAt)}\n${preview.document.vehicles.size} vehículos · ${preview.document.refuels.size} llenados · ${preview.document.expenses.size} gastos\n\nReemplazará el historial de este teléfono. Antes se guardará una copia local de recuperación del historial actual.") },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy) { Text("Restaurar historial") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } })
}
