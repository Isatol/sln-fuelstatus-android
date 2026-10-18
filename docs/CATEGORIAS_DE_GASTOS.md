# Categorías de gastos · 0.5.0

En **Ajustes → TUS DATOS**, el botón **Categorías de gastos** aparece debajo de **Mis métodos de pago**. Abre una pantalla completa, con flecha para volver a Ajustes y sin la barra inferior de pestañas.

Las categorías iniciales son Servicio, Mantenimiento, Seguro, Estacionamiento, Peaje, Lavado y Otros. Cada una tiene un icono distinto. Las categorías nuevas aparecen en orden de creación, después de Lavado y antes de Otros; Otros siempre se muestra al final.

**Agregar categoría** abre un formulario de nombre e icono. El nombre se recorta en los extremos, es obligatorio y admite hasta 50 caracteres. No se aceptan duplicados aunque cambien mayúsculas o acentos, incluidas las categorías predeterminadas. El icono inicial es Etiqueta; se puede cambiar en el modal **Seleccionar icono**.

El modal ofrece los 30 iconos del catálogo local, con sus nombres en español y búsqueda por nombre, sinónimo o código. Por ejemplo, “neumático” encuentra Llanta, “seguro” encuentra Escudo y “bateria” encuentra Batería. Una búsqueda sin resultados muestra un mensaje y permite cambiar el texto. Cancelar conserva el icono anterior. Los recursos son Material Symbols Rounded oficiales, incluidos como vectores Android bajo Apache 2.0; no requieren conexión ni una dependencia adicional.

Los formularios **Nuevo gasto** y **Editar gasto** usan el catálogo guardado y muestran nombre e icono en el selector. Un gasto nuevo empieza en Servicio si existe, o en Otros si fue eliminada; al editar se conserva la categoría original. Las categorías son compartidas por los vehículos, igual que los métodos de pago.

## Eliminar categorías (0.8.0)

Deslizar hacia la izquierda permite eliminar cualquier categoría, incluso las iniciales, excepto **Otros**. El modal identifica la categoría y, si está en uso, muestra cuántos gastos están asociados y avisa que todos pasarán a **Otros**, para todos los vehículos. Sin gastos asociados, lo indica. Cancelar o volver conserva la categoría y los gastos. También existe la acción accesible «Eliminar [nombre]».

La reasignación y la eliminación ocurren en una única transacción, sin cambiar importes, notas, fechas, pagos ni vehículos. Los fallos revierten los cambios. Las categorías eliminadas no se recrean al reabrir la app o restaurar una copia. **Borrar todos los datos** sí restablece las siete categorías iniciales. Room continúa en versión 6 y los respaldos en formato 5; la validación permite la ausencia de categorías iniciales eliminables y sigue exigiendo Otros y referencias válidas. Las copias antiguas siguen siendo legibles por esta versión; para restaurar un catálogo reducido se requiere una versión que admita eliminar categorías.

## Datos y copias

- Room pasa al esquema **5**, con migración **4→5** y las migraciones anteriores conservadas.
- La tabla `expense_categories` guarda nombre, código del icono y orden. Los gastos mantienen su campo de categoría original, sin reescribir nombres, importes, notas, fechas ni vehículos. Los nombres históricos ajenos a las siete categorías iniciales se incorporan al catálogo con Etiqueta.
- La migración incrementa la revisión local para que la siguiente copia incluya el catálogo.
- Las copias completas locales y de Drive pasan al formato **4**, que incluye categorías e iconos y conserva los formatos **1, 2 y 3** al restaurar. Las copias antiguas generan las categorías iniciales y las que aparezcan en sus gastos.
- Se rechazan copias con categorías duplicadas, iconos desconocidos, Otros ausente o modificado, o referencias de gastos a categorías inexistentes.
- La restauración reemplaza categorías, métodos de pago, vehículos, llenados y gastos dentro de una transacción.
- **Borrar todos los datos** elimina las categorías personalizadas y vuelve a dejar las siete iniciales.
- La exportación CSV sigue conteniendo únicamente llenados. Las categorías y los gastos forman parte del respaldo completo.

APK debug: `.local/releases/FuelStatus-0.5.0-categorias-de-gastos.apk`, versionCode **5**, con la misma firma debug de las entregas anteriores.
