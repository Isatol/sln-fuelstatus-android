package mx.fuelstatus.backup

import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.*
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class BackupTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun document(revision: Long = 1) = BackupDocument(1_770_000_000_000, "phone", revision,
        listOf(Vehicle("car", "Mazda 3", "Hatchback", 2025)),
        listOf(Refuel("fill", "car", 1_760_000_000_000, 12345678, 34567, 98765, false, true, true, "Estación ñ", "Nota")),
        listOf(Expense("expense", "car", 1_760_000_000_000, "Servicio", 12345, "Aceite")), Preferences("dark", false, "car"))
    private fun backup(doc: BackupDocument = document()) = LocalBackup(doc, BackupCodec.encode(doc))

    @Test fun fullHistoryRoundTripsWithoutPrecisionLoss() {
        val original = document()
        assertEquals(original, BackupCodec.decode(BackupCodec.encode(original)))
        val text = BackupCodec.encode(original).toString(Charsets.UTF_8)
        assertFalse(text.contains("accessToken")); assertFalse(text.contains("email"))
    }
    @Test fun changedPayloadIsRejected() {
        val text = BackupCodec.encode(document()).toString(Charsets.UTF_8).replace("98765", "98766")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text.toByteArray()) }
    }
    @Test fun unsupportedVersionAndTruncatedFilesAreRejected() {
        val text = BackupCodec.encode(document()).toString(Charsets.UTF_8).replace("\"version\":3", "\"version\":9")
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(text.toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{bad".toByteArray()) }
    }
    @Test fun duplicateIdsAndMissingParentsAreRejectedBeforeWrite() {
        val doc = document()
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(vehicles = doc.vehicles + doc.vehicles)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(refuels = listOf(doc.refuels.first().copy(vehicleId = "missing")))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(refuels = listOf(doc.refuels.first().copy(volumeMl = 0)))) }
    }
    @Test fun missingRequiredFieldEvenWithCorrectChecksumIsRejected() {
        val root = JsonParser.parseString(BackupCodec.encode(document()).toString(Charsets.UTF_8)).asJsonObject
        val data = root.getAsJsonObject("data")
        data.getAsJsonArray("refuels")[0].asJsonObject.remove("fullTank")
        root.addProperty("sha256", sha256(data.toString().toByteArray()))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString().toByteArray()) }
    }
    @Test fun atomicFileReplacementAndRecoveryCopiesRemainReadable() {
        val directory = temporary.newFolder()
        val local = LocalBackupStore(directory)
        val first = backup(); val second = backup(document(2))
        local.write(first); local.preserveBeforeRestore(first); local.write(second)
        assertEquals(second.document, local.latest()!!.document)
        assertEquals(first.document, BackupCodec.decode(directory.listFiles()!!.first { it.name.startsWith("recovery-") }.readBytes()))
        assertFalse(directory.listFiles()!!.any { it.name.startsWith(".pending-") })
    }

    private inner class Source : BackupSource {
        var doc = document()
        override suspend fun reset() { doc = doc.copy(revision = 1, vehicles = emptyList(), refuels = emptyList(), expenses = emptyList(), appearance = Preferences(), paymentMethods = listOf(PaymentMethod.Cash)) }
        override suspend fun revision() = doc.revision
        override suspend fun snapshot(deviceId: String, now: Long) = doc.copy(deviceId = deviceId, createdAt = now)
        override suspend fun restore(document: BackupDocument) { doc = document.copy(revision = doc.revision + 1) }
        fun edit() { doc = doc.copy(revision = doc.revision + 1, expenses = doc.expenses + Expense("new-${doc.revision}", "car", 1_770_000_000_001, "Lavado", 5000)) }
    }
    private class Store : SyncStateStore {
        override val state = MutableStateFlow(SyncState())
        override fun update(transform: (SyncState) -> SyncState) { state.value = transform(state.value) }
    }
    private class Drive : DriveGateway {
        val files = linkedMapOf<String, Pair<RemoteBackup, ByteArray>>()
        var account = "test@example.com"
        var attempts = 0
        var failBefore = false
        var loseResponse = false
        var trashCount = 0
        var failTrashAfter: Int? = null
        var uploadStarted: CompletableDeferred<Unit>? = null
        var releaseUpload: CompletableDeferred<Unit>? = null
        var afterUpload: (() -> Unit)? = null
        override suspend fun email() = account
        override suspend fun folder() = "folder"
        override suspend fun backups(folderId: String) = files.values.map { it.first }.sortedByDescending { it.createdAt }
        override suspend fun upload(folderId: String, backup: LocalBackup): RemoteBackup {
            attempts++
            uploadStarted?.complete(Unit); releaseUpload?.await()
            if (failBefore) throw IOException("Sin red")
            val remote = RemoteBackup("file-$attempts", 1_780_000_000_000 + attempts, backup.id, backup.md5, backup.document.revision, "phone")
            files[remote.id] = remote to backup.bytes
            afterUpload?.invoke()
            if (loseResponse) { loseResponse = false; throw IOException("Conexión perdida tras subir") }
            return remote
        }
        override suspend fun download(id: String) = files.getValue(id).second
        override suspend fun trash(id: String) {
            if (failTrashAfter == trashCount) throw IOException("Drive no disponible")
            files.remove(id); trashCount++
        }
        override suspend fun delete(id: String) { files.remove(id) }
    }
    private inner class Fixture {
        val source = Source(); val store = Store(); val drive = Drive()
        val directory = temporary.newFolder(); val local = LocalBackupStore(directory)
        var online = true; var scheduled = 0; var canceled = false
        val engine = BackupEngine(source, local, store, "phone", { "token" }, { drive }, { online }, { scheduled++ }, { canceled = true }, now = { 1_770_000_000_000 })
        suspend fun connect() { engine.connect("token") }
    }
    @Test fun offlineSaveAlwaysCreatesLocalCopyAndQueuesLaterUpload() = runBlocking {
        val f = Fixture(); f.connect(); f.online = false; f.source.edit(); f.engine.afterMutation()
        assertEquals(2L, f.local.latest()!!.document.revision)
        assertEquals("offline", f.store.state.value.phase)
        assertEquals(0L, f.store.state.value.lastCloudAt)
        assertEquals(SyncOutcome.Retry, f.engine.synchronize())
        f.online = true
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
        assertEquals(2L, f.store.state.value.cloudRevision)
        assertTrue(f.store.state.value.lastCloudAt > 0)
    }
    @Test fun failedUploadKeepsLocalAndNeverClaimsCloudSuccess() = runBlocking {
        val f = Fixture(); f.connect(); f.drive.failBefore = true
        assertEquals(SyncOutcome.Retry, f.engine.synchronize())
        assertNotNull(f.local.latest()); assertEquals(0L, f.store.state.value.lastCloudAt)
        assertTrue(f.store.state.value.errorDetail.contains("Sin red"))
        assertTrue(f.store.state.value.pending)
    }
    @Test fun ambiguousUploadIsAcknowledgedWithoutDuplicateOnRetry() = runBlocking {
        val f = Fixture(); f.connect(); f.drive.loseResponse = true
        assertEquals(SyncOutcome.Retry, f.engine.synchronize())
        assertEquals(1, f.drive.files.size)
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
        assertEquals(1, f.drive.attempts)
        assertEquals("synced", f.store.state.value.phase)
        assertEquals("", f.store.state.value.errorTitle)
    }
    @Test fun editDuringUploadStaysPendingAndNextAttemptUploadsNewRevision() = runBlocking {
        val f = Fixture(); f.connect(); f.drive.afterUpload = { f.source.edit() }
        assertEquals(SyncOutcome.Retry, f.engine.synchronize())
        assertEquals(1L, f.store.state.value.cloudRevision)
        f.drive.afterUpload = null
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
        assertEquals(2L, f.store.state.value.cloudRevision)
        assertEquals(2, f.local.latest()!!.document.expenses.size)
    }
    @Test fun existingRemoteHistoryRequiresChoiceBeforeAnyUpload() = runBlocking {
        val f = Fixture(); f.drive.upload("folder", backup(document(9))); f.connect()
        assertTrue(f.store.state.value.needsChoice)
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
        assertEquals(1, f.drive.attempts)
        f.engine.useLocalHistory()
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
        assertEquals(2, f.drive.files.size)
    }
    @Test fun restorePreviewDoesNotMutateAndConfirmedRestorePreservesPreviousData() = runBlocking {
        val f = Fixture(); val remote = document(12).copy(expenses = emptyList())
        f.drive.upload("folder", backup(remote)); f.connect()
        val preview = f.engine.previewRestore()
        assertEquals(1, f.source.doc.expenses.size)
        f.engine.restore(preview)
        assertEquals(0, f.source.doc.expenses.size)
        val recovery = f.directory.listFiles()!!.first { it.name.startsWith("recovery-") }
        assertEquals(1, BackupCodec.decode(recovery.readBytes()).expenses.size)
        assertEquals(0, f.local.latest()!!.document.expenses.size)
        assertTrue(f.store.state.value.pending)
    }
    @Test fun retentionKeepsLatestFiveConfirmedCloudCopies() = runBlocking {
        val f = Fixture(); f.connect()
        repeat(8) { assertEquals(SyncOutcome.Success, f.engine.synchronize(true)); f.source.edit() }
        assertEquals(5, f.drive.files.size)
        assertTrue(f.drive.files.containsKey(f.store.state.value.baselineId))
    }
    @Test fun corruptLocalFileIsRegeneratedFromDatabaseBeforeUpload() = runBlocking {
        val f = Fixture(); f.connect(); File(f.directory, "latest.json").writeText("broken")
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
        assertEquals(f.source.doc.refuels, f.local.latest()!!.document.refuels)
    }
    @Test fun disconnectStopsQueueAndKeepsLocalCopy() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize(); f.engine.disconnect()
        assertTrue(f.canceled); assertFalse(f.store.state.value.connected); assertNotNull(f.local.latest())
        assertEquals(1, f.drive.files.size)
    }
    @Test fun rateLimitIsRetryableWhilePermissionFailureNeedsActionAndTokensAreRedacted() {
        fun response(reason: String) = DriveResponse(403, """{"error":{"message":"Failure secret-token Bearer abc123","errors":[{"reason":"$reason"}]}}""".toByteArray())
        val rate = DriveApi.failure(response("rateLimitExceeded"), "secret-token")
        assertTrue(rate.retryable); assertTrue(rate.detail.contains("HTTP 403"))
        assertFalse(rate.detail.contains("secret-token")); assertFalse(rate.detail.contains("abc123"))
        assertFalse(DriveApi.failure(response("insufficientPermissions")).retryable)
    }
    @Test fun differentAccountIsRejectedBeforeUploading() = runBlocking {
        val f = Fixture(); f.connect(); f.drive.account = "other@example.com"
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
        assertEquals("auth_required", f.store.state.value.phase)
        assertEquals(0, f.drive.attempts)
        assertEquals(0L, f.store.state.value.lastCloudAt)
    }
    @Test fun remoteChangeAfterSyncPausesFurtherUploads() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize()
        f.drive.upload("folder", backup(document(99).copy(deviceId = "other-phone")))
        f.source.edit(); f.engine.afterMutation()
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
        assertTrue(f.store.state.value.needsChoice)
        assertEquals(2, f.drive.attempts)
        assertEquals(2L, f.local.latest()!!.document.revision)
    }
    @Test fun missingPreviouslyConfirmedCopyRequiresReview() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize(); f.drive.files.clear(); f.source.edit()
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
        assertEquals(1, f.drive.attempts)
        assertTrue(f.store.state.value.errorTitle.contains("ya no está disponible"))
        f.connect()
        assertEquals(SyncOutcome.Success, f.engine.synchronize())
    }
    @Test fun damagedDownloadDoesNotReplaceHistory() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize()
        val id = f.store.state.value.baselineId
        val file = f.drive.files.getValue(id)
        f.drive.files[id] = file.first to "broken".toByteArray()
        try { f.engine.previewRestore(); fail("Expected checksum failure") } catch (_: IllegalArgumentException) { }
        assertEquals(1, f.source.doc.refuels.size)
        assertEquals(1L, f.source.doc.revision)
    }

    @Test fun reconnectingAnUpToDateAccountKeepsConfirmedSyncedStatus() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize()
        val scheduled = f.scheduled
        f.connect()
        assertEquals("synced", f.store.state.value.phase)
        assertFalse(f.store.state.value.pending)
        assertEquals(scheduled, f.scheduled)
    }

    @Test fun emptyFreshInstallBackupIsValid() {
        val empty = document().copy(vehicles = emptyList(), refuels = emptyList(), expenses = emptyList(), appearance = Preferences())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }
    @Test fun oldBackupsGainExplicitUnknownFuelWithoutChangingRecordedAmounts() {
        val root = JsonParser.parseString(BackupCodec.encode(document()).toString(Charsets.UTF_8)).asJsonObject
        root.addProperty("version", 1)
        val data = root.getAsJsonObject("data")
        data.getAsJsonArray("vehicles").forEach { it.asJsonObject.remove("fuelType") }
        data.getAsJsonArray("refuels").forEach { it.asJsonObject.remove("fuelType") }
        root.addProperty("sha256", sha256(data.toString().toByteArray()))
        val restored = BackupCodec.decode(root.toString().toByteArray())
        assertEquals(FuelType.Magna.code, restored.vehicles.single().fuelType)
        assertEquals(FuelType.Unspecified.code, restored.refuels.single().fuelType)
        assertEquals(document().refuels.single().costCents, restored.refuels.single().costCents)
    }
    @Test fun fuelChoicesSurviveBackupAndInvalidTypesAreRejected() {
        val doc = document().let { it.copy(vehicles = listOf(it.vehicles.single().copy(fuelType = "premium")),
            refuels = listOf(it.refuels.single().copy(fuelType = "diesel"))) }
        assertEquals(doc, BackupCodec.decode(BackupCodec.encode(doc)))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(vehicles = listOf(doc.vehicles.single().copy(fuelType = "invalid")))) }
    }
    @Test fun localResetWorksOfflineClearsRecoveryCopiesAndDisconnectsWithoutTouchingDrive() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize()
        f.local.preserveBeforeRestore(f.local.latest()!!)
        f.online = false
        f.engine.reset(false)
        assertTrue(f.canceled)
        assertFalse(f.store.state.value.connected)
        assertEquals(0L, f.store.state.value.lastCloudAt)
        assertEquals(1, f.drive.files.size)
        assertEquals(0, f.drive.trashCount)
        assertTrue(f.local.latest()!!.document.vehicles.isEmpty())
        assertEquals(Preferences(), f.source.doc.appearance)
        assertFalse(f.directory.listFiles()!!.any { it.name.startsWith("recovery-") })
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
    }
    @Test fun checkedDriveResetMovesAllAppCopiesToTrashThenClearsLocalHistory() = runBlocking {
        val f = Fixture(); f.connect(); repeat(3) { f.engine.synchronize(true) }
        f.engine.reset(true)
        assertEquals(3, f.drive.trashCount)
        assertTrue(f.drive.files.isEmpty())
        assertTrue(f.source.doc.refuels.isEmpty())
        assertFalse(f.store.state.value.connected)
    }
    @Test fun driveResetOfflinePreservesLocalDataAndBlocksAutomaticUploads() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize(); f.online = false
        try { f.engine.reset(true); fail("Expected network requirement") } catch (_: IllegalStateException) { }
        assertEquals(1, f.source.doc.refuels.size)
        assertEquals(1, f.drive.files.size)
        assertEquals("reset_failed", f.store.state.value.phase)
        f.online = true; f.source.edit(); f.engine.afterMutation()
        assertEquals(SyncOutcome.NeedsAction, f.engine.synchronize())
        f.engine.reset(true)
        assertTrue(f.source.doc.refuels.isEmpty())
    }
    @Test fun partialDriveFailureRetainsLocalDataAndReportsProgressForRetry() = runBlocking {
        val f = Fixture(); f.connect(); repeat(3) { f.engine.synchronize(true) }
        f.drive.failTrashAfter = 1
        try { f.engine.reset(true); fail("Expected failure") } catch (_: DriveFailure) { }
        assertEquals(1, f.source.doc.refuels.size)
        assertTrue(f.store.state.value.errorDetail.contains("1 de 3"))
        assertEquals(2, f.drive.files.size)
        f.drive.failTrashAfter = null
        f.engine.reset(true)
        assertTrue(f.drive.files.isEmpty()); assertTrue(f.source.doc.vehicles.isEmpty())
    }
    @Test fun resetRefusesAccountMismatchWithoutDeletingEitherHistory() = runBlocking {
        val f = Fixture(); f.connect(); f.engine.synchronize(); f.drive.account = "other@example.com"
        try { f.engine.reset(true); fail("Expected account mismatch") } catch (_: IllegalStateException) { }
        assertEquals(1, f.source.doc.refuels.size)
        assertEquals(1, f.drive.files.size); assertEquals(0, f.drive.trashCount)
    }
    @Test fun resetWaitsForInFlightUploadThenDeletesThatCopyToo() = runBlocking {
        val f = Fixture(); f.connect()
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        f.drive.uploadStarted = started; f.drive.releaseUpload = release
        val upload = async { f.engine.synchronize() }
        started.await()
        val reset = async { f.engine.reset(true) }
        yield(); assertFalse(reset.isCompleted)
        release.complete(Unit)
        upload.await(); reset.await()
        assertEquals(1, f.drive.trashCount)
        assertTrue(f.drive.files.isEmpty()); assertFalse(f.store.state.value.connected)
    }

}
