package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R
import mx.fuelstatus.data.*

@Composable
fun DeleteDataCard(busy: Boolean, onDelete: () -> Unit) {
    var confirmation by rememberSaveable { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTitle(R.drawable.ic_delete_forever, "Borrar datos", MaterialTheme.typography.titleLarge, MaterialTheme.colorScheme.onErrorContainer)
            Text("Elimina vehículos, llenados, gastos, tarjetas guardadas, categorías personalizadas y copias locales. Restablece la apariencia, como al comenzar de nuevo.",
                color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = { confirmation = true }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.fillMaxWidth()) { Text("Borrar todos los datos") }
        }
    }
    if (confirmation) AlertDialog(onDismissRequest = { if (!busy) confirmation = false },
        title = { Text("¿Borrar todos los datos?") },
        text = { Text("Se eliminarán todos los vehículos, llenados, gastos, tarjetas guardadas, categorías personalizadas y copias locales de recuperación. Los ajustes volverán a sus valores iniciales. Esta eliminación local no se puede deshacer.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { confirmation = false; onDelete() }) {
            Text("Sí, borrar datos", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirmation = false }) { Text("Cancelar") } })
}

@Composable
fun ImportRefuelsDialog(preview: ImportPreview, vehicles: List<Vehicle>, busy: Boolean,
    onImport: (ImportPlan) -> Unit, onCreateVehicle: () -> Unit, onDismiss: () -> Unit) {
    var sourceId by rememberSaveable { mutableStateOf(preview.vehicles.first().id) }
    val source = preview.vehicles.firstOrNull { it.id == sourceId } ?: preview.vehicles.first()
    val sourceRows = preview.rows.filter { it.vehicle.id == source.id }
    val matching = vehicles.firstOrNull { it.name.equals(source.name, true) && it.type.equals(source.type, true) && it.year == source.year }
    var targetId by rememberSaveable(source.id, vehicles.map { it.id }) {
        mutableStateOf(matching?.id ?: "create")
    }
    var createdFuel by rememberSaveable(source.id) { mutableStateOf(source.fuelType) }
    val target = vehicles.firstOrNull { it.id == targetId }
    val canCreate = matching == null
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Importar historial de FuelStatus") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${sourceRows.size} ${if (sourceRows.size == 1) "llenado" else "llenados"} de ${source.label}")
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
                Text("Se conservarán el combustible y el método de pago de cada llenado del CSV. Las tarjetas que falten se agregarán a tus métodos de pago. Los datos ausentes quedarán como Sin especificar.")
                Text("Los duplicados se omiten. Tus registros actuales se conservan.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = !busy && ((targetId == "create" && canCreate) || target != null), onClick = {
            onImport(ImportPlan(source.id, target?.id, useVehicleFuel = false, newVehicleFuelType = createdFuel, source = CsvSource.FuelStatus))
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
