# Documento histórico: integración retirada en 1.0.0

Esta guía describe la integración de versiones anteriores. La aplicación 1.0.0 retira su interfaz, OAuth y operaciones remotas. No se debe usar esta guía para configurar la versión actual. Consulta [COPIAS_LOCALES.md](COPIAS_LOCALES.md).

---

# Google Drive en FuelStatus 0.2

## Configuración de Google Cloud

Ya confirmaste que creaste el proyecto, habilitaste Google Drive API, configuraste el consentimiento y creaste el cliente Android.

El cliente debe corresponder al APK que instalas:

- Tipo: Android.
- Paquete: `mx.fuelstatus`.
- SHA-1 debug de esta Mac: `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- Permiso solicitado por la app: `https://www.googleapis.com/auth/drive.file`.
- Si el proyecto está en modo de prueba, añade la cuenta de Google del teléfono a los usuarios de prueba.

Esta implementación usa Google Identity AuthorizationClient con autorización directa de Drive. No necesita un secreto de cliente, un servidor ni un archivo JSON de credenciales en el proyecto. El token de acceso solo se utiliza en memoria; los tokens de renovación los administra Google Play services. La app obtiene el correo mediante Drive `about.get` para identificar la cuenta conectada.

## Primera prueba en el teléfono

1. Compila y ejecuta desde Android Studio o instala el APK 0.2 sobre la versión anterior, conservando paquete y firma. No desinstales la versión anterior si quieres conservar su historial local.
2. Abre **Ajustes → Copias y Google Drive → Conectar Google Drive**.
3. Elige la cuenta añadida como usuario de prueba y autoriza el acceso solicitado.
4. Si no existe un historial anterior, la app crea una carpeta **FuelStatus** y programa la primera copia. Puedes pulsar **Sincronizar ahora**.
5. Comprueba el correo, la fecha de **Última copia en Drive** y **Abrir carpeta FuelStatus en Drive**. La fecha solo aparece después de confirmar una subida o reconocer una subida previa.
6. Activa modo avión, añade o edita un llenado y vuelve a Ajustes: la copia local se actualiza y la subida queda pendiente.
7. Recupera conexión y vuelve a abrir la app. Debe subir la copia pendiente. El botón manual permite reintentar inmediatamente.
8. Para comprobar restauración, pulsa **Restaurar desde Drive**. Revisa fecha y cantidades antes de confirmar. La consulta por sí sola no modifica datos.

La autorización real con tu cuenta y una subida real deben comprobarse en el teléfono; las pruebas automáticas usan un servicio de Drive simulado y no equivalen a esa validación.

## Comportamiento

- Cada cambio de vehículos, llenados, gastos o apariencia se guarda en Room y aumenta una revisión local. Después se escribe un JSON completo en el almacenamiento privado de la app, mediante sustitución atómica y vaciado del archivo a disco.
- La copia incluye vehículos, llenados, gastos y apariencia. El CSV sigue siendo una exportación de llenados.
- La carpeta de Drive se crea automáticamente y se puede abrir desde Ajustes. Esta versión no incorpora un selector de carpetas existentes.
- Se guardan archivos inmutables; después de una subida confirmada se conservan las cinco versiones más recientes de FuelStatus en esa carpeta. Los archivos ajenos a la app no se incluyen en la limpieza.
- La sincronización se solicita después de guardar, se reintenta con WorkManager cuando existe red y se vuelve a intentar al abrir la app si hay cambios pendientes. Android decide cuándo ejecuta los trabajos en segundo plano; después de cinco reintentos fallidos se espera a una nueva apertura, cambio o reintento manual.
- La fecha de Drive solo avanza tras verificar la respuesta y el MD5 de la copia. Si se pierde la respuesta de una subida ya completada, el siguiente intento puede reconocerla por su identificador de contenido y MD5.
- Los cambios hechos durante una subida quedan pendientes para una copia posterior.
- Si aparece otro historial en Drive, se pausa la subida y se ofrecen **Revisar copia de Drive** o **Usar historial de este teléfono**. No se combinan historiales automáticamente. No es una base de datos compartida en tiempo real entre teléfonos.
- La restauración valida formato, versión, checksum, identificadores y relaciones antes de reemplazar las tablas en una transacción. Primero conserva una copia local de recuperación del historial anterior; se retienen hasta cinco archivos de recuperación.
- Desconectar cancela los trabajos de la app y conserva archivos y datos. Para retirar también el permiso concedido a Google, utiliza la administración de acceso de terceros de tu cuenta de Google.

## Archivos y límites

La copia actual está en `files/backups/latest.json`, dentro del almacenamiento privado de FuelStatus. Los archivos previos a restaurar tienen el prefijo `recovery-`. La base de datos local sigue siendo la fuente principal. Si falla la escritura del JSON, el guardado de Room se conserva y Ajustes muestra el error local.

La copia JSON admite hasta 5 MB y 50 000 registros por tabla. Un archivo que supere esos límites no se sube ni se restaura; se muestra el error. El formato conserva centavos, mililitros y metros como enteros y añade SHA-256 para detectar cambios o corrupción. El checksum no es una firma digital ni cifrado. Los archivos de Drive están bajo las reglas de acceso de tu cuenta; la app no cifra su contenido adicionalmente.

Desinstalar elimina Room, las preferencias y las copias locales privadas. Las copias ya confirmadas en Drive permanecen allí. Una copia local en el mismo teléfono no protege por sí sola contra pérdida del dispositivo.

## Errores

En Ajustes, **Ver detalle del error** muestra fecha, código HTTP, razón de Google y mensaje disponibles, con tokens ocultos.

| Error | Comprobación |
| --- | --- |
| Google Identity, código 10 | Paquete y SHA-1 del cliente Android deben coincidir con la compilación instalada. |
| Acceso denegado durante autorización | Comprueba el usuario de prueba y la configuración de consentimiento. |
| HTTP 403 `accessNotConfigured` / servicio deshabilitado | Comprueba que Drive API esté habilitada en el proyecto del cliente Android. |
| HTTP 403 permisos insuficientes | Pulsa Reconectar y vuelve a conceder el permiso. |
| HTTP 401 | La app invalida el token rechazado y vuelve a solicitar acceso en el siguiente intento. Si se requiere interacción, pulsa Reconectar. |
| HTTP 429, límites de uso, HTTP 5xx o fallo de red | Se conserva la copia local y se permite reintento. |
| Cuota de almacenamiento agotada | Libera espacio en esa cuenta y pulsa Sincronizar ahora. |
| Carpeta o copia eliminada | Reconecta para revisar el destino. No se sube silenciosamente sobre un historial desaparecido. |

Para otra Mac o una firma de publicación, registra el SHA-1 correspondiente en el mismo proyecto Google Cloud. Conserva la firma con la que distribuyas futuras actualizaciones.

## Referencias

- [Autorización Android](https://developer.android.com/identity/authorization).
- [Permiso drive.file](https://developers.google.com/workspace/drive/api/guides/api-specific-auth).
- [Subidas multipart](https://developers.google.com/workspace/drive/api/guides/manage-uploads).
- [Errores de Drive](https://developers.google.com/workspace/drive/api/guides/handle-errors).
