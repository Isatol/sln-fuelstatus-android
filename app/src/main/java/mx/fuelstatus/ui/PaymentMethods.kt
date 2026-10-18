@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mx.fuelstatus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R
import mx.fuelstatus.data.PaymentBrand
import mx.fuelstatus.data.PaymentMethod
import mx.fuelstatus.data.Refuel
import mx.fuelstatus.data.Expense

@Composable
fun SettingsTitle(icon: Int, title: String, style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(title, style = style, color = color, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PaymentLogo(brand: String?, width: Dp = 36.dp, height: Dp = 26.dp) {
    val paymentBrand = brand?.let(PaymentBrand::fromCode)
    val resource = when (paymentBrand) {
        PaymentBrand.Visa -> R.drawable.ic_payment_visa
        PaymentBrand.Mastercard -> R.drawable.ic_payment_mastercard
        PaymentBrand.Amex -> R.drawable.ic_payment_amex
        else -> null
    }
    if (resource == null) Box(Modifier.size(width, height), contentAlignment = Alignment.Center) {
        Icon(painterResource(if (paymentBrand == PaymentBrand.Cash) R.drawable.ic_payments else R.drawable.ic_account_balance_wallet),
            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(height))
    } else Surface(color = Color.White, shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, Color(0xFFD7DBE7)),
        modifier = Modifier.size(width, height)) {
        Image(painterResource(resource), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(3.dp))
    }
}

@Composable
fun PaymentMethodBadge(method: PaymentMethod?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PaymentLogo(method?.brand, 28.dp, 20.dp)
        Text(method?.name ?: "Sin especificar", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PaymentMethodSelector(methods: List<PaymentMethod>, value: String?, onChange: (String?) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, allowUnspecified: Boolean = false,
    label: String = "Método de pago", compact: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val choices = (listOf(PaymentMethod.Cash) + methods).distinctBy { it.id }
    val selected = choices.firstOrNull { it.id == value }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(if (compact) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled,
                modifier = if (compact) Modifier.fillMaxSize().heightIn(min = 56.dp) else Modifier.fillMaxWidth(),
                contentPadding = if (compact) PaddingValues(horizontal = 12.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding) {
                PaymentLogo(selected?.brand, if (compact) 28.dp else 36.dp, if (compact) 20.dp else 26.dp)
                Text(selected?.name ?: "Sin especificar", modifier = Modifier.weight(1f).padding(horizontal = if (compact) 6.dp else 12.dp),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                FuelIcon(R.drawable.ic_keyboard_arrow_down, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
            }
            DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
                if (allowUnspecified) DropdownMenuItem(text = { Text("Sin especificar") },
                    leadingIcon = { PaymentLogo(null) }, onClick = { onChange(null); expanded = false })
                choices.forEach { method ->
                    DropdownMenuItem(text = { Column {
                        Text(method.name)
                        if (method.id != PaymentMethod.Cash.id) Text(PaymentBrand.fromCode(method.brand)?.label.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    } }, leadingIcon = { PaymentLogo(method.brand) }, onClick = { onChange(method.id); expanded = false })
                }
            }
        }
    }
}

@Composable
fun PaymentMethodsScreen(methods: List<PaymentMethod>, busy: Boolean, snackbar: SnackbarHostState,
    onBack: () -> Unit, onAdd: () -> Unit, refuels: List<Refuel>, expenses: List<Expense>,
    onDelete: (PaymentMethod, () -> Unit) -> Unit) {
    val entries = (listOf(PaymentMethod.Cash) + methods).distinctBy { it.id }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    val deleting = entries.firstOrNull { it.id == deletingId && it.id != PaymentMethod.Cash.id }
    Scaffold(topBar = { TopAppBar(title = { Text("Mis métodos de pago") }, navigationIcon = {
        IconButton(onClick = onBack, enabled = !busy) { FuelIcon(R.drawable.ic_arrow_back, "Volver a Ajustes") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Surface { Button(onClick = onAdd, enabled = !busy,
            modifier = Modifier.navigationBarsPadding().padding(20.dp).fillMaxWidth().heightIn(min = 54.dp)) {
            FuelIcon(R.drawable.ic_add_card); Spacer(Modifier.width(10.dp)); Text("Agregar tarjeta")
        } }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Tus opciones para registrar llenados y gastos. Desliza una tarjeta hacia la izquierda para eliminarla.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(entries, key = { it.id }) { method ->
                CatalogSwipeToDelete(method.name, protected = method.id == PaymentMethod.Cash.id, enabled = !busy && deleting == null,
                    onRequest = { deletingId = method.id }) {
                    Card(modifier = Modifier.fillMaxWidth().testTag("payment-${method.id}")) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FuelIcon(if (method.id == PaymentMethod.Cash.id) R.drawable.ic_payments else R.drawable.ic_credit_card,
                                modifier = Modifier.size(34.dp))
                            Column {
                                Text(method.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(if (method.id == PaymentMethod.Cash.id) "Disponible por defecto" else PaymentBrand.fromCode(method.brand)?.label.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (deleting != null) {
        val fills = refuels.count { it.paymentMethodId == deleting.id }
        val costs = expenses.count { it.paymentMethodId == deleting.id }
        CatalogDeleteDialog("¿Eliminar tarjeta?", "Se eliminará «${deleting.name}».\n\n" +
            if (fills + costs > 0) "Esta tarjeta está asociada a $fills ${if (fills == 1) "llenado" else "llenados"} y $costs ${if (costs == 1) "gasto" else "gastos"}. Todos los registros asociados, de todos tus vehículos, pasarán a Efectivo. Se conservarán sus demás datos."
            else "No tiene llenados ni gastos asociados.", busy,
            onDismiss = { deletingId = null }, onConfirm = { onDelete(deleting) { deletingId = null } })
    }
}

@Composable
fun AddPaymentMethodScreen(methods: List<PaymentMethod>, busy: Boolean, snackbar: SnackbarHostState,
    onBack: () -> Unit, onSave: (PaymentMethod) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf(PaymentBrand.Visa.code) }
    var expanded by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Agregar tarjeta") }, navigationIcon = {
        IconButton(onClick = onBack, enabled = !busy) { FuelIcon(R.drawable.ic_arrow_back, "Volver a mis métodos de pago") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Surface { Button(enabled = !busy, modifier = Modifier.imePadding().navigationBarsPadding().padding(20.dp).fillMaxWidth().heightIn(min = 54.dp), onClick = {
            val trimmed = name.trim()
            error = when {
                trimmed.isEmpty() || trimmed.length > PaymentMethod.MAX_NAME_LENGTH -> "Escribe un nombre de hasta ${PaymentMethod.MAX_NAME_LENGTH} caracteres."
                methods.any { it.brand == brand && it.name.equals(trimmed, true) } -> "Ya tienes una tarjeta de esa marca con ese nombre."
                else -> null
            }
            if (error == null) onSave(PaymentMethod(name = trimmed, brand = brand))
        }) { Text(if (busy) "Guardando…" else "Guardar tarjeta") } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Ponle un nombre para reconocerla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { if (it.length <= PaymentMethod.MAX_NAME_LENGTH) name = it }, enabled = !busy,
                label = { Text("Nombre de la tarjeta") }, placeholder = { Text("Por ejemplo: Visa personal") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Marca de la tarjeta", style = MaterialTheme.typography.labelLarge)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expanded = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        PaymentLogo(brand)
                        Text(PaymentBrand.fromCode(brand)?.label.orEmpty(), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                        FuelIcon(R.drawable.ic_keyboard_arrow_down)
                    }
                    DropdownMenu(expanded && !busy, onDismissRequest = { expanded = false }) {
                        PaymentBrand.entries.filter { it != PaymentBrand.Cash }.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, leadingIcon = { PaymentLogo(option.code) },
                                onClick = { brand = option.code; expanded = false })
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
