@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mx.fuelstatus.R
import mx.fuelstatus.data.*

@Composable
fun ExpenseCategoryIcon(code: String, modifier: Modifier = Modifier) {
    Icon(painterResource(categoryIconResource(code)), contentDescription = null,
        tint = MaterialTheme.colorScheme.primary, modifier = modifier.size(24.dp))
}

@Composable
fun ExpenseCategoriesScreen(categories: List<ExpenseCategory>, busy: Boolean, snackbar: SnackbarHostState,
    onBack: () -> Unit, onAdd: () -> Unit, expenses: List<Expense>, onDelete: (ExpenseCategory, () -> Unit) -> Unit) {
    var deletingName by rememberSaveable { mutableStateOf<String?>(null) }
    val deleting = categories.firstOrNull { it.name == deletingName && it.name != ExpenseCategory.OTHER_NAME }
    Scaffold(topBar = { TopAppBar(title = { Text("Categorías de gastos") }, navigationIcon = {
        IconButton(onClick = onBack, enabled = !busy) { FuelIcon(R.drawable.ic_arrow_back, "Volver a Ajustes") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Surface { Button(onClick = onAdd, enabled = !busy,
            modifier = Modifier.navigationBarsPadding().padding(20.dp).fillMaxWidth().heightIn(min = 54.dp)) {
            FuelIcon(R.drawable.ic_add); Spacer(Modifier.width(10.dp)); Text("Agregar categoría")
        } }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Organiza los gastos de tus vehículos. Desliza una categoría hacia la izquierda para eliminarla, excepto Otros.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(categories, key = { it.name }) { category ->
                CatalogSwipeToDelete(category.name, protected = category.name == ExpenseCategory.OTHER_NAME, enabled = !busy && deleting == null,
                    onRequest = { deletingName = category.name }) {
                    Card(Modifier.fillMaxWidth().testTag("category-${category.name}")) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ExpenseCategoryIcon(category.icon)
                            Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if (deleting != null) {
        val count = expenses.count { it.category == deleting.name }
        CatalogDeleteDialog("¿Eliminar categoría?", "Se eliminará «${deleting.name}».\n\n" +
            if (count > 0) "Esta categoría está asociada a $count ${if (count == 1) "gasto" else "gastos"}. Todos los gastos asociados, de todos tus vehículos, pasarán a Otros. Se conservarán sus demás datos."
            else "No tiene gastos asociados.", busy,
            onDismiss = { deletingName = null }, onConfirm = { onDelete(deleting) { deletingName = null } })
    }
}

@Composable
fun AddExpenseCategoryScreen(categories: List<ExpenseCategory>, busy: Boolean, snackbar: SnackbarHostState,
    onBack: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(CategoryIcon.Tag.code) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Agregar categoría") }, navigationIcon = {
        IconButton(onClick = onBack, enabled = !busy) { FuelIcon(R.drawable.ic_arrow_back, "Volver a categorías de gastos") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Surface { Button(enabled = !busy, modifier = Modifier.imePadding().navigationBarsPadding().padding(20.dp)
            .fillMaxWidth().heightIn(min = 54.dp), onClick = {
            val trimmed = name.trim()
            error = when {
                trimmed.isEmpty() || trimmed.length > ExpenseCategory.MAX_NAME_LENGTH -> "Escribe un nombre de hasta ${ExpenseCategory.MAX_NAME_LENGTH} caracteres."
                categories.any { categorySearchKey(it.name) == categorySearchKey(trimmed) } -> "Ya existe una categoría con ese nombre."
                else -> null
            }
            if (error == null) onSave(trimmed, icon)
        }) { Text(if (busy) "Guardando…" else "Guardar categoría") } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Elige un nombre y un icono para reconocerla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { if (it.length <= ExpenseCategory.MAX_NAME_LENGTH) name = it }, enabled = !busy,
                label = { Text("Nombre de la categoría") }, placeholder = { Text("Por ejemplo: Llantas") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Icono", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showPicker = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                    ExpenseCategoryIcon(icon)
                    Text(CategoryIcon.fromCode(icon)?.label.orEmpty(), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    FuelIcon(R.drawable.ic_keyboard_arrow_down)
                }
                Text("Selecciona un icono", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (showPicker) CategoryIconPicker(icon, { icon = it; showPicker = false }, { showPicker = false })
}

@Composable
fun CategoryIconPicker(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val results = remember(search) { CategoryIcon.entries.filter { it.matches(search) } }
    val cellSize = 120.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.94f).imePadding().heightIn(max = 640.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Seleccionar icono", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(search, { search = it }, label = { Text("Buscar icono") },
                    placeholder = { Text("Llanta, seguro, aceite…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (results.isEmpty()) Text("No hay iconos para esa búsqueda.", modifier = Modifier.weight(1f, fill = false))
                else LazyVerticalGrid(columns = GridCells.Adaptive(cellSize), modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.code }) { option ->
                        FilterChip(selected = option.code == selected, onClick = { onSelect(option.code) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), label = {
                                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExpenseCategoryIcon(option.code)
                                    Text(option.label, style = MaterialTheme.typography.labelMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            })
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancelar") }
            }
        }
    }
}

@Composable
fun ExpenseCategorySelector(categories: List<ExpenseCategory>, value: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.name == value }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                ExpenseCategoryIcon(selected?.icon ?: CategoryIcon.Tag.code)
                Text(value, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                FuelIcon(R.drawable.ic_keyboard_arrow_down)
            }
            DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
                categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) },
                    leadingIcon = { ExpenseCategoryIcon(category.icon) }, onClick = { onChange(category.name); expanded = false }) }
            }
        }
    }
}
