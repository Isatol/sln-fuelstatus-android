package mx.fuelstatus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.Normalizer

fun categorySearchKey(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)

enum class CategoryIcon(val code: String, val label: String, val keywords: String) {
    Build("build", "Llave de taller", "servicio mecanica herramienta"),
    Settings("settings", "Engranaje", "mantenimiento motor"),
    Shield("verified_user", "Escudo", "seguro proteccion"),
    Parking("local_parking", "Estacionamiento", "parking"),
    Toll("toll", "Peaje", "caseta carretera"),
    Wash("local_car_wash", "Lavado", "limpieza agua"),
    More("more_horiz", "Más opciones", "otros"),
    Car("directions_car", "Auto", "vehiculo coche"),
    Battery("battery_charging_full", "Batería", "energia acumulador"),
    Tire("tire_repair", "Llanta", "rueda neumatico"),
    Oil("oil_barrel", "Aceite", "lubricante motor"),
    Speed("speed", "Medidor", "velocidad odometro"),
    Key("key", "Llave de auto", "duplicado"),
    Light("lightbulb", "Foco", "luces iluminacion"),
    Climate("ac_unit", "Climatización", "aire acondicionado frio"),
    Sparkles("auto_awesome", "Brillo", "encerado limpieza"),
    Paint("format_paint", "Pintura", "carroceria"),
    Fuel("local_gas_station", "Combustible", "gasolina diesel"),
    Receipt("receipt_long", "Recibo", "pago gasto factura"),
    Wallet("account_balance_wallet", "Cartera", "dinero"),
    Warning("warning", "Alerta", "multa emergencia"),
    Truck("local_shipping", "Camión", "carga grua"),
    Bike("two_wheeler", "Motocicleta", "moto transporte"),
    Location("location_on", "Ubicación", "viaje mapa"),
    Route("route", "Ruta", "camino recorrido"),
    Calendar("calendar_month", "Calendario", "revision fecha"),
    Inspection("fact_check", "Inspección", "verificacion revision"),
    Part("inventory_2", "Refacción", "pieza repuesto paquete"),
    Tag("sell", "Etiqueta", "categoria"),
    Tools("handyman", "Herramientas", "reparacion taller"),
    ;

    fun matches(query: String): Boolean = categorySearchKey("$label $keywords $code").contains(categorySearchKey(query))
    companion object { fun fromCode(code: String) = entries.firstOrNull { it.code == code } }
}

@Entity(tableName = "expense_categories")
data class ExpenseCategory(@PrimaryKey val name: String, val icon: String, val sortOrder: Int) {
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val OTHER_NAME = "Otros"
        val Other = ExpenseCategory(OTHER_NAME, CategoryIcon.More.code, Int.MAX_VALUE)
        val Defaults = listOf(
            ExpenseCategory("Servicio", CategoryIcon.Build.code, 0),
            ExpenseCategory("Mantenimiento", CategoryIcon.Settings.code, 1),
            ExpenseCategory("Seguro", CategoryIcon.Shield.code, 2),
            ExpenseCategory("Estacionamiento", CategoryIcon.Parking.code, 3),
            ExpenseCategory("Peaje", CategoryIcon.Toll.code, 4),
            ExpenseCategory("Lavado", CategoryIcon.Wash.code, 5),
            Other,
        )

        // Legacy expenses store the category name. Preserve that name exactly, even for older custom values.
        fun forLegacyExpenses(names: List<String>): List<ExpenseCategory> = Defaults +
            names.distinct().filter { name -> Defaults.none { it.name == name } }.sorted().mapIndexed { index, name ->
                ExpenseCategory(name, CategoryIcon.Tag.code, 6 + index)
            }
    }
}
