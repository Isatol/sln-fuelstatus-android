package mx.fuelstatus.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.Instant

data class FuelImportRow(val vehicle: Vehicle, val refuel: Refuel, val paymentMethod: PaymentMethod? = null)

object FuelCsv {
    const val MAX_BYTES = 5 * 1024 * 1024

    /** RFC 4180 fields, escaped quotes, CRLF, BOM, and semicolon exports. Reject incomplete files. */
    fun parse(text: String): List<FuelImportRow> {
        require(text.length <= MAX_BYTES) { "El CSV supera el límite de 5 MB." }
        val clean = text.removePrefix("\uFEFF")
        val firstLine = clean.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val rows = readRows(clean, delimiter).filterNot { row -> row.all(String::isBlank) }
        require(rows.isNotEmpty()) { "El archivo está vacío." }
        val headers = rows.first().map { it.trim() }
        val required = listOf("carName", "dateISO8601", "amount", "price", "meters", "fullFillup")
        require(headers.containsAll(required)) { "No es un CSV de llenados de Fuel. Faltan: ${required.filterNot { it in headers }.joinToString()}." }
        val result = rows.drop(1).mapIndexed { index, values ->
            try {
                require(values.size == headers.size) { "Número de columnas incorrecto." }
                val row = headers.zip(values).toMap()
                fun value(name: String) = row[name].orEmpty().trim()
                fun decimal(name: String) = value(name).replace(',', '.').toBigDecimal()
                fun scaled(value: BigDecimal, scale: Int): Long = value.movePointRight(scale).setScale(0, RoundingMode.HALF_UP).longValueExact()
                fun bool(name: String, default: Boolean): Boolean = when (value(name).lowercase()) {
                    "true", "1", "yes" -> true
                    "false", "0", "no" -> false
                    "" -> default
                    else -> error("Valor inválido en $name.")
                }
                val carName = value("carName")
                require(carName.isNotBlank()) { "El vehículo no tiene nombre." }
                require(!bool("carIsEV", false)) { "Esta versión importa vehículos de combustible." }
                val year = value("carYear").toIntOrNull() ?: 2025
                require(year in 1886..2100) { "Año del vehículo inválido." }
                val vehicle = Vehicle(id = digest("$carName|${value("carType")}|$year"), name = carName, type = value("carType"), year = year,
                    fuelType = FuelType.fromCsv(value("vehicleFuelType")).takeIf { it != FuelType.Unspecified }?.code ?: FuelType.Magna.code)
                val amount = decimal("amount")
                val price = decimal("price")
                val odo = decimal("meters")
                require(amount > BigDecimal.ZERO && price > BigDecimal.ZERO && odo >= BigDecimal.ZERO) { "Litros, precio o kilometraje inválidos." }
                val paymentName = value("paymentMethodName")
                val paymentBrand = value("paymentMethodBrand").lowercase()
                val method = when {
                    paymentName.isEmpty() && paymentBrand.isEmpty() -> null
                    paymentBrand == PaymentBrand.Cash.code -> PaymentMethod.Cash
                    else -> {
                        require(PaymentBrand.fromCode(paymentBrand)?.let { it != PaymentBrand.Cash } == true &&
                            paymentName.isNotEmpty() && paymentName.length <= PaymentMethod.MAX_NAME_LENGTH) { "Método de pago inválido." }
                        PaymentMethod("payment-${digest("$paymentBrand|${paymentName.lowercase()}")}", paymentName, paymentBrand)
                    }
                }
                val refuel = Refuel(vehicleId = vehicle.id, timestamp = Instant.parse(value("dateISO8601")).toEpochMilli(),
                    odometerMeters = scaled(odo, 3), volumeMl = scaled(amount, 3), costCents = scaled(amount * price, 2),
                    fullTank = bool("fullFillup", true), included = bool("showInStatistic", true),
                    missedPrevious = bool("missedPrevious", false),
                    station = value("gasStation"), notes = value("comment"),
                    fuelType = FuelType.fromCsv(value("fuelType").ifBlank { value("octane") }).code,
                    paymentMethodId = method?.id)
                require(refuel.volumeMl > 0 && refuel.costCents > 0) { "Importe o volumen menor a la precisión admitida." }
                FuelImportRow(vehicle, refuel.copy(id = "fuel-${signature(refuel)}"), method)
            } catch (e: Exception) {
                throw IllegalArgumentException("Fila ${index + 2}: ${e.message ?: "dato inválido"}", e)
            }
        }
        require(result.isNotEmpty()) { "El CSV no contiene llenados." }
        return result
    }

    fun signature(row: Refuel): String = digest("${row.vehicleId}|${row.timestamp}|${row.odometerMeters}|${row.volumeMl}|${row.costCents}")
    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun readRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                    else { quoted = false; closed = true }
                } else field.append(c)
            } else when (c) {
                '"' -> { require(field.isEmpty() && !closed) { "Comillas inválidas en el CSV." }; quoted = true }
                delimiter -> { fields.add(field.toString()); field.clear(); closed = false }
                '\r', '\n' -> {
                    fields.add(field.toString()); field.clear(); rows.add(fields); fields = mutableListOf(); closed = false
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> { require(!closed) { "Texto después de cerrar comillas." }; field.append(c) }
            }
            i++
        }
        require(!quoted) { "El CSV tiene comillas sin cerrar." }
        if (field.isNotEmpty() || fields.isNotEmpty() || closed) { fields.add(field.toString()); rows.add(fields) }
        return rows
    }

    fun export(vehicles: List<Vehicle>, refuels: List<Refuel>, paymentMethods: List<PaymentMethod> = listOf(PaymentMethod.Cash)): String {
        val header = listOf("carName", "carType", "carYear", "carIsEV", "dateISO8601", "amount", "price", "meters", "fullFillup", "showInStatistic", "comment", "gasStation", "missedPrevious", "fuelType", "vehicleFuelType", "paymentMethodName", "paymentMethodBrand")
        val methodsById = paymentMethods.associateBy { it.id }
        fun escape(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${s.replace("\"", "\"\"")}\"" else s
        return buildString {
            append(header.joinToString(",")).append("\r\n")
            for (r in refuels.sortedBy { it.timestamp }) {
                val v = vehicles.first { it.id == r.vehicleId }
                val method = r.paymentMethodId?.let { methodsById[it] ?: error("No se encontró el método de pago de un llenado.") }
                // Enough precision to reconstruct the same total to the cent when importing.
                val price = BigDecimal(r.costCents).movePointLeft(2).divide(BigDecimal(r.volumeMl).movePointLeft(3), 12, RoundingMode.HALF_UP)
                append(listOf(v.name, v.type, v.year.toString(), "false", Instant.ofEpochMilli(r.timestamp).toString(),
                    BigDecimal(r.volumeMl).movePointLeft(3).toPlainString(), price.toPlainString(),
                    BigDecimal(r.odometerMeters).movePointLeft(3).toPlainString(), r.fullTank.toString(), r.included.toString(), r.notes, r.station, r.missedPrevious.toString(), r.fuelType, v.fuelType,
                    method?.name.orEmpty(), method?.brand.orEmpty())
                    .joinToString(",") { escape(it) }).append("\r\n")
            }
        }
    }
}
