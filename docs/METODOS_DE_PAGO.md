# Métodos de pago · FuelStatus 0.4

## Ajustes y tarjetas

Los títulos Tema, Copias y Google Drive, Trae tu historial de Fuel y Borrar datos incluyen iconos Material Symbols Rounded. En TUS DATOS, entre importar y borrar, aparece el botón **Mis métodos de pago**.

Este botón abre una pantalla completa con navegación de regreso a Ajustes. Efectivo está disponible desde el inicio. **Agregar tarjeta** abre un formulario para capturar un nombre de hasta 80 caracteres y elegir VISA, Mastercard o American Express. Los selectores y la lista muestran el logo de cada marca. La combinación de nombre y marca no puede repetirse; las validaciones se ejecutan también al guardar en Room.

Los métodos son compartidos por los vehículos de la app. Cada tarjeta contiene su identificador, nombre y marca. Los logos son recursos locales, disponibles sin conexión y con un fondo claro que conserva su legibilidad en modo oscuro.

## Llenados e importación

Nuevo llenado comienza con Efectivo seleccionado. El usuario puede elegir otra tarjeta y la elección queda guardada en ese registro. Al editar un llenado se mantiene su método anterior. En el historial aparece el icono y nombre del método debajo del importe total; los nombres largos pueden ocupar dos líneas y se abrevian visualmente cuando hace falta.

El diálogo de importación de Fuel agrega **Método de pago para los registros**, inicialmente Efectivo. La selección se aplica a todos los llenados nuevos del vehículo de origen elegido. El combustible sigue teniendo su propio selector. Los duplicados mantienen sus datos existentes, incluido su método de pago.

Los llenados guardados antes de esta versión se muestran como **Sin especificar**. No se deduce su método de pago a partir de importes, notas ni combustible. Al editar uno puede asignarse Efectivo o una tarjeta.

## Persistencia y copias

La migración de Room 3→4 crea `payment_methods` con Efectivo y añade una referencia opcional en `refuels`. Conserva todos los valores del historial anterior. La referencia se valida mediante una clave foránea e índice; no se pueden guardar llenados que apunten a métodos inexistentes.

El respaldo JSON usa el formato 3 e incluye los métodos y sus referencias. FuelStatus 0.4 sigue leyendo los formatos 1 y 2: incorpora Efectivo en la lista y conserva el método de los llenados antiguos como Sin especificar. Los respaldos nuevos requieren una versión que entienda el formato 3. La restauración reemplaza métodos, vehículos, llenados y gastos dentro de la misma transacción.

El CSV exportado incluye las columnas `paymentMethodName` y `paymentMethodBrand`; los registros sin especificar dejan ambos campos vacíos. El lector acepta las exportaciones anteriores sin estas columnas. La importación desde la interfaz aplica el método seleccionado en el diálogo. La lectura del CSV también conserva los metadatos para herramientas de recuperación y comprobaciones de exportación/importación.

Borrar datos elimina las tarjetas personalizadas y sus asignaciones junto con el resto del historial. Efectivo vuelve a ser la única opción, como en una instalación nueva. Las copias de recuperación y de Drive usan el mismo formato de respaldo completo.

## Recursos visuales

Los avisos de los iconos y las referencias de los logos se incluyen en `app/src/main/assets/licenses`. Los iconos de Material Symbols y las marcas de pago tienen avisos separados.
