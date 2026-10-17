@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R
import mx.fuelstatus.data.*
import java.time.*
import java.util.UUID

@Composable
fun RefuelEditor(vehicle: Vehicle, records: List<Refuel>, existing: Refuel?, busy: Boolean,
    onDismiss: () -> Unit, onSave: (Refuel) -> Unit, onDelete: (Refuel) -> Unit,
    paymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash)) {
    val key = existing?.id ?: "new-refuel"
    var timestamp by rememberSaveable(key) { mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var odometer by rememberSaveable(key) { mutableStateOf(existing?.let { inputNumber(it.odometerMeters, 3) }.orEmpty()) }
    var liters by rememberSaveable(key) { mutableStateOf(existing?.let { inputNumber(it.volumeMl, 3) }.orEmpty()) }
    var total by rememberSaveable(key) { mutableStateOf(existing?.let { inputNumber(it.costCents, 2) }.orEmpty()) }
    var fuelType by rememberSaveable(key) { mutableStateOf(existing?.fuelType ?: vehicle.fuelType) }
    var paymentMethodId by rememberSaveable(key) { mutableStateOf(if (existing != null) existing.paymentMethodId else PaymentMethod.Cash.id) }
    var full by rememberSaveable(key) { mutableStateOf(existing?.fullTank ?: true) }
    var included by rememberSaveable(key) { mutableStateOf(existing?.included ?: true) }
    var missed by rememberSaveable(key) { mutableStateOf(existing?.missedPrevious ?: false) }
    var station by rememberSaveable(key) { mutableStateOf(existing?.station.orEmpty()) }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    EditorSheet(if (existing == null) "Nuevo llenado" else "Editar llenado", vehicle.label, busy, onDismiss) {
        FuelSelector(fuelType, { fuelType = it }, allowUnspecified = existing != null)
        PaymentMethodSelector(paymentMethods, paymentMethodId, { paymentMethodId = it }, enabled = !busy, allowUnspecified = existing != null)
        DateField(timestamp) { timestamp = it }
        NumberField("Odómetro (km)", odometer, { odometer = it }, "Último: ${records.maxOfOrNull { it.km }?.let { number(it, 0) } ?: "—"} km")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField("Litros", liters, { liters = it }, modifier = Modifier.weight(1f))
            NumberField("Total pagado ($)", total, { total = it }, modifier = Modifier.weight(1f))
        }
        val volume = scaledInput(liters, 3)
        val cents = scaledInput(total, 2)
        if (volume != null && volume > 0 && cents != null && cents > 0) {
            Text("Precio calculado: ${money(cents / 100.0 / (volume / 1000.0))} / L", style = MaterialTheme.typography.bodyMedium)
        }
        ToggleRow("Tanque lleno", "Desactívalo si fue una carga parcial.", full) { full = it }
        ToggleRow("Incluir en estadísticas", "Cuenta esta carga en totales y promedios.", included) { included = it }
        ToggleRow("Falta una carga anterior", "Reinicia el cálculo de consumo desde aquí.", missed) { missed = it }
        OutlinedTextField(station, { station = it }, label = { Text("Gasolinera (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
            val meters = scaledInput(odometer, 3)
            val ml = scaledInput(liters, 3)
            val amount = scaledInput(total, 2)
            error = when {
                meters == null || meters < 0 -> "Escribe un odómetro válido en kilómetros."
                ml == null || ml <= 0 -> "Los litros deben ser mayores a cero."
                amount == null || amount <= 0 -> "El total pagado debe ser mayor a cero."
                records.any { it.id != existing?.id && it.odometerMeters == meters } -> "Ya existe una carga con ese odómetro. Revisa el kilometraje."
                else -> null
            }
            if (error == null) onSave(Refuel(existing?.id ?: UUID.randomUUID().toString(), vehicle.id, timestamp,
                meters!!, ml!!, amount!!, full, included, missed, station.trim(), notes.trim(), fuelType, paymentMethodId))
        }) { Text("Guardar llenado") }
        if (existing != null) TextButton(onClick = { deleteConfirmation = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Eliminar llenado", color = MaterialTheme.colorScheme.error)
        }
    }
    if (deleteConfirmation && existing != null) DeleteDialog("este llenado", { deleteConfirmation = false }) { onDelete(existing) }
}

@Composable
fun ExpenseEditor(vehicle: Vehicle, existing: Expense?, busy: Boolean, onDismiss: () -> Unit,
    onSave: (Expense) -> Unit, onDelete: (Expense) -> Unit, categories: List<ExpenseCategory> = ExpenseCategory.Defaults) {
    val key = existing?.id ?: "new-expense"
    var timestamp by rememberSaveable(key) { mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var category by rememberSaveable(key) { mutableStateOf(existing?.category ?: "Servicio") }
    var total by rememberSaveable(key) { mutableStateOf(existing?.let { inputNumber(it.costCents, 2) }.orEmpty()) }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    EditorSheet(if (existing == null) "Nuevo gasto" else "Editar gasto", vehicle.label, busy, onDismiss) {
        DateField(timestamp) { timestamp = it }
        ExpenseCategorySelector(categories, category, { category = it }, enabled = !busy)
        NumberField("Total pagado ($)", total, { total = it })
        OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
            val amount = scaledInput(total, 2)
            if (amount == null || amount <= 0) error = "El total pagado debe ser mayor a cero."
            else onSave(Expense(existing?.id ?: UUID.randomUUID().toString(), vehicle.id, timestamp, category, amount, notes.trim()))
        }) { Text("Guardar gasto") }
        if (existing != null) TextButton(onClick = { deleteConfirmation = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Eliminar gasto", color = MaterialTheme.colorScheme.error)
        }
    }
    if (deleteConfirmation && existing != null) DeleteDialog("este gasto", { deleteConfirmation = false }) { onDelete(existing) }
}

@Composable
fun VehicleEditor(existing: Vehicle?, vehicles: List<Vehicle>, busy: Boolean, onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    val key = existing?.id ?: "new-vehicle"
    var name by rememberSaveable(key) { mutableStateOf(existing?.name.orEmpty()) }
    var type by rememberSaveable(key) { mutableStateOf(existing?.type.orEmpty()) }
    var fuelType by rememberSaveable(key) { mutableStateOf(existing?.fuelType ?: FuelType.Magna.code) }
    var year by rememberSaveable(key) { mutableStateOf(existing?.year?.toString().orEmpty()) }
    var error by rememberSaveable(key) { mutableStateOf<String?>(null) }
    EditorSheet(if (existing == null) "Añadir vehículo" else "Editar vehículo", "Un historial independiente para cada auto", busy, onDismiss) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre / modelo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(type, { type = it }, label = { Text("Tipo (Hatchback, Sedán…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(year, { year = it }, label = { Text("Año") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        FuelSelector(fuelType, { fuelType = it })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), onClick = {
            val y = year.toIntOrNull()
            error = when {
                name.isBlank() -> "Escribe el nombre del vehículo."
                y == null || y !in 1886..2100 -> "Escribe un año válido."
                vehicles.any { it.id != existing?.id && it.name.equals(name.trim(), true) && it.type.equals(type.trim(), true) && it.year == y } -> "Ese vehículo ya está registrado."
                else -> null
            }
            if (error == null) onSave(Vehicle(existing?.id ?: UUID.randomUUID().toString(), name.trim(), type.trim(), y!!, fuelType))
        }) { Text("Guardar vehículo") }
    }
}

@Composable
fun FuelSelector(value: String, onChange: (String) -> Unit, allowUnspecified: Boolean = false, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Combustible: ${FuelType.fromCode(value).label}")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            FuelType.entries.filter { allowUnspecified || it != FuelType.Unspecified }.forEach { fuel ->
                DropdownMenuItem(text = { Text(fuel.label) }, onClick = { onChange(fuel.code); expanded = false })
            }
        }
    }
}

@Composable
private fun EditorSheet(title: String, subtitle: String, busy: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, enabled = !busy) { FuelIcon(R.drawable.ic_close, "Cerrar formulario") }
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, supporting: String? = null, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier = modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        supportingText = supporting?.let { { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun DateField(timestamp: Long, onChange: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Fecha: ${date(timestamp)}") }
    if (show) {
        val localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        val state = rememberDatePickerState(initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { show = false }, confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    val chosen = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    if (chosen != localDate) onChange(chosen.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }; show = false
            }) { Text("Elegir fecha") }
        }, dismissButton = { TextButton(onClick = { show = false }) { Text("Cancelar") } }) { DatePicker(state) }
    }
}

@Composable
private fun DeleteDialog(subject: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("¿Eliminar $subject?") },
        text = { Text("Se quitará del historial y se recalcularán las estadísticas.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
