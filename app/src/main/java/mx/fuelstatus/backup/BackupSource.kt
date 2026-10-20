package mx.fuelstatus.backup

interface BackupSource {
    suspend fun revision(): Long
    suspend fun snapshot(deviceId: String, now: Long): BackupDocument
    suspend fun reset()
    suspend fun restore(document: BackupDocument)
}
