package mx.fuelstatus.data

import androidx.room.withTransaction
import java.util.UUID

data class ImportResult(val imported: Int, val duplicates: Int)

class FuelRepository(val database: FuelDatabase) {
    val dao = database.dao()
    suspend fun initialize() = database.withTransaction {
        dao.initializeRevision(BackupRevision())
        dao.initializeCash(PaymentMethod.Cash)
    }
    suspend fun addPaymentMethod(value: PaymentMethod) = change {
        require(value.id.isNotBlank() && value.id != PaymentMethod.Cash.id) { "Identificador de tarjeta inválido." }
        require(PaymentBrand.fromCode(value.brand)?.let { it != PaymentBrand.Cash } == true) { "Elige VISA, Mastercard o American Express." }
        val name = value.name.trim()
        require(name.isNotEmpty() && name.length <= PaymentMethod.MAX_NAME_LENGTH) { "Escribe un nombre de hasta ${PaymentMethod.MAX_NAME_LENGTH} caracteres." }
        require(dao.allPaymentMethods().none { it.brand == value.brand && it.name.equals(name, true) }) { "Ya tienes una tarjeta de esa marca con ese nombre." }
        dao.insertPaymentMethod(value.copy(name = name))
    }
    suspend fun save(value: Refuel) = change { dao.save(value) }
    suspend fun save(value: Vehicle) = change { dao.save(value) }
    suspend fun save(value: Expense) = change { dao.save(value) }
    suspend fun delete(value: Refuel) = change { dao.delete(value) }
    suspend fun delete(value: Expense) = change { dao.delete(value) }
    suspend fun reset() = database.withTransaction {
        dao.deleteAllRefuels(); dao.deleteAllExpenses(); dao.deleteAllVehicles(); dao.deleteRevision()
        dao.deleteAllPaymentMethods(); dao.initializeCash(PaymentMethod.Cash)
        dao.initializeRevision(BackupRevision())
    }
    suspend fun settingsChanged() = change { }
    private suspend fun change(block: suspend () -> Unit) = database.withTransaction {
        dao.initializeRevision(BackupRevision()); block(); dao.changed()
    }
    suspend fun import(rows: List<FuelImportRow>, plan: ImportPlan? = null): ImportResult = database.withTransaction {
        val selected = if (plan == null) rows else rows.filter { it.vehicle.id == plan.sourceVehicleId }
        require(selected.isNotEmpty()) { "No hay llenados para el vehículo elegido." }
        require(plan == null || FuelType.entries.any { it.code == plan.newVehicleFuelType }) { "Combustible inválido." }
        val vehicles = dao.allVehicles().toMutableList()
        val methods = dao.allPaymentMethods().toMutableList()
        require(plan == null || methods.any { it.id == plan.paymentMethodId }) { "El método de pago elegido ya no existe. Vuelve a seleccionarlo." }
        if (plan != null && plan.targetVehicleId == null) {
            val source = selected.first().vehicle
            require(vehicles.none { it.name.equals(source.name, true) && it.type.equals(source.type, true) && it.year == source.year }) {
                "Ese vehículo ya está registrado. Elige el vehículo de destino antes de importar."
            }
        }
        val initialVehicleCount = vehicles.size
        val existing = dao.allRefuels()
        val signatures = existing.map(FuelCsv::signature).toMutableSet()
        val existingIds = existing.map { it.id }.toMutableSet()
        val existingVehicleById = existing.associate { it.id to it.vehicleId }
        val pending = mutableListOf<Refuel>()
        for (row in selected) {
            val vehicle = if (plan?.targetVehicleId != null) vehicles.firstOrNull { it.id == plan.targetVehicleId }
                ?: error("El vehículo de destino ya no existe.") else vehicles.firstOrNull {
                it.name.equals(row.vehicle.name, true) && it.type.equals(row.vehicle.type, true) && it.year == row.vehicle.year
            } ?: row.vehicle.copy(id = UUID.randomUUID().toString(), fuelType = plan?.newVehicleFuelType ?: row.vehicle.fuelType).also { dao.save(it); vehicles.add(it) }
            val mapped = row.refuel.copy(vehicleId = vehicle.id,
                fuelType = if (plan?.useVehicleFuel == true) vehicle.fuelType else row.refuel.fuelType)
            val refuel = mapped.copy(id = "fuel-${FuelCsv.signature(mapped)}")
            val signature = FuelCsv.signature(refuel)
            // Earlier versions kept the CSV ID even when mapping to a different vehicle.
            // Keep those records recognizable after the user edits their values.
            val legacyDuplicate = existingVehicleById[row.refuel.id] == vehicle.id
            if (!legacyDuplicate && refuel.id !in existingIds && signatures.add(signature)) {
                val methodId = if (plan != null) plan.paymentMethodId else row.paymentMethod?.let { imported ->
                    methods.firstOrNull { it.brand == imported.brand && it.name.equals(imported.name, true) }
                        ?: imported.copy(id = if (imported.brand == PaymentBrand.Cash.code) PaymentMethod.Cash.id else UUID.randomUUID().toString())
                            .also { dao.insertPaymentMethod(it); methods.add(it) }
                }?.id
                pending.add(refuel.copy(paymentMethodId = methodId)); existingIds.add(refuel.id)
            }
        }
        val inserted = dao.insertRefuels(pending).count { it != -1L }
        if (inserted > 0 || vehicles.size != initialVehicleCount) { dao.initializeRevision(BackupRevision()); dao.changed() }
        ImportResult(inserted, selected.size - inserted)
    }
}
