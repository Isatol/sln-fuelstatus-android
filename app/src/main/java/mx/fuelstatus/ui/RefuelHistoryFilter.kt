@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import mx.fuelstatus.R
import mx.fuelstatus.domain.*
import java.time.*
import java.time.format.DateTimeFormatter

@Stable
class RefuelHistoryFilterState(today: LocalDate) {
    var period by mutableStateOf(RefuelHistoryPeriod.ThreeMonths)
    var startDay by mutableLongStateOf(today.minusMonths(3).toEpochDay())
    var endDay by mutableLongStateOf(today.toEpochDay())
    var choosingDates by mutableStateOf(false)
    val customRange get() = RefuelDateRange(LocalDate.ofEpochDay(startDay), LocalDate.ofEpochDay(endDay))

    fun reset(today: LocalDate = LocalDate.now()) {
        period = RefuelHistoryPeriod.ThreeMonths
        startDay = today.minusMonths(3).toEpochDay(); endDay = today.toEpochDay(); choosingDates = false
    }

    fun apply(range: RefuelDateRange) {
        startDay = range.start.toEpochDay(); endDay = range.endInclusive.toEpochDay()
        period = RefuelHistoryPeriod.Custom; choosingDates = false
    }

    companion object {
        val Saver = listSaver<RefuelHistoryFilterState, Any>(
            save = { listOf(it.period.name, it.startDay, it.endDay, it.choosingDates) },
            restore = { values -> RefuelHistoryFilterState(LocalDate.ofEpochDay(values[2] as Long)).apply {
                period = RefuelHistoryPeriod.valueOf(values[0] as String)
                startDay = values[1] as Long; endDay = values[2] as Long; choosingDates = values[3] as Boolean
            } })
    }
}

@Composable
fun rememberRefuelHistoryFilter(today: LocalDate = LocalDate.now()): RefuelHistoryFilterState =
    rememberSaveable(saver = RefuelHistoryFilterState.Saver) { RefuelHistoryFilterState(today) }

data class RefuelCalendar(val today: LocalDate, val zone: ZoneId)

@Composable
fun rememberRefuelCalendar(): RefuelCalendar {
    fun current(): RefuelCalendar {
        val zone = ZoneId.systemDefault()
        return RefuelCalendar(LocalDate.now(zone), zone)
    }
    var calendar by remember { mutableStateOf(current()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { calendar = current() }
    LaunchedEffect(calendar) {
        val midnight = calendar.today.plusDays(1).atStartOfDay(calendar.zone).toInstant()
        delay(Duration.between(Instant.now(), midnight).toMillis().coerceAtLeast(1))
        calendar = current()
    }
    return calendar
}

private fun rangeDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", MexicanLocale))

@Composable
fun RefuelHistoryFilter(state: RefuelHistoryFilterState, range: RefuelDateRange, shown: Int, total: Int) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                FuelIcon(R.drawable.ic_filter_list)
                Text("Periodo: ${state.period.label}", modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                FuelIcon(R.drawable.ic_keyboard_arrow_down)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                RefuelHistoryPeriod.entries.forEach { period -> DropdownMenuItem(text = { Text(period.label) },
                    leadingIcon = { FuelIcon(if (period == RefuelHistoryPeriod.Custom) R.drawable.ic_category_calendar_month else R.drawable.ic_filter_list) },
                    onClick = {
                        expanded = false
                        if (period == RefuelHistoryPeriod.Custom) state.choosingDates = true else state.period = period
                    }) }
            }
        }
        Text("${rangeDate(range.start)} — ${rangeDate(range.endInclusive)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Mostrando $shown de $total llenados", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (state.choosingDates) RefuelDateRangeDialog(state.customRange, state::apply, { state.choosingDates = false })
}

@Composable
fun RefuelDateRangeDialog(initialRange: RefuelDateRange, onApply: (RefuelDateRange) -> Unit, onDismiss: () -> Unit) {
    // Material picker dates are UTC calendar days, not local instants.
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = initialRange.endInclusive.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val start = picker.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val end = picker.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(), topBar = {
            TopAppBar(title = { Text("Periodo personalizado") }, navigationIcon = {
                IconButton(onClick = onDismiss) { FuelIcon(R.drawable.ic_arrow_back, "Volver sin aplicar filtro") }
            })
        }, bottomBar = {
            Surface { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(enabled = start != null && end != null && !start.isAfter(end), modifier = Modifier.weight(1f),
                    onClick = { if (start != null && end != null) onApply(RefuelDateRange(start, end)) }) { Text("Aplicar filtro") }
            } }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { Text("Fecha de inicio", style = MaterialTheme.typography.labelLarge)
                        Text(start?.let(::rangeDate) ?: "Sin seleccionar", style = MaterialTheme.typography.bodyLarge) }
                    Column(Modifier.weight(1f)) { Text("Fecha de fin", style = MaterialTheme.typography.labelLarge)
                        Text(end?.let(::rangeDate) ?: "Sin seleccionar", style = MaterialTheme.typography.bodyLarge) }
                }
                TextButton(onClick = { picker.displayMode = if (picker.displayMode == DisplayMode.Picker) DisplayMode.Input else DisplayMode.Picker },
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp)) {
                    FuelIcon(if (picker.displayMode == DisplayMode.Picker) R.drawable.ic_edit else R.drawable.ic_category_calendar_month)
                    Spacer(Modifier.width(8.dp))
                    Text(if (picker.displayMode == DisplayMode.Picker) "Escribir fechas" else "Elegir en calendario")
                }
                DateRangePicker(picker, modifier = Modifier.fillMaxWidth().weight(1f), title = null, headline = null, showModeToggle = false)
            }
        }
    }
}
