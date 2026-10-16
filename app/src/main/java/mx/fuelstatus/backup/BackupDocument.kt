package mx.fuelstatus.backup

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import java.security.MessageDigest

data class BackupDocument(
    val createdAt: Long,
    val deviceId: String,
    val revision: Long,
    val vehicles: List<Vehicle>,
    val refuels: List<Refuel>,
    val expenses: List<Expense>,
    val appearance: Preferences,
)

data class LocalBackup(val document: BackupDocument, val bytes: ByteArray) {
    val id: String get() = sha256(bytes)
    val md5: String get() = digest("MD5", bytes)
}

fun digest(algorithm: String, bytes: ByteArray): String = MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }
fun sha256(bytes: ByteArray): String = digest("SHA-256", bytes)

object BackupCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    private val gson = Gson()

    fun encode(document: BackupDocument): ByteArray {
        val payload = gson.toJsonTree(document).asJsonObject
        validate(payload)
        val envelope = JsonObject().apply {
            addProperty("format", "fuelstatus-backup")
            addProperty("version", 2)
            addProperty("sha256", sha256(gson.toJson(payload).toByteArray(Charsets.UTF_8)))
            add("data", payload)
        }
        return gson.toJson(envelope).toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_BYTES) { "La copia supera el límite de 5 MB." }
        }
    }

    fun decode(bytes: ByteArray): BackupDocument {
        require(bytes.size <= MAX_BYTES) { "La copia supera el límite de 5 MB." }
        try {
            val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
            require(root.string("format") == "fuelstatus-backup") { "No es una copia de FuelStatus." }
            val version = root.long("version")
            require(version in 1L..2L) { "Esta copia necesita otra versión de FuelStatus." }
            val payload = root.getAsJsonObject("data") ?: error("Faltan los datos del respaldo.")
            require(root.string("sha256") == sha256(gson.toJson(payload).toByteArray(Charsets.UTF_8))) { "La copia está incompleta o su contenido fue modificado." }
            if (version == 1L) {
                payload.getAsJsonArray("vehicles")?.forEach { row ->
                    if (!row.asJsonObject.has("fuelType")) row.asJsonObject.addProperty("fuelType", FuelType.Magna.code)
                }
                payload.getAsJsonArray("refuels")?.forEach { row ->
                    if (!row.asJsonObject.has("fuelType")) row.asJsonObject.addProperty("fuelType", FuelType.Unspecified.code)
                }
            }
            validate(payload)
            return gson.fromJson(payload, BackupDocument::class.java)
        } catch (e: IllegalArgumentException) { throw e }
        catch (e: Exception) { throw IllegalArgumentException("El archivo de respaldo no tiene un formato válido.", e) }
    }

    private fun validate(data: JsonObject) {
        require(data.long("createdAt") > 0 && data.long("revision") > 0) { "Fecha o revisión de respaldo inválida." }
        require(data.string("deviceId").isNotBlank()) { "Falta el identificador de la copia." }
        fun rows(name: String): List<JsonObject> {
            val array = data.getAsJsonArray(name) ?: error("Falta $name.")
            require(array.size() <= 50_000) { "Demasiados registros en $name." }
            return array.map { it.asJsonObject }.also { rows ->
                val ids = rows.map { it.string("id") }
                require(ids.none(String::isBlank) && ids.size == ids.distinct().size) { "Identificadores inválidos o duplicados en $name." }
            }
        }
        val vehicles = rows("vehicles")
        val ids = vehicles.map { it.string("id") }.toSet()
        vehicles.forEach {
            require(it.string("name").isNotBlank()) { "Vehículo sin nombre." }
            it.string("type")
            require(FuelType.entries.any { fuel -> fuel.code == it.string("fuelType") }) { "Combustible del vehículo inválido." }
            require(it.long("year") in 1886..2100) { "Año del vehículo inválido." }
        }
        rows("refuels").forEach {
            require(it.string("vehicleId") in ids) { "Una carga hace referencia a un vehículo inexistente." }
            require(it.long("timestamp") > 0 && it.long("odometerMeters") >= 0 && it.long("volumeMl") > 0 && it.long("costCents") > 0) { "Carga con valores inválidos." }
            it.bool("fullTank"); it.bool("included"); it.bool("missedPrevious")
            it.string("station"); it.string("notes")
            require(FuelType.entries.any { fuel -> fuel.code == it.string("fuelType") }) { "Combustible del llenado inválido." }
        }
        rows("expenses").forEach {
            require(it.string("vehicleId") in ids) { "Un gasto hace referencia a un vehículo inexistente." }
            require(it.long("timestamp") > 0 && it.long("costCents") > 0 && it.string("category").isNotBlank()) { "Gasto con valores inválidos." }
            it.string("notes")
        }
        val appearance = data.getAsJsonObject("appearance") ?: error("Faltan los ajustes.")
        require(appearance.string("theme") in listOf("system", "light", "dark")) { "Tema de apariencia inválido." }
        appearance.bool("dynamicColor")
        require(appearance.string("vehicleId").let { it.isEmpty() || it in ids }) { "El vehículo seleccionado no existe." }
    }

    private fun JsonObject.string(name: String): String {
        val p = getAsJsonPrimitive(name)
        require(p != null && p.isString) { "Campo de texto inválido: $name." }
        return p.asString.also { require(it.length <= 100_000) { "Texto demasiado largo: $name." } }
    }
    private fun JsonObject.long(name: String): Long {
        val p = getAsJsonPrimitive(name)
        require(p != null && p.isNumber) { "Campo numérico inválido: $name." }
        return p.asBigDecimal.longValueExact()
    }
    private fun JsonObject.bool(name: String) {
        require(getAsJsonPrimitive(name)?.isBoolean == true) { "Campo booleano inválido: $name." }
    }
}
