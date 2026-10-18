package mx.fuelstatus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import mx.fuelstatus.R

internal enum class StatisticInfo(val title: String, val explanation: String) {
    Efficiency("Rendimiento", "Sumamos los kilómetros y los litros consumidos de los intervalos completos entre tanques llenos. El rendimiento es kilómetros totales ÷ litros totales (km/L); no es el promedio de rendimientos individuales.\n\nEl primer tanque lleno fija el punto de partida: sus litros no se cuentan como consumidos en el primer intervalo. Las cargas parciales se acumulan hasta cerrar el siguiente tanque lleno. Una carga excluida o una carga anterior faltante reinicia el cálculo. El tramo posterior al último tanque lleno queda pendiente."),
    ConsumptionTrend("Tendencia de consumo", "Comparamos los tres últimos intervalos completos válidos con los tres anteriores. Se necesitan seis intervalos consecutivos, como mínimo siete cargas con tanque lleno. Las parciales se acumulan; una carga faltante, excluida o un retroceso del odómetro interrumpe la secuencia.\n\nCada periodo usa litros totales ÷ kilómetros totales × 100. La variación es (consumo reciente ÷ consumo anterior − 1) × 100, redondeada a un decimal. Menos consumo por kilómetro es mejor: una variación de −5 % o menor indica mejora; por encima de −5 % y por debajo de 5 % es estable; desde 5 % hay aumento, y desde 15 % se destaca como importante. Son umbrales descriptivos, no un diagnóstico mecánico.\n\nLa equivalencia en km/L usa kilómetros ÷ litros de cada periodo. Los litros adicionales o de ahorro comparan los litros recientes con los que habrías usado para esa misma distancia manteniendo el consumo anterior.\n\nDistancia recorrida compara los kilómetros de esos mismos periodos. Su variación es (km recientes ÷ km anteriores − 1) × 100. Las fechas pueden abarcar distinta cantidad de días; no es una comparación de kilómetros diarios."),
    NextFullRefuel("Próximo llenado estimado", "Usamos entre tres y seis intervalos completos válidos recientes y contamos los días de calendario local entre sus tanques llenos.\n\nA la fecha del último tanque lleno sumamos el menor y el mayor intervalo observado para obtener el rango. La fecha habitual usa la mediana de días, redondeada hacia arriba. La mediana es el valor central al ordenar los intervalos; si hay dos valores centrales, se promedian.\n\nEl rango visible comienza como mínimo hoy. Si ya terminó, pedimos actualizar el historial en lugar de inventar otra fecha futura. Una carga parcial posterior se señala en la tarjeta.\n\nEs una orientación sobre tus hábitos, no un intervalo de confianza estadístico. No conocemos el combustible restante ni cuánto conducirás. Las cargas futuras no se usan como observaciones; los huecos y exclusiones reinician la base comparable."),
    UsualRange("Alcance de una carga habitual", "Tomamos la mediana de los litros comprados en las tres últimas cargas completas válidas: es la cantidad central al ordenar esas tres cargas. Esa cantidad es la referencia de una carga habitual, no la capacidad del tanque.\n\nAlcance estimado = litros habituales × (kilómetros totales ÷ litros consumidos de los tres intervalos recientes). Las cargas parciales cuentan para el rendimiento del intervalo, pero no se confunden con los litros de una sola carga completa.\n\nCuando hay seis intervalos comparables, calculamos también cuánto rendirían esos mismos litros con el rendimiento anterior y mostramos la diferencia en kilómetros. Es un escenario si mantienes el consumo reciente, no la autonomía restante. Se excluyen registros futuros y se muestra la fecha de referencia."),
    RecentCost("Costo reciente / 100 km", "Usamos hasta los tres últimos intervalos completos válidos del bloque reciente; necesitas al menos uno. Sumamos el costo del combustible consumido, incluidas las cargas parciales, y lo dividimos entre sus kilómetros.\n\nCosto por 100 km = costo total ÷ kilómetros totales × 100.\n\nEl pago del primer tanque que inicia cada secuencia no corresponde al combustible consumido después y no se incluye como costo de ese primer intervalo. Esta tarjeta no suma otros gastos. A diferencia de Costo / km, usa solo los intervalos recientes y descarta registros futuros."),
    PriceTrend("Tendencia del precio / L", "Tomamos las últimas seis compras incluidas del mismo combustible identificado que la última compra incluida. Comparamos las tres recientes con las tres anteriores. No mezclamos Magna, Premium y Diésel; Sin especificar no permite esta comparación.\n\nPrecio de cada grupo = importe total ÷ litros totales. Así, las cargas de más litros tienen el peso que les corresponde.\n\nVariación = (precio reciente ÷ precio anterior − 1) × 100. Una cifra positiva indica que el precio medio subió; una negativa, que bajó. Las fechas indican qué compras se comparan. No se usan compras futuras ni se necesita continuidad del odómetro para comparar precios."),
    Savings("Ahorro potencial", "Necesitas los seis intervalos completos de una tendencia válida. Calculamos cuántos litros usarías para recorrer 500 km con el consumo reciente y con el anterior.\n\nDiferencia en litros = (L/100 km recientes − L/100 km anteriores) × 5.\n\nSi el consumo aumentó, esos litros representan el ahorro posible al recuperar el rendimiento anterior. Si mejoró, mostramos el ahorro que ya supondría mantenerlo. Una diferencia que redondea a cero se describe como menor a 0.1 L.\n\nPara convertirlo a pesos usamos costo total ÷ litros totales de los tres intervalos recientes. La estimación supone mantener ese precio: no predice el precio futuro ni garantiza un ahorro."),
    MonthlySpending("Gasto estimado del mes", "Sumamos las compras incluidas de combustible y todos los otros gastos registrados del vehículo en el mes actual hasta hoy. Requerimos movimientos de este mes y al menos siete días de observación.\n\nLa observación empieza en la fecha más reciente entre el inicio del mes y el primer movimiento histórico disponible, y cuenta hasta hoy inclusive.\n\nEstimación = acumulado + (acumulado ÷ días observados) × días restantes del mes. Calculamos combustible y otros gastos por separado y luego los sumamos.\n\nSi empezaste a registrar a mitad de mes, no inventamos gastos de los días anteriores. Los gastos puntuales también forman parte del ritmo observado. El resultado supone que ese ritmo continúa; el último día del mes coincide con el acumulado. Los registros futuros no se usan."),
    AverageDistance("Distancia media entre cargas", "Ordenamos los llenados por fecha y odómetro y revisamos cada par de cargas consecutivas. Incluimos tanto cargas completas como parciales.\n\nDistancia media = suma de kilómetros de los pares válidos ÷ número de pares válidos. Cinco cargas consecutivas producen cuatro intervalos, no cinco. Necesitas al menos dos cargas para formar un par.\n\nSe descarta el par si contiene una carga excluida, si la segunda indica una carga anterior faltante o si el odómetro no aumenta. No unimos registros atravesando esos huecos. Mide distancia entre cargas, no distancia de viajes individuales."),
    Frequency("Frecuencia de carga", "Usamos los mismos pares consecutivos válidos que en Distancia media entre cargas, incluidas las cargas parciales.\n\nFrecuencia = suma de días entre las fechas locales de esos pares ÷ número de pares válidos. Dos cargas el mismo día aportan cero días. Contamos días de calendario, no bloques de 24 horas, para respetar cambios de horario.\n\nTambién mostramos el mínimo y máximo de días observados. Una carga excluida, una anterior faltante o un odómetro que no aumenta interrumpe el par. Esta media de todas las cargas es distinta de la previsión del próximo tanque lleno."),
    CostPerKm("Costo / km", "Sumamos el costo del combustible consumido en todos los intervalos completos entre tanques llenos y lo dividimos entre sus kilómetros totales.\n\nCosto por km = costo total de esos intervalos ÷ kilómetros totales.\n\nLas cargas parciales se acumulan hasta el siguiente tanque lleno; el primer tanque fija la referencia y su compra no se cuenta como combustible consumido en el primer intervalo. Las exclusiones y las cargas anteriores faltantes reinician la secuencia. No se suman otros gastos ni se limita el cálculo a los tres intervalos recientes."),
    AveragePrice("Precio medio / L", "Dividimos todo el importe registrado en las cargas incluidas entre todos los litros de esas cargas del vehículo.\n\nPrecio medio por litro = importe total ÷ litros totales.\n\nEs un promedio ponderado por litros, no el promedio simple de los precios de cada carga. Incluye cargas completas y parciales, además de la primera carga de referencia. Puede reunir los distintos combustibles registrados y no se limita a los periodos comparados en la tendencia."),
    RegisteredLiters("Litros registrados", "Sumamos los litros de todos los llenados del vehículo que estén incluidos en estadísticas.\n\nCuenta tanto tanques llenos como cargas parciales y también el primer tanque que establece la referencia. Las cargas excluidas no se suman.\n\nEs combustible registrado como comprado, no una medición de lo que queda en el tanque ni de los litros consumidos únicamente en intervalos completos."),
    EvaluatedDistance("Km evaluados", "Sumamos la distancia recorrida entre el inicio y el cierre de todos los intervalos completos entre tanques llenos. Las cargas parciales pertenecen al intervalo que termina en el siguiente tanque lleno.\n\nNo sumamos lecturas del odómetro: sumamos las diferencias de cada intervalo válido. Una carga excluida o una anterior faltante reinicia la referencia. El tramo posterior al último tanque lleno todavía no se evalúa.\n\nEsta distancia es la base del rendimiento y del costo por kilómetro; puede ser menor que toda la distancia registrada en el historial."),
    TotalSpending("Combustible + otros gastos", "Sumamos el importe de todos los llenados incluidos en estadísticas y el importe de todos los otros gastos del vehículo seleccionado.\n\nTotal = combustible registrado incluido + otros gastos registrados.\n\nLas compras de combustible incluyen cargas completas, parciales y el primer tanque de referencia. No se limita a los intervalos completos ni al mes actual. Representa importes registrados, no una estimación futura."),
    RecentIntervals("Últimos intervalos", "Mostramos hasta los últimos ocho intervalos completos calculados entre tanques llenos. Cada fila muestra la fecha de cierre y su rendimiento: kilómetros del intervalo ÷ litros consumidos en él.\n\nLas cargas parciales se acumulan hasta el siguiente tanque lleno. El primer tanque fija la referencia; las exclusiones y las cargas anteriores faltantes reinician la secuencia.\n\nLas barras usan una escala común basada en el mejor rendimiento mostrado, con un mínimo de referencia de 1 km/L: una barra más larga representa más km/L. Son intervalos entre cargas, no viajes, y no equivalen a los grupos de tres usados para la tendencia.");
}

internal val LocalCompactStatisticsHeader = staticCompositionLocalOf { false }

@Composable
internal fun StatisticsInfoButton(info: StatisticInfo) {
    var open by rememberSaveable(info) { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
        FuelIcon(R.drawable.ic_info, "Información de ${info.title}")
    }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(info.title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Se usan los registros del vehículo seleccionado.", style = MaterialTheme.typography.bodySmall)
                info.explanation.split("\n\n").forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Entendido") } })
}

@Composable
internal fun StatisticsCardHeader(info: StatisticInfo, icon: Int,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface) {
    if (LocalCompactStatisticsHeader.current) Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FuelIcon(icon)
            Spacer(Modifier.weight(1f))
            StatisticsInfoButton(info)
        }
        Text(info.title, style = style, color = color)
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FuelIcon(icon)
        Text(info.title, style = style, color = color, modifier = Modifier.weight(1f))
        StatisticsInfoButton(info)
    }
}
