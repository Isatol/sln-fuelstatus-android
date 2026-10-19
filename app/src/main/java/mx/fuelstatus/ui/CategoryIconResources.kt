package mx.fuelstatus.ui

import mx.fuelstatus.R

fun categoryIconResource(code: String): Int = when (code) {
    "build" -> R.drawable.ic_category_build
    "settings" -> R.drawable.ic_category_settings
    "verified_user" -> R.drawable.ic_category_verified_user
    "local_parking" -> R.drawable.ic_category_local_parking
    "toll" -> R.drawable.ic_category_toll
    "local_car_wash" -> R.drawable.ic_category_local_car_wash
    "more_horiz" -> R.drawable.ic_category_more_horiz
    "directions_car" -> R.drawable.ic_category_directions_car
    "battery_charging_full" -> R.drawable.ic_category_battery_charging_full
    "tire_repair" -> R.drawable.ic_category_tire_repair
    "oil_barrel" -> R.drawable.ic_category_oil_barrel
    "speed" -> R.drawable.ic_category_speed
    "key" -> R.drawable.ic_category_key
    "lightbulb" -> R.drawable.ic_category_lightbulb
    "ac_unit" -> R.drawable.ic_category_ac_unit
    "auto_awesome" -> R.drawable.ic_category_auto_awesome
    "format_paint" -> R.drawable.ic_category_format_paint
    "local_gas_station" -> R.drawable.ic_category_local_gas_station
    "receipt_long" -> R.drawable.ic_category_receipt_long
    "account_balance_wallet" -> R.drawable.ic_category_account_balance_wallet
    "warning" -> R.drawable.ic_category_warning
    "local_shipping" -> R.drawable.ic_category_local_shipping
    "two_wheeler" -> R.drawable.ic_category_two_wheeler
    "location_on" -> R.drawable.ic_category_location_on
    "route" -> R.drawable.ic_category_route
    "calendar_month" -> R.drawable.ic_category_calendar_month
    "fact_check" -> R.drawable.ic_category_fact_check
    "inventory_2" -> R.drawable.ic_category_inventory_2
    "sell" -> R.drawable.ic_category_sell
    "handyman" -> R.drawable.ic_category_handyman
    "add_card" -> R.drawable.ic_add_card
    "arrow_back" -> R.drawable.ic_arrow_back
    "chevron_right" -> R.drawable.ic_chevron_right
    "cloud_sync" -> R.drawable.ic_cloud_sync
    "credit_card" -> R.drawable.ic_credit_card
    "delete_forever" -> R.drawable.ic_delete_forever
    "filter_list" -> R.drawable.ic_filter_list
    "keyboard_arrow_down" -> R.drawable.ic_keyboard_arrow_down
    "notes" -> R.drawable.ic_notes
    "palette" -> R.drawable.ic_palette
    "payments" -> R.drawable.ic_payments
    "upload_file" -> R.drawable.ic_upload_file
    "water_drop" -> R.drawable.ic_water_drop
    "water_full" -> R.drawable.ic_water_full
    "water_medium" -> R.drawable.ic_water_medium
    else -> R.drawable.ic_category_sell
}
