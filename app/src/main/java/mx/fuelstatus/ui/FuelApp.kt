@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mx.fuelstatus.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.fuelstatus.*
import mx.fuelstatus.R
import mx.fuelstatus.data.*
import mx.fuelstatus.domain.*

private enum class Destination(val label: String, val icon: Int) {
    Refuels("Llenados", R.drawable.ic_pump), Expenses("Gastos", R.drawable.ic_receipt),
    Statistics("Estadísticas", R.drawable.ic_chart), Vehicles("Vehículos", R.drawable.ic_car), Settings("Ajustes", R.drawable.ic_settings)
}

@Composable
fun FuelApp(model: FuelViewModel, onConnectDrive: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val backup by model.backupState.collectAsStateWithLifecycle()
    val driveBusy by model.driveBusy.collectAsStateWithLifecycle()
    val importPreview by model.importPreview.collectAsStateWithLifecycle()
    val restorePreview by model.restorePreview.collectAsStateWithLifecycle()
    FuelTheme(state.preferences) {
        var destination by rememberSaveable { mutableStateOf(Destination.Refuels) }
        var editor by rememberSaveable { mutableStateOf<String?>(null) }
        var editingId by rememberSaveable { mutableStateOf<String?>(null) }
        var dataPage by rememberSaveable { mutableStateOf<String?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val haptic = LocalHapticFeedback.current
        val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) model.prepareImport(uri)
        }
        val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(model::exportCsv) }
        LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
        if (dataPage != null) {
            val back = { if (!busy && !driveBusy) dataPage = when (dataPage) {
                "add-payment" -> "payments"
                "add-category" -> "categories"
                else -> null
            } }
            BackHandler(onBack = back)
            when (dataPage) {
                "payments" -> PaymentMethodsScreen(state.paymentMethods, busy || driveBusy, snackbar, back,
                    onAdd = { dataPage = "add-payment" })
                "add-payment" -> AddPaymentMethodScreen(state.paymentMethods, busy || driveBusy, snackbar, back,
                    onSave = { method -> model.addPaymentMethod(method) { dataPage = "payments" } })
                "categories" -> ExpenseCategoriesScreen(state.expenseCategories, busy || driveBusy, snackbar, back,
                    onAdd = { dataPage = "add-category" })
                "add-category" -> AddExpenseCategoryScreen(state.expenseCategories, busy || driveBusy, snackbar, back,
                    onSave = { name, icon -> model.addExpenseCategory(name, icon) { dataPage = "categories" } })
            }
            return@FuelTheme
        }
        BackHandler(destination != Destination.Refuels && editor == null) { destination = Destination.Refuels }
        fun openEditor(type: String, id: String? = null) { editingId = id; editor = type }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Surface(modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Destination.entries.forEach { tab ->
                            val selected = tab == destination
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).selectable(selected, role = Role.Tab, onClick = {
                                destination = tab; haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 5.dp)) {
                                    Icon(painterResource(tab.icon), contentDescription = tab.label, modifier = Modifier.size(23.dp))
                                }
                                Text(tab.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (state.loaded && !busy && (state.selectedVehicle != null || destination == Destination.Vehicles)) {
                    when (destination) {
                        Destination.Refuels -> ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = "Nuevo llenado" }, onClick = { openEditor("refuel") },
                            icon = { FuelIcon(R.drawable.ic_add) }, text = { Text("Nuevo llenado") })
                        Destination.Expenses -> ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = "Nuevo gasto" }, onClick = { openEditor("expense") },
                            icon = { FuelIcon(R.drawable.ic_add) }, text = { Text("Nuevo gasto") })
                        Destination.Vehicles -> ExtendedFloatingActionButton(modifier = Modifier.semantics { contentDescription = "Añadir vehículo" }, onClick = { openEditor("vehicle") },
                            icon = { FuelIcon(R.drawable.ic_add) }, text = { Text("Añadir vehículo") })
                        else -> Unit
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FUELSTATUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        Text("EN TU TELÉFONO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(destination.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    if (destination !in listOf(Destination.Vehicles, Destination.Settings)) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(onClick = { expanded = true }, label = { Text(state.selectedVehicle?.label ?: "Sin vehículos") },
                                leadingIcon = { FuelIcon(R.drawable.ic_car) })
                            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                state.vehicles.forEach { vehicle -> DropdownMenuItem(text = { Text(vehicle.label) }, onClick = {
                                    model.selectVehicle(vehicle.id); expanded = false
                                }) }
                            }
                        }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (!state.loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else AnimatedContent(destination, label = "section",
                    modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal)
                            AnimatedContentTransitionScope.SlideDirection.Left
                        else AnimatedContentTransitionScope.SlideDirection.Right
                        (slideIntoContainer(direction, tween(300, easing = FastOutSlowInEasing)) togetherWith
                            slideOutOfContainer(direction, tween(300, easing = FastOutSlowInEasing))).using(null)
                    }) { page ->
                    when (page) {
                        Destination.Refuels -> RefuelsScreen(state, { openEditor("refuel", it.id) }, { importFile.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) })
                        Destination.Expenses -> ExpensesScreen(state) { openEditor("expense", it.id) }
                        Destination.Statistics -> StatisticsScreen(state)
                        Destination.Vehicles -> VehiclesScreen(state, model::selectVehicle) { openEditor("vehicle", it.id) }
                        Destination.Settings -> SettingsScreen(state.preferences, busy || driveBusy || backup.running, model::theme, model::dynamicColor,
                            onImport = { importFile.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
                            onExport = { exportFile.launch("FuelStatus_llenados.csv") },
                            onPaymentMethods = { dataPage = "payments" },
                            onExpenseCategories = { dataPage = "categories" },
                            backupContent = { DriveBackupCard(backup, busy || driveBusy, onConnectDrive,
                                model::syncDrive, model::disconnectDrive, model::reviewDriveCopy, model::useLocalHistory) },
                            deleteContent = { DeleteDataCard(backup, busy || driveBusy) { deleteDrive ->
                                model.resetData(deleteDrive) { destination = Destination.Refuels; editor = null; editingId = null }
                            } })
                    }
                }
            }
        }
        if (editor == null) importPreview?.let { preview ->
            ImportFuelDialog(preview, state.vehicles, state.selectedVehicle?.id, busy || driveBusy,
                model::confirmImport, { destination = Destination.Vehicles; openEditor("vehicle") }, model::dismissImport, state.paymentMethods)
        }
        restorePreview?.let { RestoreBackupDialog(it, busy || driveBusy, model::confirmRestore, model::dismissRestore) }
        val close = { editor = null; editingId = null }
        when (editor) {
            "refuel" -> state.selectedVehicle?.let { vehicle -> RefuelEditor(vehicle, state.selectedRefuels,
                state.refuels.firstOrNull { it.id == editingId }, busy, close,
                onSave = { model.save(it) { haptic.performHapticFeedback(HapticFeedbackType.Confirm); close() } },
                onDelete = { model.delete(it, close) }, paymentMethods = state.paymentMethods) }
            "expense" -> state.selectedVehicle?.let { vehicle -> ExpenseEditor(vehicle,
                state.expenses.firstOrNull { it.id == editingId }, busy, close,
                onSave = { model.save(it, close) }, onDelete = { model.delete(it, close) }, categories = state.expenseCategories,
                paymentMethods = state.paymentMethods) }
            "vehicle" -> VehicleEditor(state.vehicles.firstOrNull { it.id == editingId }, state.vehicles, busy, close,
                onSave = { model.save(it, close) })
        }
    }
}

@Composable
fun FuelIcon(resource: Int, description: String? = null, modifier: Modifier = Modifier) =
    Icon(painterResource(resource), contentDescription = description, modifier = modifier.size(24.dp))

@Composable
private fun RefuelsScreen(state: FuelState, onEdit: (Refuel) -> Unit, onImport: () -> Unit) {
    val refuels = state.selectedRefuels
    val stats = remember(refuels) { statistics(refuels) }
    val periods = remember(stats) { stats.periods.associateBy { it.endId } }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Hero("TU RENDIMIENTO", stats.kmPerLiter?.let { number(it) } ?: "—", "km/L",
            if (stats.periods.isEmpty()) "Aparecerá después de dos tanques llenos." else "Promedio de ${stats.periods.size} intervalos completos") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Combustible", money(stats.fuelCost), Modifier.weight(1f))
                Metric("Historial", "${refuels.size} llenados", Modifier.weight(1f))
            }
        }
        if (refuels.isEmpty()) item { EmptyState(R.drawable.ic_pump, "Aquí empieza tu historial",
            "Registra tu primer llenado o trae las cargas que ya tienes en Fuel.", "Importar CSV de Fuel", onImport) }
        else {
            item { SectionLabel("TODOS LOS LLENADOS", "Toca una carga para editar") }
            items(refuels, key = { it.id }) { refuel ->
                Card(onClick = { onEdit(refuel) }, shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(11.dp)) { FuelIcon(R.drawable.ic_pump) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(date(refuel.timestamp), fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    RefuelDetail(R.drawable.ic_route, "${number(refuel.km, 0)} km")
                                    RefuelDetail(if (refuel.fullTank) R.drawable.ic_water_full else R.drawable.ic_water_medium,
                                        if (refuel.fullTank) "Tanque lleno" else "Carga parcial")
                                }
                                Text(FuelType.fromCode(refuel.fuelType).label, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(Modifier.widthIn(max = 132.dp), horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(money(refuel.cost), fontWeight = FontWeight.Bold)
                                PaymentMethodBadge(state.paymentMethods.firstOrNull { it.id == refuel.paymentMethodId })
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallMetric("Litros", "${number(refuel.liters)} L", Modifier.weight(1f), R.drawable.ic_water_drop)
                            SmallMetric("Precio / L", money(refuel.pricePerLiter), Modifier.weight(1f), R.drawable.ic_payments)
                            SmallMetric("Rendimiento", periods[refuel.id]?.let { "${number(it.kmPerLiter)} km/L" } ?: "—",
                                Modifier.weight(1f), R.drawable.ic_speed)
                        }
                        if (!refuel.included) Text("Fuera de estadísticas", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        if (refuel.missedPrevious) Text("Falta una carga anterior", style = MaterialTheme.typography.labelMedium)
                        if (refuel.station.isNotBlank()) Text(refuel.station, style = MaterialTheme.typography.bodySmall)
                        if (refuel.notes.isNotBlank()) Text(refuel.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesScreen(state: FuelState, onEdit: (Expense) -> Unit) {
    val expenses = state.selectedExpenses
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Hero("CUIDAR TU AUTO", money(expenses.sumOf { it.costCents } / 100.0), "", "${expenses.size} gastos registrados · sin combustible") }
        if (expenses.isEmpty()) item { EmptyState(R.drawable.ic_receipt, "Cada gasto cuenta", "Guarda servicios, seguros, estacionamientos y otros gastos de tu vehículo.") }
        items(expenses, key = { it.id }) { expense ->
            Card(onClick = { onEdit(expense) }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    FuelIcon(R.drawable.ic_receipt)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(expense.category, fontWeight = FontWeight.Bold)
                        Text(date(expense.timestamp), style = MaterialTheme.typography.bodySmall)
                        if (expense.notes.isNotBlank()) Text(expense.notes, style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.widthIn(max = 132.dp), horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(money(expense.costCents / 100.0), fontWeight = FontWeight.Bold)
                        PaymentMethodBadge(state.paymentMethods.firstOrNull { it.id == expense.paymentMethodId })
                    }
                }
            }
        }
    }
}

@Composable
private fun VehiclesScreen(state: FuelState, onSelect: (String) -> Unit, onEdit: (Vehicle) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Un historial para cada camino.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp)) }
        if (state.vehicles.isEmpty()) item {
            Text("Añade tu primer vehículo con el botón inferior o créalo al importar un CSV de Fuel.")
        }
        items(state.vehicles, key = { it.id }) { vehicle ->
            val selected = vehicle.id == state.selectedVehicle?.id
            val records = state.refuels.filter { it.vehicleId == vehicle.id }
            Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor =
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FuelIcon(R.drawable.ic_car, modifier = Modifier.size(40.dp))
                        IconButton(onClick = { onEdit(vehicle) }) { FuelIcon(R.drawable.ic_edit, "Editar ${vehicle.label}") }
                    }
                    Text(vehicle.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${vehicle.type} · ${vehicle.year}")
                    Text(FuelType.fromCode(vehicle.fuelType).label, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SmallMetric("Odómetro", records.maxOfOrNull { it.km }?.let { "${number(it, 0)} km" } ?: "Sin registros")
                        SmallMetric("Llenados", records.size.toString())
                    }
                    FilledTonalButton(onClick = { onSelect(vehicle.id) }, enabled = !selected) { Text(if (selected) "Vehículo seleccionado" else "Seleccionar vehículo") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(preferences: Preferences, busy: Boolean, onTheme: (String) -> Unit, onDynamic: (Boolean) -> Unit, onImport: () -> Unit, onExport: () -> Unit,
    onPaymentMethods: () -> Unit, onExpenseCategories: () -> Unit, backupContent: @Composable () -> Unit, deleteContent: @Composable () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionLabel("APARIENCIA", "A tu manera") }
        item { Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTitle(R.drawable.ic_palette, "Tema")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro").forEach { (key, label) ->
                        FilterChip(selected = preferences.theme == key, onClick = { onTheme(key) }, label = { Text(label) })
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Color dinámico", fontWeight = FontWeight.Bold)
                        Text("Material You · colores de tu fondo", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = preferences.dynamicColor, onCheckedChange = onDynamic)
                }
                Text("Al desactivarlo se usa la paleta verde de FuelStatus.", style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { backupContent() }
        item { SectionLabel("TUS DATOS", "Siempre contigo") }
        item { Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsTitle(R.drawable.ic_upload_file, "Trae tu historial de Fuel")
                Text("Importa vehículos y llenados desde tu CSV. Los duplicados se omiten y no se reemplazan tus registros.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onImport, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Importar CSV de Fuel") }
                OutlinedButton(onClick = onExport, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Exportar llenados a CSV") }
                Text("La exportación incluye los llenados de todos tus vehículos. La copia completa de Drive también incluye los otros gastos.", style = MaterialTheme.typography.bodySmall)
            }
        } }
        item {
            FilledTonalButton(onClick = onPaymentMethods, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                FuelIcon(R.drawable.ic_account_balance_wallet)
                Text("Mis métodos de pago", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                FuelIcon(R.drawable.ic_chevron_right)
            }
        }
        item {
            FilledTonalButton(onClick = onExpenseCategories, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                FuelIcon(R.drawable.ic_category_sell)
                Text("Categorías de gastos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                FuelIcon(R.drawable.ic_chevron_right)
            }
        }
        item { deleteContent() }
        item { Explanation("Tu auto, tus datos", "Todo se guarda en este teléfono, con copias opcionales en tu Google Drive. Sin anuncios ni suscripción. Los importes se muestran en pesos mexicanos y las distancias en kilómetros.") }
        item { Text("FuelStatus · 0.8.0\nHecha para Android", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
    }
}

@Composable
internal fun Hero(title: String, value: String, unit: String, subtitle: String, icon: Int? = null) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon == null) Text(title, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            else SettingsTitle(icon, title, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displayLarge.copy(fontSize = if (value.length > 9) 36.sp else 56.sp), fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false))
                if (unit.isNotBlank()) Text(unit, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 9.dp))
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun RefuelDetail(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
private fun SmallMetric(label: String, value: String, modifier: Modifier = Modifier, icon: Int? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            icon?.let {
                Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
@Composable
private fun SectionLabel(label: String, secondary: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(secondary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable
private fun EmptyState(icon: Int, title: String, text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(18.dp)) { FuelIcon(icon) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}
@Composable
internal fun Explanation(title: String, text: String, icon: Int? = null) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon == null) Text(title, style = MaterialTheme.typography.titleMedium)
        else SettingsTitle(icon, title)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
