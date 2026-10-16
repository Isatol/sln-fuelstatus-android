package mx.fuelstatus.data

enum class FuelType(val code: String, val label: String) {
    Magna("magna", "Magna · 87 octanos"),
    Premium("premium", "Premium · 91 octanos"),
    Diesel("diesel", "Diésel"),
    Unspecified("unspecified", "Sin especificar");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code } ?: Unspecified
        fun fromCsv(value: String): FuelType {
            val text = value.trim().lowercase()
            return when {
                text in listOf("magna", "regular", "87") || text.contains("magna") -> Magna
                text in listOf("premium", "91", "92", "93") || text.contains("premium") -> Premium
                text in listOf("diesel", "diésel") -> Diesel
                else -> Unspecified
            }
        }
    }
}

data class ImportPlan(val sourceVehicleId: String, val targetVehicleId: String? = null,
    val useVehicleFuel: Boolean = true, val newVehicleFuelType: String = FuelType.Magna.code,
    val paymentMethodId: String = PaymentMethod.Cash.id)

data class ImportPreview(val rows: List<FuelImportRow>) {
    val vehicles get() = rows.map { it.vehicle }.distinctBy { it.id }
}
