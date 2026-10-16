package mx.fuelstatus.data

import androidx.room.*
import java.util.UUID

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Mazda 3",
    val type: String = "Hatchback",
    val year: Int = 2025,
) {
    @get:Ignore
    val label: String get() = "$name $type $year".trim()
}

@Entity(tableName = "refuels", foreignKeys = [ForeignKey(entity = Vehicle::class,
    parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class Refuel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val timestamp: Long,
    val odometerMeters: Long,
    val volumeMl: Long,
    val costCents: Long,
    val fullTank: Boolean = true,
    val included: Boolean = true,
    val missedPrevious: Boolean = false,
    val station: String = "",
    val notes: String = "",
) {
    @get:Ignore
    val liters get() = volumeMl / 1000.0
    @get:Ignore
    val km get() = odometerMeters / 1000.0
    @get:Ignore
    val cost get() = costCents / 100.0
    @get:Ignore
    val pricePerLiter get() = if (volumeMl > 0) cost / liters else 0.0
}

@Entity(tableName = "expenses", foreignKeys = [ForeignKey(entity = Vehicle::class,
    parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId: String,
    val timestamp: Long,
    val category: String,
    val costCents: Long,
    val notes: String = "",
)

@Entity(tableName = "backup_revision")
data class BackupRevision(@PrimaryKey val id: Int = 1, val revision: Long = 1)
