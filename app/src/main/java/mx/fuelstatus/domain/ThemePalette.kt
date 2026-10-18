package mx.fuelstatus.domain

enum class ThemePalette(val code: String, val label: String) {
    Green("green", "FuelStatus"), Blue("blue", "Azul"), Violet("violet", "Violeta"),
    Rose("rose", "Rosa"), Orange("orange", "Naranja"), Gray("gray", "Gris");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: Green
    }
}
