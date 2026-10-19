package mx.fuelstatus.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Maintains private local copies using the existing backup format. */
class BackupEngine(
    private val source: BackupSource,
    private val local: LocalBackupStore,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun refreshLocal(): LocalBackup = mutex.withLock { refreshLocked() }

    private suspend fun refreshLocked(): LocalBackup {
        val revision = source.revision()
        val previous = runCatching { local.latest() }.getOrNull()
        return previous?.takeIf { it.document.revision == revision && it.document.deviceId == deviceId }
            ?: source.snapshot(deviceId, now()).let { LocalBackup(it, BackupCodec.encode(it)) }.also(local::write)
    }

    /** A failed auxiliary copy must not report a committed database save as failed. */
    suspend fun afterMutation() {
        try { refreshLocal() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { }
    }

    /** Invoked only after the user confirms deleting local data. */
    suspend fun reset() = mutex.withLock {
        source.reset()
        local.clear()
        refreshLocked()
        Unit
    }
}
