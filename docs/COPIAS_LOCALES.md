# Copias locales · FuelStatus 1.0.0

El historial principal se guarda en Room, esquema 6. Cada cambio intenta actualizar una copia completa privada en `files/backups/latest.json`, con formato 5. Conserva vehículos, llenados, gastos, métodos de pago, categorías y apariencia. Los formatos anteriores siguen siendo legibles por el códec y por la fuente de restauración transaccional existente.

La escritura utiliza un archivo temporal y sustitución atómica. Si falla el archivo auxiliar después de guardar en Room, los registros ya confirmados se conservan. Una copia dañada se regenera desde la base. Los archivos privados de recuperación ya existentes se conservan al actualizar.

La versión 1.0.0 no tiene botones para exportar o restaurar copias completas. El CSV de Ajustes incluye llenados; no sustituye una copia completa. Las copias privadas están en el mismo dispositivo: desinstalar o perder el teléfono también implica perderlas. Los archivos CSV guardados fuera del almacenamiento privado se conservan al desinstalar.

## Borrar todos los datos

En Ajustes, **Borrar todos los datos** abre una confirmación. Cancelar conserva el historial. Confirmar borra vehículos, llenados, gastos, tarjetas personalizadas, categorías personalizadas y archivos privados de respaldo/recuperación. Restablece la apariencia, Efectivo y las siete categorías iniciales. Después se crea una copia privada del estado vacío. La operación funciona sin conexión.

Si la fuente de datos rechaza el reinicio, las copias actuales y de recuperación no se eliminan. La fuente coordina los cambios de Room y apariencia para preservar el historial si falla, por ejemplo, la actualización del icono del launcher.

## Actualizaciones desde versiones anteriores

Se conserva la identidad del dispositivo usada por las copias y el formato 5. La inicialización cancela el trabajo persistente de sincronización anterior, retira su configuración y conserva la base, apariencia y archivos locales. La clase del antiguo worker permanece únicamente como operación vacía para terminar de forma segura cualquier trabajo retenido por Android. La versión no solicita permisos de Internet ni de estado de red y no incorpora el SDK de autorización de Google.

No se ejecutan operaciones sobre cuentas o archivos remotos al actualizar ni al borrar datos locales. La integración con servicios externos queda fuera de 1.0.0.
