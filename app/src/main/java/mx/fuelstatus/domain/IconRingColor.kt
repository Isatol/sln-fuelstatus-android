package mx.fuelstatus.domain

enum class IconRingColor(val code: String, val label: String) {
    Android("android", "Android"), Green("green", "Verde FuelStatus"), Blue("blue", "Azul"),
    Violet("violet", "Violeta"), Rose("rose", "Rosa"), Orange("orange", "Naranja"), Gray("gray", "Gris");

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: Android
    }
}
