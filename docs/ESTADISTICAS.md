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

### Equivalencia en km/L, litros y distancia

La misma tarjeta añade el rendimiento ponderado de cada periodo en **km/L** (`kilómetros totales / litros totales`). Por ejemplo: «Tu consumo aumentó un 10.0 % (tu rendimiento bajó de 12.5 a 11.4 km/L) respecto al periodo anterior». El porcentaje sigue midiendo el cambio en combustible por kilómetro; no se interpreta como un cambio del mismo porcentaje en km/L. Si ambos rendimientos se muestran iguales a un decimal, el texto indica que se mantuvo ese rendimiento.

Los litros adicionales o de ahorro son una estimación para **la distancia del periodo reciente**, calculada sin redondear el porcentaje:

`Diferencia de litros = litros recientes − (litros anteriores / km anteriores) × km recientes`.

Un resultado positivo representa litros adicionales frente a mantener el consumo anterior; uno negativo representa menos litros. Por ejemplo, 120 L en 1,500 km antes y 105.6 L en 1,200 km recientes equivalen a 9.6 L adicionales para esos 1,200 km, aunque el total de litros registrados sea menor. La diferencia se muestra aproximadamente a un decimal; si redondea a cero, se indica que es menor a 0.1 L.

El apartado **Distancia recorrida**, dentro de la misma tarjeta, compara los kilómetros de esos dos periodos: diferencia en km y `(km recientes / km anteriores − 1) × 100`. Muestra ambos totales y conserva las fechas al pie. Los periodos pueden durar distinto número de días: esta comparación no representa kilómetros por día. Diferencias que redondean a cero se describen como prácticamente la misma distancia.

Las equivalencias y la comparación de distancia solo aparecen cuando existe una tendencia válida. No requieren campos nuevos, migraciones ni cambios al formato de respaldo.

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

Todas las cards de Estadísticas incorporan iconos de su indicador y un botón **ⓘ** de información. El botón abre un modal específico con la fuente de los datos, la fórmula, los requisitos y las limitaciones del cálculo o estimación. Incluye las seis proyecciones, el rendimiento principal y los últimos intervalos: 16 ayudas cuando todas las tarjetas están disponibles.

La información también está disponible cuando una tarjeta no tiene datos suficientes. El modal permite desplazar explicaciones largas y cerrarlo con **Entendido**, Atrás o pulsando fuera; conserva su apertura al recrearse la pantalla. La explicación general **Cómo calculamos** se elimina de la lista, junto con el espacio que ocupaba.

Los botones tienen un área táctil de 48 dp y una descripción accesible que identifica la tarjeta. En tarjetas estrechas, el icono del indicador y el botón de información comparten una fila y el título se coloca debajo para conservar su ancho. En pantallas estrechas o con texto ampliado, las parejas de cards se apilan para conservar legibilidad.

## Proyecciones y métricas recientes

Se añaden seis tarjetas debajo de Tendencia de consumo. Próximo llenado, Alcance de una carga habitual, Ahorro potencial y Gasto estimado del mes ocupan una fila completa. Costo reciente / 100 km y Tendencia del precio / L comparten fila cuando el ancho y el tamaño de texto lo permiten.

Todas las proyecciones usan el vehículo seleccionado y excluyen registros fechados después del día local actual. La fecha y la zona horaria se actualizan al volver a la pantalla y al cambiar el día. Editar, eliminar o importar registros y cambiar de vehículo recalcula los resultados. Son escenarios basados en hábitos registrados: no extrapolan indefinidamente una mejora o un aumento de consumo.

### Próximo llenado estimado

Requiere al menos tres intervalos completos válidos del bloque más reciente, sin atravesar cargas excluidas, faltantes ni retrocesos del odómetro. Usa como máximo los seis intervalos más recientes entre tanques llenos, contando días de calendario local. El mínimo y máximo de días observados, sumados a la fecha del último tanque lleno, forman un rango orientativo; la mediana de días, redondeada hacia arriba, proporciona la fecha habitual. El rango no representa un intervalo de confianza estadístico.

El rango visible comienza como mínimo hoy. Si ya terminó, la tarjeta pide actualizar el historial y conserva la fecha estimada original: no inventa una nueva fecha futura. Una carga parcial posterior se señala expresamente. Si la mediana es cero días, se pide una frecuencia en días distintos. Se muestra cuántos intervalos respaldan la estimación; no se infieren litros restantes ni capacidad del tanque.

### Alcance de una carga habitual

Requiere tres intervalos completos válidos. Los litros de referencia son la mediana de las cantidades **compradas en las últimas tres cargas con tanque lleno**. Las cargas parciales se suman para obtener el rendimiento del intervalo, pero no se confunden con el volumen de una sola carga habitual.

`Alcance estimado = litros habituales × (km totales / litros consumidos de los tres intervalos recientes)`.

Con seis intervalos válidos se compara también el alcance para **esos mismos litros** usando el rendimiento de los tres intervalos anteriores. El texto indica cuántos km más, menos o similares se podrían recorrer si se mantiene el consumo reciente. Se muestran fecha y número de intervalos de referencia. El alcance describe esa cantidad de combustible, no la autonomía restante del vehículo.

### Costo reciente / 100 km y tendencia del precio / L

El costo reciente usa `costo acumulado / km acumulados × 100` de hasta tres intervalos completos válidos; necesita al menos uno. Se muestran cantidad de intervalos y fecha final.

El precio compara las últimas tres compras incluidas con las tres anteriores del **mismo combustible identificado que la última compra incluida**, ponderando cada grupo por litros (`importe total / litros totales`). Magna, Premium y Diésel no se mezclan para inferir una subida de precio; Sin especificar no permite esta comparación. Se requieren seis compras comparables y se muestran el combustible, ambos precios, variación y fechas. Esta métrica de precios no requiere continuidad del odómetro.

### Ahorro potencial

Requiere los seis intervalos de una tendencia válida. Compara el combustible necesario para 500 km con el consumo reciente y anterior. Si el consumo aumentó, presenta el ahorro posible al recuperar el rendimiento anterior. Si mejoró, indica los litros que ya se usarían de menos al mantenerlo. Una diferencia que redondea a cero a un decimal se muestra como menor a 0.1 L.

La equivalencia monetaria usa el precio ponderado por litros consumidos en los tres intervalos recientes y supone mantener ese precio. Es una estimación, no una promesa de ahorro.

### Gasto estimado del mes

Proyecta el cierre del mes calendario actual sumando combustible incluido y otros gastos del vehículo. Requiere registros de ese mes y al menos siete días de observación. La observación comienza el día más reciente entre el inicio de mes y el primer movimiento histórico disponible del vehículo; cuenta hasta hoy inclusive.

`Estimación de cada componente = acumulado registrado + (acumulado / días observados) × días restantes del mes`.

Se muestran por separado combustible y otros gastos, acumulado, días observados y días restantes. Si el historial comenzó a mitad del mes, no se inventan gastos anteriores a ese inicio. Los gastos puntuales también forman parte del ritmo registrado; el resultado es condicional a que ese ritmo continúe. Sin movimientos actuales o sin siete días se explica el requisito, sin publicar una proyección ficticia. El último día del mes, la estimación coincide con el acumulado.

Estas tarjetas se calculan en memoria a partir de los datos existentes: no cambian Room 6, migraciones, importación ni respaldo 5.
