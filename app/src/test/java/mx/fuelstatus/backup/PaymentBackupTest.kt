package mx.fuelstatus.backup

import com.google.gson.JsonParser
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test

class PaymentBackupTest {
    private val card = PaymentMethod("visa", "Visa personal", "visa")
    private fun document() = BackupDocument(1000, "phone", 8,
        listOf(Vehicle("car", "Auto", "Sedán", 2024)),
        listOf(Refuel("fill", "car", 1000, 1000000, 40000, 100000, paymentMethodId = card.id)),
        emptyList(), Preferences(), listOf(PaymentMethod.Cash, card))

    @Test fun paymentMethodsAndReferencesRoundTripWithHistory() {
        val doc = document()
        assertEquals(doc, BackupCodec.decode(BackupCodec.encode(doc)))
        assertEquals("visa", BackupCodec.decode(BackupCodec.encode(doc)).refuels.single().paymentMethodId)
    }

    @Test fun oldVersionTwoBackupGainsCashWithoutGuessingHistoricalPayment() {
        val root = JsonParser.parseString(BackupCodec.encode(document()).toString(Charsets.UTF_8)).asJsonObject
        root.addProperty("version", 2)
        val data = root.getAsJsonObject("data")
        data.remove("paymentMethods")
        data.getAsJsonArray("refuels").forEach { it.asJsonObject.remove("paymentMethodId") }
        root.addProperty("sha256", sha256(data.toString().toByteArray(Charsets.UTF_8)))
        val migrated = BackupCodec.decode(root.toString().toByteArray(Charsets.UTF_8))
        assertEquals(listOf(PaymentMethod.Cash), migrated.paymentMethods)
        assertNull(migrated.refuels.single().paymentMethodId)
        assertEquals(100000L, migrated.refuels.single().costCents)
    }

    @Test fun missingPaymentReferencesInvalidBrandsAndDuplicateMethodsAreRejected() {
        val doc = document()
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(paymentMethods = listOf(PaymentMethod.Cash))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(paymentMethods = doc.paymentMethods + card)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(paymentMethods = listOf(card))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(doc.copy(paymentMethods = listOf(PaymentMethod.Cash, card.copy(brand = "invalid")))) }
    }
}
