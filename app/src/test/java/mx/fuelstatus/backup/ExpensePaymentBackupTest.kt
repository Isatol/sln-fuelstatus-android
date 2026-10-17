package mx.fuelstatus.backup

import com.google.gson.JsonParser
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test

class ExpensePaymentBackupTest {
    private val card = PaymentMethod("visa", "Visa personal", "visa")
    private fun document() = BackupDocument(1000, "phone", 12, listOf(Vehicle("car", "Auto", "Sedán", 2024)),
        emptyList(), listOf(
            Expense("card-expense", "car", 1000, "Servicio", 12345, "Original", card.id),
            Expense("cash-expense", "car", 1001, "Peaje", 5000, paymentMethodId = PaymentMethod.Cash.id),
            Expense("old-expense", "car", 1002, "Otros", 2500),
        ), Preferences(), listOf(PaymentMethod.Cash, card))

    @Test fun expensesKeepCardCashAndUnknownPaymentsInCompleteBackup() {
        val original = document()
        val bytes = BackupCodec.encode(original)
        assertEquals(5, JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject.get("version").asInt)
        assertEquals(original, BackupCodec.decode(bytes))
    }

    @Test fun formatsOneThroughFourDoNotGuessPaymentsForHistoricalExpenses() {
        for (version in 1..4) {
            val root = JsonParser.parseString(BackupCodec.encode(document()).toString(Charsets.UTF_8)).asJsonObject
            root.addProperty("version", version)
            val data = root.getAsJsonObject("data")
            data.getAsJsonArray("expenses").forEach { it.asJsonObject.remove("paymentMethodId") }
            if (version == 1) data.getAsJsonArray("vehicles").forEach { it.asJsonObject.remove("fuelType") }
            if (version < 3) data.remove("paymentMethods")
            if (version < 4) data.remove("expenseCategories")
            root.addProperty("sha256", sha256(data.toString().toByteArray(Charsets.UTF_8)))
            val restored = BackupCodec.decode(root.toString().toByteArray(Charsets.UTF_8))
            assertEquals(document().expenses.map { it.copy(paymentMethodId = null) }, restored.expenses)
        }
    }

    @Test fun danglingAndInvalidExpensePaymentReferencesAreRejected() {
        val original = document()
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encode(original.copy(paymentMethods = listOf(PaymentMethod.Cash)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encode(original.copy(expenses = listOf(original.expenses.first().copy(paymentMethodId = ""))))
        }
    }
}
