package mx.fuelstatus

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import mx.fuelstatus.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.fuelstatus.data.*
import java.io.ByteArrayOutputStream

data class Preferences(val theme: String = "system", val dynamicColor: Boolean = true, val vehicleId: String = "")
data class FuelState(val vehicles: List<Vehicle> = emptyList(), val refuels: List<Refuel> = emptyList(),
    val expenses: List<Expense> = emptyList(), val preferences: Preferences = Preferences(), val loaded: Boolean = false,
    val paymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash)) {
    val selectedVehicle get() = vehicles.firstOrNull { it.id == preferences.vehicleId } ?: vehicles.firstOrNull()
    val selectedRefuels get() = refuels.filter { it.vehicleId == selectedVehicle?.id }
    val selectedExpenses get() = expenses.filter { it.vehicleId == selectedVehicle?.id }
}

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = (application as FuelApplication).graph
    private val repository = graph.repository
    private val engine = graph.backup.engine
    private val preferences = graph.appearance.state
    val backupState = engine.state
    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview = _importPreview.asStateFlow()
    private val _restorePreview = MutableStateFlow<RestorePreview?>(null)
    val restorePreview = _restorePreview.asStateFlow()
    private val _driveBusy = MutableStateFlow(false)
    val driveBusy = _driveBusy.asStateFlow()
    val state = combine(repository.dao.vehicles(), repository.dao.refuels(), repository.dao.expenses(), preferences, repository.dao.paymentMethods()) { vehicles, refuels, expenses, preference, methods ->
        FuelState(vehicles, refuels, expenses, preference, loaded = true, paymentMethods = methods)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelState(preferences = preferences.value))
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init { work { repository.initialize(); engine.refreshLocal() } }

    fun onForeground() {
        if (_driveBusy.value || engine.state.value.phase == "resetting") return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initialize()
                engine.refreshLocal()
                val status = engine.state.value
                if (status.connected && status.pending && !status.running && !status.needsChoice &&
                    status.phase != "auth_required" && BackupRuntime.isOnline(getApplication())) {
                    if (engine.synchronize() == SyncOutcome.Retry) engine.requestSync()
                } else engine.requestSync()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { engine.failed(e) }
        }
    }
    private fun preference(change: (Preferences) -> Preferences) = work(changed = true) {
        graph.appearance.update(change)
        repository.settingsChanged()
    }
    fun selectVehicle(id: String) { preference { it.copy(vehicleId = id) } }
    fun theme(value: String) { preference { it.copy(theme = value) } }
    fun dynamicColor(value: Boolean) { preference { it.copy(dynamicColor = value) } }
    fun addPaymentMethod(method: PaymentMethod, onSuccess: () -> Unit) = work("Tarjeta guardada", onSuccess, true) { repository.addPaymentMethod(method) }
    fun save(refuel: Refuel, onSuccess: () -> Unit) = work("Llenado guardado", onSuccess, true) { repository.save(refuel) }
    fun save(expense: Expense, onSuccess: () -> Unit) = work("Gasto guardado", onSuccess, true) { repository.save(expense) }
    fun save(vehicle: Vehicle, onSuccess: () -> Unit) = work("Vehículo guardado", onSuccess, true) {
        repository.save(vehicle); graph.appearance.update { it.copy(vehicleId = vehicle.id) }
    }
    fun delete(refuel: Refuel, onSuccess: () -> Unit) = work("Llenado eliminado", onSuccess, true) { repository.delete(refuel) }
    fun delete(expense: Expense, onSuccess: () -> Unit) = work("Gasto eliminado", onSuccess, true) { repository.delete(expense) }

    suspend fun beginDriveAuthorization() = withContext(Dispatchers.IO) { engine.connecting() }
    fun driveAuthorizationCanceled() { viewModelScope.launch(Dispatchers.IO) { engine.canceled() } }
    fun driveAuthorizationFailed(error: Exception) { viewModelScope.launch(Dispatchers.IO) { engine.failed(GoogleDriveAuthorization.failure(error)) } }
    fun driveAuthorized(token: String) { driveAction { engine.connect(token) } }
    fun syncDrive() { driveAction {
        if (!BackupRuntime.isOnline(getApplication()) || engine.synchronize(force = true) == SyncOutcome.Retry)
            engine.requestSync(force = true)
    } }
    fun disconnectDrive() { driveAction { engine.disconnect() } }
    fun reviewDriveCopy() { driveAction { _restorePreview.value = engine.previewRestore() } }
    fun dismissRestore() { _restorePreview.value = null }
    fun useLocalHistory() { driveAction { engine.useLocalHistory() } }
    fun resetData(deleteDrive: Boolean, onSuccess: () -> Unit) {
        if (_driveBusy.value) return
        work("Datos eliminados", onSuccess) {
            engine.reset(deleteDrive)
            _restorePreview.value = null
            _importPreview.value = null
        }
    }
    fun confirmRestore() {
        val preview = _restorePreview.value ?: return
        work("Historial restaurado; copia local guardada", { _restorePreview.value = null }) { engine.restore(preview) }
    }
    private fun driveAction(block: suspend () -> Unit) {
        if (_driveBusy.value || _busy.value) return
        _driveBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { engine.failed(e) }
            finally { _driveBusy.value = false }
        }
    }

    fun prepareImport(uri: Uri) = work() {
        val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= FuelCsv.MAX_BYTES) { "El CSV supera el límite de 5 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("No se pudo abrir el archivo.")
        _importPreview.value = ImportPreview(FuelCsv.parse(bytes.toString(Charsets.UTF_8)))
    }

    fun dismissImport() { if (!_busy.value) _importPreview.value = null }
    fun confirmImport(plan: ImportPlan) {
        val preview = _importPreview.value ?: return
        work(onSuccess = { _importPreview.value = null }, changed = true) {
            val result = repository.import(preview.rows, plan)
            if (plan.targetVehicleId == null) {
                val source = preview.vehicles.first { it.id == plan.sourceVehicleId }
                repository.dao.allVehicles().firstOrNull {
                    it.name.equals(source.name, true) && it.type.equals(source.type, true) && it.year == source.year
                }?.let { vehicle ->
                    graph.appearance.update { it.copy(vehicleId = vehicle.id) }
                }
            } else graph.appearance.update { it.copy(vehicleId = plan.targetVehicleId) }
            repository.settingsChanged()
            _messages.emit("${result.imported} llenados importados · ${result.duplicates} duplicados omitidos")
        }
    }

    fun exportCsv(uri: Uri) = work("CSV de llenados exportado") {
        val text = FuelCsv.export(repository.dao.allVehicles(), repository.dao.allRefuels(), repository.dao.allPaymentMethods())
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
            ?: error("No se pudo guardar el archivo.")
    }

    private fun work(message: String? = null, onSuccess: (() -> Unit)? = null, changed: Boolean = false, block: suspend () -> Unit) {
        if (_busy.value || _driveBusy.value) return
        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try { block(); if (changed) engine.afterMutation(); onSuccess?.let { withContext(Dispatchers.Main) { it() } }; message?.let { _messages.emit(it) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _messages.emit(e.message ?: "No se pudo completar la operación.") }
            finally { _busy.value = false }
        }
    }
}
