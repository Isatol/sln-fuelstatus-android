package mx.fuelstatus.backup

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import mx.fuelstatus.domain.IconRingColor
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
    @Test fun customPaletteRoundTripsWithBackupVersionFive() {
        val original = document().let { it.copy(appearance = it.appearance.copy(colorPalette = "violet")) }
        val bytes = BackupCodec.encode(original)
        assertEquals(5, JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject.get("version").asInt)
        assertEquals(original, BackupCodec.decode(bytes))
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encode(original.copy(appearance = original.appearance.copy(colorPalette = "unknown")))
        }
    }
    @Test fun oldBackupsKeepTheirDynamicChoiceAndDefaultToFuelStatusGreen() {
        for (dynamic in listOf(true, false)) {
            val original = document().let { it.copy(appearance = it.appearance.copy(dynamicColor = dynamic)) }
            val root = JsonParser.parseString(BackupCodec.encode(original).toString(Charsets.UTF_8)).asJsonObject
            val data = root.getAsJsonObject("data")
            data.getAsJsonObject("appearance").remove("colorPalette")
            root.addProperty("sha256", sha256(data.toString().toByteArray()))
            assertEquals(original, BackupCodec.decode(root.toString().toByteArray()))
        }
    }
    @Test fun iconRingColorsRoundTripWithoutChangingBackupFormatOrAppPalette() {
        for (color in IconRingColor.entries) {
            val original = document().let { it.copy(appearance = it.appearance.copy(colorPalette = "violet", iconRingColor = color.code)) }
            val bytes = BackupCodec.encode(original)
            assertEquals(5, JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject.get("version").asInt)
            assertEquals(original, BackupCodec.decode(bytes))
        }
    }
    @Test fun oldBackupWithoutIconChoiceUsesAndroidAndInvalidChoiceIsRejected() {
        val original = document()
        val root = JsonParser.parseString(BackupCodec.encode(original).toString(Charsets.UTF_8)).asJsonObject
        val data = root.getAsJsonObject("data")
        val appearance = data.getAsJsonObject("appearance")
        appearance.remove("iconRingColor")
        root.addProperty("sha256", sha256(data.toString().toByteArray()))
        assertEquals(original, BackupCodec.decode(root.toString().toByteArray()))
        for (invalid in listOf("unknown", "")) {
            appearance.addProperty("iconRingColor", invalid)
            root.addProperty("sha256", sha256(data.toString().toByteArray()))
            assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString().toByteArray()) }
        }
        appearance.add("iconRingColor", com.google.gson.JsonNull.INSTANCE)
        root.addProperty("sha256", sha256(data.toString().toByteArray()))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(root.toString().toByteArray()) }
    }
    @Test fun unsupportedVersionAndTruncatedFilesAreRejected() {
        val text = BackupCodec.encode(document()).toString(Charsets.UTF_8).replace("\"version\":5", "\"version\":9")
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
    private inner class Source : BackupSource {
        var doc = document()
        var rejectReset = false
        var snapshotStarted: CompletableDeferred<Unit>? = null
        var releaseSnapshot: CompletableDeferred<Unit>? = null
        override suspend fun revision() = doc.revision
        override suspend fun snapshot(deviceId: String, now: Long): BackupDocument {
            snapshotStarted?.complete(Unit)
            releaseSnapshot?.await()
            return doc.copy(deviceId = deviceId, createdAt = now)
        }
        override suspend fun reset() {
            check(!rejectReset) { "Synthetic reset failure" }
            doc = doc.copy(revision = 1, vehicles = emptyList(), refuels = emptyList(), expenses = emptyList(),
                appearance = Preferences(), paymentMethods = listOf(PaymentMethod.Cash), expenseCategories = ExpenseCategory.Defaults)
        }
        override suspend fun restore(document: BackupDocument) { doc = document.copy(revision = doc.revision + 1) }
        fun edit() { doc = doc.copy(revision = doc.revision + 1, expenses = doc.expenses + Expense("new-${doc.revision}", "car", 1_770_000_000_001, "Lavado", 5000)) }
    }

    private inner class Fixture {
        val source = Source()
        val directory = temporary.newFolder()
        val local = LocalBackupStore(directory)
        val engine = BackupEngine(source, local, "phone", now = { 1_770_000_000_000 })
    }

    @Test fun savedChangesUpdateTheCompleteLocalCopyAndUnchangedRevisionReusesIt() = runBlocking {
        val f = Fixture()
        val first = f.engine.refreshLocal()
        assertEquals(first.document, f.engine.refreshLocal().document)
        f.source.edit(); f.engine.afterMutation()
        assertEquals(f.source.doc, f.local.latest()!!.document)
        assertEquals(2L, f.local.latest()!!.document.revision)
    }
    @Test fun corruptLocalCopyIsRegeneratedFromTheDatabase() = runBlocking {
        val f = Fixture(); f.engine.refreshLocal()
        File(f.directory, "latest.json").writeText("incomplete")
        assertEquals(f.source.doc, f.engine.refreshLocal().document)
    }
    @Test fun failedAuxiliaryFileDoesNotReportCommittedDataAsFailed() = runBlocking {
        val source = Source(); source.edit()
        val file = temporary.newFile()
        val engine = BackupEngine(source, LocalBackupStore(file), "phone")
        engine.afterMutation()
        assertEquals(2L, source.doc.revision)
        assertEquals(2, source.doc.expenses.size)
    }
    @Test fun cancellationIsPropagatedInsteadOfReportedAsSuccessfulCopy() = runBlocking {
        val f = Fixture()
        f.source.releaseSnapshot = CompletableDeferred<Unit>().also { it.cancel() }
        try { f.engine.afterMutation(); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertNull(f.local.latest())
    }
    @Test fun confirmedLocalResetClearsHistoryRecoveryAndCustomCatalogs() = runBlocking {
        val f = Fixture()
        f.source.doc = f.source.doc.copy(paymentMethods = listOf(PaymentMethod.Cash, PaymentMethod("card", "Prueba", "visa")),
            expenseCategories = ExpenseCategory.Defaults + ExpenseCategory("Llantas", "tire_repair", 6))
        f.local.preserveBeforeRestore(f.engine.refreshLocal())
        f.engine.reset()
        val empty = f.local.latest()!!.document
        assertTrue(empty.vehicles.isEmpty()); assertTrue(empty.refuels.isEmpty()); assertTrue(empty.expenses.isEmpty())
        assertEquals(Preferences(), empty.appearance)
        assertEquals(listOf(PaymentMethod.Cash), empty.paymentMethods)
        assertEquals(ExpenseCategory.Defaults, empty.expenseCategories)
        assertFalse(f.directory.listFiles()!!.any { it.name.startsWith("recovery-") })
    }
    @Test fun failedDatabaseResetPreservesCurrentAndRecoveryCopies() = runBlocking {
        val f = Fixture(); val before = f.engine.refreshLocal()
        f.local.preserveBeforeRestore(before)
        f.source.rejectReset = true
        try { f.engine.reset(); fail("Expected reset failure") } catch (_: IllegalStateException) { }
        assertEquals(before.document, f.source.doc)
        assertEquals(before.document, f.local.latest()!!.document)
        assertTrue(f.directory.listFiles()!!.any { it.name.startsWith("recovery-") })
    }
    @Test fun resetWaitsForLocalSnapshotAndFinishesWithAnEmptyCopy() = runBlocking {
        val f = Fixture()
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        f.source.snapshotStarted = started; f.source.releaseSnapshot = release
        val copy = async { f.engine.refreshLocal() }; started.await()
        val reset = async { f.engine.reset() }; yield(); assertFalse(reset.isCompleted)
        release.complete(Unit); copy.await(); reset.await()
        assertTrue(f.local.latest()!!.document.vehicles.isEmpty())
    }
}
