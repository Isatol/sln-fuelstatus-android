package mx.fuelstatus.backup

import kotlinx.coroutines.flow.StateFlow

data class SyncState(
    val email: String = "",
    val folderId: String = "",
    val phase: String = "disconnected",
    val lastLocalAt: Long = 0,
    val localRevision: Long = 0,
    val lastCloudAt: Long = 0,
    val cloudRevision: Long = 0,
    val baselineId: String = "",
    val lastAttemptAt: Long = 0,
    val errorTitle: String = "",
    val errorDetail: String = "",
    val errorAt: Long = 0,
    val localError: String = "",
    val candidateId: String = "",
    val candidateAt: Long = 0,
    val pendingUploadId: String = "",
    val pendingUploadMd5: String = "",
    val pendingUploadRevision: Long = 0,
) {
    val connected get() = email.isNotBlank()
    val pending get() = localRevision > cloudRevision
    val needsChoice get() = candidateId.isNotBlank()
    val running get() = phase in listOf("connecting", "uploading", "restoring")
}

interface SyncStateStore {
    val state: StateFlow<SyncState>
    fun update(transform: (SyncState) -> SyncState)
}

interface BackupSource {
    suspend fun revision(): Long
    suspend fun snapshot(deviceId: String, now: Long): BackupDocument
    suspend fun restore(document: BackupDocument)
}

data class RemoteBackup(
    val id: String,
    val createdAt: Long,
    val clientId: String,
    val md5: String,
    val revision: Long,
    val deviceId: String,
)

interface DriveGateway {
    suspend fun email(): String
    suspend fun folder(): String
    suspend fun backups(folderId: String): List<RemoteBackup>
    suspend fun upload(folderId: String, backup: LocalBackup): RemoteBackup
    suspend fun download(id: String): ByteArray
    suspend fun delete(id: String)
}

class DriveFailure(val title: String, val detail: String, val retryable: Boolean = false, val authRequired: Boolean = false, val httpCode: Int? = null) : Exception(title)
enum class SyncOutcome { Success, Retry, NeedsAction }
data class RestorePreview(val email: String, val file: RemoteBackup, val document: BackupDocument)
