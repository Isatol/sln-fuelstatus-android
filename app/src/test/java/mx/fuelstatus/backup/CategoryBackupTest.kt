package mx.fuelstatus.backup

import com.google.gson.JsonParser
import mx.fuelstatus.Preferences
import mx.fuelstatus.data.*
import org.junit.Assert.*
import org.junit.Test

class CategoryBackupTest {
    private val custom = ExpenseCategory("Llantas", CategoryIcon.Tire.code, 6)
    private fun document() = BackupDocument(1000, "phone", 12, listOf(Vehicle("car", "Auto", "Sedán", 2024)),
        emptyList(), listOf(Expense("expense", "car", 1000, custom.name, 12345, "Original")), Preferences(),
        expenseCategories = ExpenseCategory.Defaults + custom)

    @Test fun catalogAndAssignedExpensesRoundTripWithoutLosingAmounts() {
        val original = document()
        assertEquals(original, BackupCodec.decode(BackupCodec.encode(original)))
        assertEquals(5, JsonParser.parseString(BackupCodec.encode(original).toString(Charsets.UTF_8)).asJsonObject.get("version").asInt)
    }

    @Test fun versionThreeBackupBuildsCatalogFromOriginalExpenseNames() {
        val root = JsonParser.parseString(BackupCodec.encode(document()).toString(Charsets.UTF_8)).asJsonObject
        root.addProperty("version", 3)
        val data = root.getAsJsonObject("data")
        data.remove("expenseCategories")
        root.addProperty("sha256", sha256(data.toString().toByteArray(Charsets.UTF_8)))
        val restored = BackupCodec.decode(root.toString().toByteArray(Charsets.UTF_8))
        assertEquals(document().expenses, restored.expenses)
        assertTrue(restored.expenseCategories.containsAll(ExpenseCategory.Defaults))
        assertEquals(CategoryIcon.Tag.code, restored.expenseCategories.single { it.name == custom.name }.icon)
    }

    @Test fun deletedDefaultCategoriesStayAbsentAfterBackupRoundTrip() {
        val original = document().copy(expenseCategories = listOf(custom, ExpenseCategory.Other))
        assertEquals(original, BackupCodec.decode(BackupCodec.encode(original)))
        val onlyOther = original.copy(expenses = original.expenses.map { it.copy(category = ExpenseCategory.OTHER_NAME) },
            expenseCategories = listOf(ExpenseCategory.Other))
        assertEquals(onlyOther, BackupCodec.decode(BackupCodec.encode(onlyOther)))
    }

    @Test fun brokenReferencesDuplicateNamesAndInvalidIconsAreRejected() {
        val original = document()
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(original.copy(expenseCategories = ExpenseCategory.Defaults)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(original.copy(expenseCategories = original.expenseCategories + custom)) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(original.copy(expenseCategories = ExpenseCategory.Defaults + custom.copy(icon = "unknown"))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(original.copy(expenseCategories = listOf(custom))) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(original.copy(expenseCategories = ExpenseCategory.Defaults + custom.copy(sortOrder = -1))) }
    }

    @Test fun iconSearchUnderstandsSpanishAccentsAliasesAndCodes() {
        assertTrue(CategoryIcon.Tire.matches("neumático"))
        assertTrue(CategoryIcon.Shield.matches("SEGURO"))
        assertTrue(CategoryIcon.Battery.matches(" bateria "))
        assertTrue(CategoryIcon.Battery.matches("battery_charging"))
        assertFalse(CategoryIcon.Tire.matches("seguro"))
        assertEquals(CategoryIcon.entries.size, CategoryIcon.entries.count { it.matches("") })
    }
}
