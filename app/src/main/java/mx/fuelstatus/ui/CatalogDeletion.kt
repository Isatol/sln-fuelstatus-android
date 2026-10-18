package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.fuelstatus.R

@Composable
fun CatalogSwipeToDelete(name: String, protected: Boolean, enabled: Boolean, onRequest: () -> Unit, content: @Composable () -> Unit) {
    if (protected) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(state = state, enableDismissFromStartToEnd = false, gesturesEnabled = enabled,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequest()
                scope.launch { state.reset() }
            }
        }, modifier = Modifier.fillMaxWidth().semantics {
            customActions = if (enabled) listOf(CustomAccessibilityAction("Eliminar $name") { onRequest(); true }) else emptyList()
        }, backgroundContent = {
            Surface(Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).clearAndSetSemantics {}, color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_delete_forever), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }) { content() }
}

@Composable
fun CatalogDeleteDialog(title: String, message: String, busy: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        icon = { FuelIcon(R.drawable.ic_delete_forever) }, title = { Text(title) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) { Text(message) } },
        confirmButton = { TextButton(enabled = !busy, onClick = onConfirm) {
            Text(if (busy) "Eliminando…" else "Eliminar", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } })
}
