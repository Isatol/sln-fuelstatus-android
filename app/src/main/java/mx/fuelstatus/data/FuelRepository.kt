package mx.fuelstatus.data

import androidx.room.withTransaction

data class ImportResult(val imported: Int, val duplicates: Int)

class FuelRepository(val database: FuelDatabase) {
    val dao = database.dao()
    suspend fun initialize() = database.withTransaction {
        dao.initializeRevision(BackupRevision())
        if (dao.allVehicles().isEmpty()) dao.save(Vehicle(id = "mazda-3-2025"))
    }
    suspend fun save(value: Refuel) = change { dao.save(value) }
    suspend fun save(value: Vehicle) = change { dao.save(value) }
    suspend fun save(value: Expense) = change { dao.save(value) }
    suspend fun delete(value: Refuel) = change { dao.delete(value) }
    suspend fun delete(value: Expense) = change { dao.delete(value) }
    suspend fun settingsChanged() = change { }
    private suspend fun change(block: suspend () -> Unit) = database.withTransaction {
        dao.initializeRevision(BackupRevision()); block(); dao.changed()
    }
    suspend fun import(rows: List<FuelImportRow>): ImportResult = database.withTransaction {
        val vehicles = dao.allVehicles().toMutableList()
        val initialVehicleCount = vehicles.size
        val existing = dao.allRefuels()
        val signatures = existing.map(FuelCsv::signature).toMutableSet()
        val existingIds = existing.map { it.id }.toMutableSet()
        val pending = mutableListOf<Refuel>()
        for (row in rows) {
            val vehicle = vehicles.firstOrNull {
                it.name.equals(row.vehicle.name, true) && it.type.equals(row.vehicle.type, true) && it.year == row.vehicle.year
            } ?: row.vehicle.also { dao.save(it); vehicles.add(it) }
            val refuel = row.refuel.copy(vehicleId = vehicle.id)
            val signature = FuelCsv.signature(refuel)
            if (refuel.id !in existingIds && signatures.add(signature)) {
                pending.add(refuel); existingIds.add(refuel.id)
            }
        }
        val inserted = dao.insertRefuels(pending).count { it != -1L }
        if (inserted > 0 || vehicles.size != initialVehicleCount) { dao.initializeRevision(BackupRevision()); dao.changed() }
        ImportResult(inserted, rows.size - inserted)
    }
}
