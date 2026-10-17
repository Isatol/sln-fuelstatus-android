# Estadísticas de consumo · 0.8.0

Los indicadores se calculan para el vehículo seleccionado y se actualizan al agregar, editar, importar o eliminar registros. No se modifican los datos guardados, las migraciones, Room 6 ni el formato de respaldo 5. No se añaden servicios externos.

## Tendencia de consumo

Compara los tres últimos intervalos completos entre tanques llenos con los tres anteriores. Las cargas parciales se acumulan hasta cerrar cada intervalo. La primera carga con tanque lleno establece el punto de partida; sus litros no se consideran consumidos en el primer intervalo.

Cada periodo usa `litros consumidos / kilómetros recorridos × 100` (L/100 km). Se suman litros y kilómetros del grupo antes de dividir; no se promedian rendimientos individuales. Menos L/100 km representa una mejora.

La variación es `(consumo reciente / consumo anterior − 1) × 100`, redondeada a un decimal, que también se usa para clasificar:

| Variación | Estado |
|---|---|
| ≤ −5 % | Consumo en mejora |
| > −5 % y < 5 % | Consumo estable |
| ≥ 5 % y < 15 % | Consumo en aumento |
| ≥ 15 % | Aumento importante del consumo |

Son umbrales descriptivos acordados para la aplicación, no un diagnóstico mecánico ni una comparación universal entre vehículos. El porcentaje se acompaña de texto y de las fechas reales de **Periodo reciente** y **Periodo anterior**. La carga que cierra un periodo establece el punto de partida del siguiente: ambos rangos pueden compartir una fecha.

Se necesitan seis intervalos completos consecutivos (como mínimo siete cargas con tanque lleno). Una carga excluida, una carga faltante o un retroceso del odómetro según las fechas reinicia la secuencia comparable. Las cargas parciales posteriores al último tanque lleno no distorsionan una comparación ya completada. Si no hay datos suficientes, se explica en la card, sin asignar un estado de consumo.

Se conservan los cálculos históricos de rendimiento, precio y costo por kilómetro. Para la tendencia se comprueba además que todas las cargas del intervalo pertenezcan a la secuencia cronológica válida, incluidas las parciales.

## Distancia media entre cargas

`Suma de kilómetros entre cargas consecutivas válidas / número de intervalos válidos`.

Se incluyen cargas completas y parciales. Cinco cargas consecutivas producen cuatro intervalos; 800 km entre ellas dan 200 km por intervalo. No representa la distancia de un viaje individual.

## Frecuencia de carga

`Suma de días entre cargas consecutivas válidas / número de intervalos válidos`.

Se usan fechas del calendario en la zona horaria local, sin dividir milisegundos entre 24 horas. Así los cambios de horario no distorsionan el número de días. Dos cargas del mismo día aportan cero días. La card muestra la media y el mínimo/máximo observados; si todos los intervalos son iguales, muestra ese valor sin repetir un rango idéntico.

Para ambas medias se ordena el historial por fecha y odómetro y se examinan pares adyacentes sin eliminar filas antes del cálculo. Se descarta el par cuando contiene una carga excluida, cuando la segunda indica una carga anterior faltante o cuando el odómetro no aumenta. No se conectan registros a través de ese hueco. Las medias pueden usar los intervalos válidos de todo el historial, aunque haya huecos en otros tramos. Con menos de dos registros consecutivos válidos se muestra `—` y una explicación.

## Presentación

Todas las cards de Estadísticas incorporan iconos Material: rendimiento, tendencia, distancia media, frecuencia, costo/km, precio medio/L, litros, kilómetros evaluados, gastos totales, últimos intervalos y explicación del cálculo. En pantallas estrechas o con texto ampliado, las parejas de cards se apilan para conservar legibilidad.
