package mx.fuelstatus.backup

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class DriveRequest(val method: String, val path: String, val query: Map<String, String> = emptyMap(),
    val body: ByteArray? = null, val contentType: String = "application/json; charset=UTF-8", val upload: Boolean = false)
data class DriveResponse(val code: Int, val bytes: ByteArray)
fun interface DriveTransport { fun execute(request: DriveRequest): DriveResponse }

class GoogleDriveTransport(private val token: String) : DriveTransport {
    override fun execute(request: DriveRequest): DriveResponse {
        val base = if (request.upload) "https://www.googleapis.com/upload/drive/v3/" else "https://www.googleapis.com/drive/v3/"
        val query = request.query.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        val connection = URL(base + request.path + if (query.isEmpty()) "" else "?$query").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = 15_000; connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            request.body?.let {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", request.contentType)
                connection.setFixedLengthStreamingMode(it.size)
                connection.outputStream.use { stream -> stream.write(it) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { readLimited(it, BackupCodec.MAX_BYTES) } ?: ByteArray(0)
            return DriveResponse(code, bytes)
        } finally { connection.disconnect() }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun readLimited(input: InputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            require(out.size() + size <= max) { "La respuesta de Drive supera el tamaño admitido." }
            out.write(buffer, 0, size)
        }
        return out.toByteArray()
    }
}

/** Small REST surface; the only scope required is drive.file. No tokens are persisted in backups. */
class DriveApi(private val transport: DriveTransport, private val sensitiveToken: String = "") : DriveGateway {
    private val fileFields = "id,createdTime,md5Checksum,appProperties"
    override suspend fun email(): String = json(DriveRequest("GET", "about", mapOf("fields" to "user(emailAddress)")))
        .getAsJsonObject("user").get("emailAddress").asString.also { require(it.isNotBlank()) { "Drive no devolvió la cuenta conectada." } }

    override suspend fun folder(): String {
        val query = "trashed = false and mimeType = 'application/vnd.google-apps.folder' and appProperties has { key='fuelstatusFolder' and value='1' }"
        val existing = json(DriveRequest("GET", "files", mapOf("q" to query, "fields" to "files(id)", "orderBy" to "createdTime", "pageSize" to "100")))
            .getAsJsonArray("files")
        if (existing.size() > 0) return existing.first().asJsonObject.get("id").asString
        val metadata = JsonObject().apply {
            addProperty("name", "FuelStatus"); addProperty("mimeType", "application/vnd.google-apps.folder")
            add("appProperties", JsonObject().apply { addProperty("fuelstatusFolder", "1") })
        }
        return json(DriveRequest("POST", "files", mapOf("fields" to "id"), metadata.toString().toByteArray(Charsets.UTF_8))).get("id").asString
    }

    override suspend fun backups(folderId: String): List<RemoteBackup> {
        val query = "'${id(folderId)}' in parents and trashed = false and appProperties has { key='fuelstatusBackup' and value='1' }"
        val results = mutableListOf<RemoteBackup>()
        var next = ""
        do {
            val params = mutableMapOf("q" to query, "fields" to "nextPageToken,files($fileFields)", "orderBy" to "createdTime desc", "pageSize" to "100")
            if (next.isNotEmpty()) params["pageToken"] = next
            val page = json(DriveRequest("GET", "files", params))
            page.getAsJsonArray("files").forEach { results += remote(it.asJsonObject) }
            next = page.get("nextPageToken")?.asString.orEmpty()
            require(results.size < 10_000) { "La carpeta contiene demasiadas copias. Revisa el destino en Drive." }
        } while (next.isNotEmpty())
        return results.sortedWith(compareByDescending<RemoteBackup> { it.createdAt }.thenByDescending { it.id })
    }

    override suspend fun upload(folderId: String, backup: LocalBackup): RemoteBackup {
        val stamp = Instant.ofEpochMilli(backup.document.createdAt).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val metadata = JsonObject().apply {
            addProperty("name", "FuelStatus-$stamp-${backup.id.take(8)}.json")
            addProperty("mimeType", "application/json")
            add("parents", com.google.gson.JsonArray().apply { add(id(folderId)) })
            add("appProperties", JsonObject().apply {
                addProperty("fuelstatusBackup", "1"); addProperty("clientBackupId", backup.id)
                addProperty("revision", backup.document.revision.toString()); addProperty("deviceId", backup.document.deviceId)
            })
        }
        val boundary = "fuelstatus-${UUID.randomUUID()}"
        val output = ByteArrayOutputStream()
        output.write(("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
            "--$boundary\r\nContent-Type: application/json\r\n\r\n").toByteArray(Charsets.UTF_8))
        output.write(backup.bytes)
        output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        return remote(json(DriveRequest("POST", "files", mapOf("uploadType" to "multipart", "fields" to fileFields),
            output.toByteArray(), "multipart/related; boundary=$boundary", upload = true)))
    }

    override suspend fun download(id: String): ByteArray = response(DriveRequest("GET", "files/${id(id)}", mapOf("alt" to "media"))).bytes
    override suspend fun delete(id: String) { response(DriveRequest("DELETE", "files/${id(id)}")) }

    private fun remote(json: JsonObject): RemoteBackup {
        val props = json.getAsJsonObject("appProperties") ?: JsonObject()
        return RemoteBackup(json.get("id").asString, Instant.parse(json.get("createdTime").asString).toEpochMilli(),
            props.get("clientBackupId")?.asString.orEmpty(), json.get("md5Checksum")?.asString.orEmpty(),
            props.get("revision")?.asString?.toLongOrNull() ?: 0, props.get("deviceId")?.asString.orEmpty())
    }
    private fun json(request: DriveRequest): JsonObject = JsonParser.parseString(response(request).bytes.toString(Charsets.UTF_8)).asJsonObject
    private fun response(request: DriveRequest): DriveResponse {
        val response = transport.execute(request)
        if (response.code !in 200..299) throw failure(response, sensitiveToken)
        return response
    }
    private fun id(value: String): String = value.also { require(it.matches(Regex("[A-Za-z0-9_-]{1,256}"))) { "Identificador de Drive inválido." } }

    companion object {
        fun failure(response: DriveResponse, token: String = ""): DriveFailure {
            val root = runCatching { JsonParser.parseString(response.bytes.toString(Charsets.UTF_8)).asJsonObject.getAsJsonObject("error") }.getOrNull()
            val reason = runCatching { root?.getAsJsonArray("errors")?.firstOrNull()?.asJsonObject?.get("reason")?.asString }.getOrNull().orEmpty()
            val message = runCatching { root?.get("message")?.asString }.getOrNull().orEmpty()
            val retry = response.code == 401 || response.code == 408 || response.code == 429 || response.code in 500..599 ||
                reason in listOf("rateLimitExceeded", "userRateLimitExceeded", "backendError")
            val title = when {
                response.code == 401 -> "Drive no aceptó la sesión; se volverá a obtener acceso"
                reason == "storageQuotaExceeded" -> "La cuenta de Drive no tiene espacio disponible"
                reason in listOf("accessNotConfigured", "SERVICE_DISABLED") -> "Falta habilitar Google Drive API en Google Cloud"
                response.code == 403 && !retry -> "Google Drive rechazó el permiso de acceso"
                response.code == 404 -> "No se encontró la carpeta o la copia en Drive"
                response.code == 429 || retry -> "Drive no está disponible temporalmente"
                else -> "No se pudo completar la operación con Drive"
            }
            var detail = "HTTP ${response.code}${if (reason.isBlank()) "" else " · $reason"}\n${message.ifBlank { "Google no devolvió más detalles." }}"
            if (token.isNotBlank()) detail = detail.replace(token, "[oculto]")
            detail = detail.replace(Regex("(?i)Bearer\\s+[^\\s\"]+"), "Bearer [oculto]").take(4000)
            return DriveFailure(title, detail, retryable = retry, httpCode = response.code)
        }
    }
}
