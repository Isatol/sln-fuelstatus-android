package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mx.fuelstatus.backup.SyncState
import mx.fuelstatus.data.*

@Composable
fun DeleteDataCard(state: SyncState, busy: Boolean, onDelete: (Boolean) -> Unit) {
    var deleteDrive by rememberSaveable(state.email) { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf(false) }
    // Changing accounts or disconnecting also invalidates any open confirmation.
    LaunchedEffect(state.email) { confirmation = false }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Borrar datos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            Text("Elimina vehículos, llenados, gastos y copias locales. Restablece la apariencia y desconecta Drive, como al comenzar de nuevo.",
                color = MaterialTheme.colorScheme.onErrorContainer)
            if (state.connected) {
                Row(Modifier.fillMaxWidth().toggleable(deleteDrive, enabled = !busy, role = Role.Checkbox,
                    onValueChange = { deleteDrive = it }), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = deleteDrive, onCheckedChange = null, enabled = !busy)
                    Column(Modifier.weight(1f)) {
                        Text("También borrar el historial de Drive", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(state.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Button(onClick = { confirmation = true }, enabled = !busy && !state.running,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.fillMaxWidth()) { Text("Borrar todos los datos") }
        }
    }
    if (confirmation) AlertDialog(onDismissRequest = { if (!busy) confirmation = false },
        title = { Text("¿Borrar todos los datos?") },
        text = { Text("Se eliminarán todos los vehículos, llenados, gastos y copias locales de recuperación. Los ajustes volverán a sus valores iniciales y se desconectará Drive. Esta eliminación local no se puede deshacer.\n\n" +
            if (deleteDrive && state.connected) "También se enviarán a la papelera todas las copias de FuelStatus de ${state.email}. Necesitas conexión. Si Drive falla, los datos locales se conservan."
            else "Las copias existentes en Drive se conservarán.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmation = false; onDelete(deleteDrive && state.connected) }) {
            Text("Sí, borrar datos", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmation = false }) { Text("Cancelar") } })
}

@Composable
fun ImportFuelDialog(preview: ImportPreview, vehicles: List<Vehicle>, selectedVehicleId: String?, busy: Boolean,
    onImport: (ImportPlan) -> Unit, onCreateVehicle: () -> Unit, onDismiss: () -> Unit) {
    var sourceId by rememberSaveable { mutableStateOf(preview.vehicles.first().id) }
    val source = preview.vehicles.firstOrNull { it.id == sourceId } ?: preview.vehicles.first()
    val sourceRows = preview.rows.filter { it.vehicle.id == source.id }
    var targetId by rememberSaveable(vehicles.map { it.id }) { mutableStateOf(selectedVehicleId ?: vehicles.firstOrNull()?.id ?: "create") }
    var useVehicleFuel by rememberSaveable { mutableStateOf(true) }
    var createdFuel by rememberSaveable(source.id) { mutableStateOf(source.fuelType) }
    val target = vehicles.firstOrNull { it.id == targetId }
    val matching = vehicles.firstOrNull { it.name.equals(source.name, true) && it.type.equals(source.type, true) && it.year == source.year }
    val canCreate = matching == null
    val label = FuelType.fromCode(target?.fuelType ?: createdFuel).label
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Importar historial de Fuel") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${sourceRows.size} llenados de ${source.label}")
                if (preview.vehicles.size > 1) {
                    Text("El archivo contiene varios vehículos. Elige cuál importar:")
                    preview.vehicles.forEach { vehicle ->
                        ImportChoice(vehicle.label, source.id == vehicle.id, !busy) { sourceId = vehicle.id }
                    }
                }
                HorizontalDivider()
                if (vehicles.isEmpty()) {
                    Text("No tienes vehículos registrados. Puedes crear uno primero o importar y crear el vehículo encontrado en el archivo.")
                    OutlinedButton(onClick = onCreateVehicle, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Crear un vehículo primero") }
                } else Text("¿A qué vehículo deseas importar los llenados?")
                vehicles.forEach { vehicle -> ImportChoice(vehicle.label, targetId == vehicle.id, !busy) { targetId = vehicle.id } }
                if (canCreate) ImportChoice("Importar y crear ${source.label}", targetId == "create", !busy) { targetId = "create" }
                else Text("El vehículo del archivo ya está registrado. Elige un vehículo de destino.", style = MaterialTheme.typography.bodySmall)
                if (targetId == "create" && canCreate) FuelSelector(createdFuel, { createdFuel = it }, enabled = !busy)
                Row(Modifier.fillMaxWidth().toggleable(useVehicleFuel, enabled = !busy, role = Role.Checkbox,
                    onValueChange = { useVehicleFuel = it }), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useVehicleFuel, onCheckedChange = null, enabled = !busy)
                    Text("Usar $label en todos los llenados importados", modifier = Modifier.weight(1f))
                }
                if (!useVehicleFuel) Text("Se conservará el combustible indicado en el CSV. Si el archivo no lo indica, el registro quedará como Sin especificar.", style = MaterialTheme.typography.bodySmall)
                Text("Los duplicados se omiten. Tus registros actuales se conservan.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = !busy && ((targetId == "create" && canCreate) || target != null), onClick = {
            onImport(ImportPlan(source.id, target?.id, useVehicleFuel, createdFuel))
        }) { Text(if (targetId == "create") "Crear e importar" else "Importar llenados") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ImportChoice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.weight(1f))
    }
}
