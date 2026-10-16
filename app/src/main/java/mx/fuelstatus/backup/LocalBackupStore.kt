package mx.fuelstatus.backup

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Private, durable files. The previous latest file is kept until the new file has been flushed. */
class LocalBackupStore(private val directory: File) {
    fun latest(): LocalBackup? {
        val file = File(directory, "latest.json")
        if (!file.exists()) return null
        require(file.length() <= BackupCodec.MAX_BYTES) { "La copia local supera el límite de tamaño." }
        val bytes = file.readBytes()
        return LocalBackup(BackupCodec.decode(bytes), bytes)
    }

    fun write(backup: LocalBackup) = atomicWrite(File(directory, "latest.json"), backup.bytes)

    fun preserveBeforeRestore(backup: LocalBackup) {
        atomicWrite(File(directory, "recovery-${backup.document.createdAt}-${backup.id.take(12)}.json"), backup.bytes)
        directory.listFiles()?.filter { it.name.startsWith("recovery-") && it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        check(directory.isDirectory || directory.mkdirs()) { "No se pudo crear la carpeta de respaldo local." }
        val temporary = File(directory, ".pending-${UUID.randomUUID()}")
        try {
            FileOutputStream(temporary).use { stream -> stream.write(bytes); stream.fd.sync() }
            try {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { temporary.delete() }
    }
}
