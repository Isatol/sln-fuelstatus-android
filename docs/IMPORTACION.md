# CSV de Fuel

En **Ajustes → Importar CSV de Fuel**, elige el export de llenados. La importación conserva los registros existentes, añade los nuevos y comunica cuántos se omitieron por estar repetidos. Toda la validación ocurre antes de escribir y la escritura usa una transacción.

## Formato del export encontrado

| Columna | Interpretación |
| --- | --- |
| `carName`, `carType`, `carYear` | Identificación del vehículo; si coincide con uno existente se usa su historial. |
| `carIsEV` | En esta versión debe ser falso: el importador es de combustible. |
| `dateISO8601` | Instante completo con zona horaria; se presenta según la zona local del dispositivo. |
| `amount` | Litros. |
| `price` | Precio **por litro**. El total se calcula como `amount × price`, redondeado a centavos. |
| `meters` | En este export de Fuel, el valor representa el **odómetro en kilómetros**, aunque el encabezado diga meters. |
| `fullFillup` | Tanque completo o carga parcial. |
| `showInStatistic` | Inclusión en totales y consumo. |
| `comment`, `gasStation` | Notas y gasolinera. |

Las columnas adicionales del CSV original se ignoran. No se importan coordenadas, precios de compra ni costos anuales del vehículo. El método de pago se elige en el diálogo de importación. `drivenDistance` no se usa: se calculan diferencias de odómetro. No se solicita acceso a la ubicación.

## Validaciones

Se admiten CSV UTF-8 con BOM opcional, saltos LF/CRLF, campos entre comillas, comillas escapadas, texto multilínea y separadores coma o punto y coma. Si el separador es punto y coma, los decimales también pueden usar coma. Límite: 5 MB.

Se rechazan columnas obligatorias ausentes, filas incompletas, fechas inválidas, booleanos desconocidos, litros/precios no positivos y odómetros negativos. El mensaje identifica la fila cuando es posible. Un error evita importar parcialmente el historial.

Los duplicados se reconocen por vehículo, fecha, odómetro, litros e importe. Los registros importados también tienen un identificador determinista para que repetir la misma importación después de editar una carga no sobrescriba ni duplique esa carga.

## Exportación de FuelStatus

Incluye llenados de todos los vehículos en UTF-8, con encabezados del importador y una columna adicional `missedPrevious` para conservar el indicador de carga faltante al volver a FuelStatus. El precio por litro se exporta con precisión suficiente para recuperar el mismo importe en centavos.

La compatibilidad de entrada se ha definido a partir del export de Fuel encontrado. La importación de este CSV de salida en la aplicación Fuel original todavía no se ha comprobado.


## Selección de destino y combustible (0.3)

Antes de importar se muestra una vista previa para elegir el vehículo de origen del archivo y el destino en la aplicación, o crear el vehículo encontrado. Un checkbox permite aplicar el combustible del destino a todos los registros nuevos. Si se desactiva, se conserva `fuelType` del CSV o Sin especificar cuando no esté indicado. La exportación incluye las columnas opcionales `fuelType` y `vehicleFuelType`. Consulte [DATOS_Y_COMBUSTIBLE.md](DATOS_Y_COMBUSTIBLE.md).

## Métodos de pago (0.4)

El diálogo agrega un selector con Efectivo y las tarjetas registradas. El método elegido se aplica a todos los llenados nuevos de esa importación y no cambia duplicados existentes. La exportación añade `paymentMethodName` y `paymentMethodBrand` (`cash`, `visa`, `mastercard` o `amex`), con campos vacíos si el registro no tiene método especificado. El lector reconoce estas columnas cuando están presentes; los CSV anteriores siguen siendo válidos. Consulte [METODOS_DE_PAGO.md](METODOS_DE_PAGO.md).
