# Métodos de pago en gastos · 0.6.0

**Nuevo gasto** y **Editar gasto** muestran el mismo selector de métodos de pago que los llenados, debajo de Categoría. Incluye Efectivo y las tarjetas guardadas en Ajustes, con sus nombres e iconos Material de efectivo o tarjeta de crédito.

- Los gastos nuevos empiezan en **Efectivo**.
- Al editar se conserva el método guardado. Se puede elegir otro o dejar **Sin especificar**, igual que en los llenados.
- Los gastos anteriores conservan **Sin especificar**; la app no intenta deducir cómo se pagaron.
- Las tarjetas del historial de Gastos muestran el icono Material de tarjeta de crédito y el nombre del método debajo del importe.
- La selección se conserva al recrear la Activity con el formulario abierto.

## Persistencia y compatibilidad

Room pasa al esquema **6**. La migración **5→6** añade `expenses.paymentMethodId`, inicialmente nulo, con índice y referencia a `payment_methods`. Se conservan las migraciones anteriores y no se cambian importes, categorías, notas, fechas ni vehículos. La revisión local aumenta para generar una copia con el formato actualizado.

Las copias completas privadas locales pasan al formato **5** e incluyen las asignaciones de pago de los gastos. Se siguen leyendo los formatos **1, 2, 3 y 4**, dejando el pago de sus gastos como Sin especificar. Se rechazan referencias a métodos inexistentes antes de restaurar.

La restauración inserta los métodos de pago antes de los gastos, dentro de la misma transacción. Borrar datos elimina primero gastos y llenados y después las tarjetas, conservando Efectivo al reiniciar. No cambia la exportación CSV, que sigue siendo de llenados.

APK debug: `.local/releases/FuelStatus-0.6.0-pagos-en-gastos.apk`, versionCode **6**, con la misma firma debug de las entregas anteriores.
