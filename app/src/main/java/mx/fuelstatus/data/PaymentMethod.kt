package mx.fuelstatus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PaymentBrand(val code: String, val label: String) {
    Cash("cash", "Efectivo"),
    Visa("visa", "VISA"),
    Mastercard("mastercard", "Mastercard"),
    Amex("amex", "American Express");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code }
    }
}

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String,
) {
    companion object {
        const val MAX_NAME_LENGTH = 80
        val Cash = PaymentMethod("cash", "Efectivo", PaymentBrand.Cash.code)
    }
}
