# Datos, combustible e importación · 0.3

## Borrar datos

En Ajustes → TUS DATOS aparece una tarjeta roja **Borrar datos**. El botón abre un diálogo de confirmación; cancelar no modifica nada.

Al confirmar se borran vehículos, llenados, gastos y los archivos privados de respaldo y recuperación. Se restablece la apariencia, se vacía el vehículo seleccionado y se desconecta Drive. La app queda sin vehículos, llenados ni gastos, con solo Efectivo, las siete categorías iniciales y los ajustes predeterminados, como una instalación nueva de esta versión. No reaparece automáticamente el vehículo inicial de versiones anteriores. Los CSV exportados por el usuario fuera del almacenamiento privado de la app se conservan.

Si hay una cuenta de Drive conectada aparece **También borrar el historial de Drive**, desactivado inicialmente. Si no se marca, se conservan las copias remotas. Si se marca, se requiere conexión: primero se envían a la papelera todas las copias de FuelStatus de la carpeta conectada y después se borra el historial local. Se conserva la carpeta y no se tocan archivos ajenos a FuelStatus. La recuperación desde la papelera depende de las reglas de Google Drive.

Ante un fallo remoto se conservan los datos locales, se muestra el error y se pausa la sincronización automática para no recrear las copias recién retiradas. Si hubo un borrado parcial se muestra cuántas copias se procesaron; puede repetirse Borrar datos para completar la operación. Tras una interrupción de la app se mantiene esa pausa hasta que se revise la operación.

La desconexión de la app no elimina la cuenta de Google del teléfono ni revoca el permiso de Google Play services. No se ejecuta ningún borrado remoto al instalar este APK: solo después de marcar la opción y confirmar desde la aplicación.

## Combustible

Los vehículos nuevos permiten elegir Magna (87 octanos), Premium (91 octanos) o Diésel. Magna es el valor predeterminado del vehículo. Cada llenado conserva su propia elección: al crear uno nuevo se selecciona el combustible del vehículo y puede cambiarse antes de guardar. Cambiar el predeterminado del vehículo no altera llenados anteriores.

La migración de Room de versión 2 a 3 añade estos campos sin borrar registros. Los vehículos anteriores comienzan con Magna como predeterminado para futuras capturas; los llenados históricos se mantienen como **Sin especificar**. No se deduce su combustible a partir del precio.

Se guarda el combustible en la copia JSON (formato versión 2) y al exportar CSV. Se siguen pudiendo restaurar copias JSON de formato 1: se aplican los mismos valores de migración.

Referencia de octanajes: [Pemex: lo que necesitas saber al cargar combustible](https://www.pemex.com/negocio/gasolineras/pemex-te-lleva/en-tu-camino/Paginas/002.aspx).

## Importar llenados de FuelStatus

1. En Ajustes → TUS DATOS → Llenados de FuelStatus, elegir el archivo CSV exportado por FuelStatus. Esta lectura no modifica registros.
2. Si contiene varios vehículos, elegir cuál importar.
3. Elegir un vehículo ya registrado o **Importar y crear** el vehículo encontrado. Si aún no hay vehículos se ofrece también **Crear un vehículo primero**.
4. Se conserva el combustible y el método de pago de cada llenado del CSV. Si el archivo no tiene esos campos, permanecen como Sin especificar. Las tarjetas que falten se agregan al catálogo.
5. Confirmar la importación. Los duplicados se omiten y mantienen sus datos previos, incluidas las ediciones de combustible. La importación no modifica registros que ya estaban guardados.

Crear un vehículo desde el archivo genera una identidad nueva, de modo que no sobrescribe un vehículo existente que haya sido renombrado. Volver a importar al mismo vehículo detecta duplicados. Importar deliberadamente el mismo archivo a otro vehículo permite mantener un historial separado.

También se reconocen los identificadores de llenados importados por versiones anteriores, aunque sus importes o notas se hayan editado después; volver a importar no reemplaza esas ediciones.
