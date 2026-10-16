# Validación 0.2.0 · Google Drive

16 de octubre de 2026. Misma configuración Android/Gradle descrita en la validación inicial.

- **APK debug 0.2.0 compilado y firmado:** `app/build/outputs/apk/debug/app-debug.apk`; copia de entrega en `.local/releases/FuelStatus-0.2.0.apk` (36 378 862 bytes).
- **45 pruebas JVM aprobadas, sin omisiones:** 10 de estadísticas, 11 de CSV (incluida la comprobación opcional del archivo privado), 21 de copia/sincronización/restauración y 3 del contrato REST de Drive.
- **7 pruebas instrumentadas aprobadas:** 4 de Room (importación/persistencia/migración 1→2/restauración completa y transacciones), 2 de Ajustes de Drive (conectar/error visible/fecha de nube no confirmada) y 1 de navegación, guardado, tema y acceso a la tarjeta de Drive.
- Revisión visual de la tarjeta de Drive y repetición de la prueba de navegación aprobadas; captura en `.local/qa/ajustes-drive.png`.
- **Android Lint:** cero errores y 20 avisos, incluidos consejos KTX y los avisos anteriores del proyecto.
- **Firma del APK verificada:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK de entrega:** `ace9e77c7cc1b4169c0f06a7178f6ebb8e04303651df08e7fa094c4e4f50d773`.
- Se verificó que el APK no incluye CSV privados ni archivos de respaldo del usuario.

Las pruebas de copia cubren pérdida de red, guardado local, reintento, respuesta de subida perdida sin duplicación, ediciones durante subida, rechazo de otra cuenta, cambio/eliminación del historial remoto, checksum, formato inválido, relaciones entre datos, límites de retención, restauración con recuperación previa y conservación de datos al desconectar. El contrato REST comprueba consulta de cuenta, creación de carpeta, paginación, metadatos y contenido multipart. El servicio de Drive de estas pruebas es simulado.

La primera ejecución de las pruebas de interfaz falló con el emulador dormido. Tras activar la pantalla y quitar su bloqueo, las siete pruebas pasaron. No se modificó la app para ocultar ese fallo.

**Pendiente de comprobación externa:** autorización con el cliente OAuth del usuario, cuenta Google de prueba, subida/descarga real y ciclo de modo avión en el Pixel. Estas pruebas no se han ejecutado contra Google Drive real. La guía está en [GOOGLE_DRIVE.md](GOOGLE_DRIVE.md).

---

# Validación histórica de la recreación 0.1.0

16 de octubre de 2026. Android Studio y JDK integrados de esta Mac, SDK 37.0, Gradle 9.4.1, AGP 9.2.1. Emulador Medium Phone, Android 17 / API 37.0.

## Resultado

- **APK debug generado:** `app/build/outputs/apk/debug/app-debug.apk`.
- **21 pruebas JVM aprobadas:** 10 de estadísticas y 11 de CSV; incluida la comprobación opcional de un archivo privado externo al repositorio.
- **3 pruebas instrumentadas aprobadas:** importación repetida y conservación de ediciones, persistencia tras reabrir la base de datos, navegación por las cinco pantallas / guardado de llenado / cambio de tema.
- **Android Lint:** cero errores, 16 avisos. Incluyen versiones más recientes disponibles, target SDK 36, recomendaciones de estilo Kotlin/Compose y configuración de transferencia de datos. No se han suprimido los avisos para ocultarlos.
- El APK no contiene archivos CSV ni el historial personal.
- Revisión visual de las cinco secciones, colores dinámicos, paleta verde y modo oscuro. Se comprobó el contraste de los iconos de la barra de estado después de ajustar la obtención de la Activity desde el contexto de Compose.

Las pruebas de navegación detectaron etiquetas ausentes en los botones flotantes. Se añadieron descripciones de accesibilidad y semántica de selección a la navegación. La prueba quedó aprobada después de la corrección.

## Contraste con el CSV local

Se contrastaron el importador, los totales y los cálculos con un archivo privado externo al repositorio. El detalle numérico de esa comprobación se conserva únicamente en los archivos locales de trabajo.

La prueba opcional acepta `-PfuelCsv=/ruta/al/archivo.csv` y verifica que la exportación e importación conserven todos los campos del historial. Las pruebas publicadas de estadísticas y precisión monetaria usan datos sintéticos.

## Límites de esta validación

La comprobación se hizo en emulador; falta probar el APK en el Pixel 10 y contrastar sus promedios con la pantalla de Fuel. No se validaron Drive ni mapas porque están fuera del alcance inicial. La exportación CSV contiene llenados, no otros gastos; el archivo de salida aún no se ha importado en la aplicación Fuel original.

Los informes se generan en `app/build/reports/tests`, `app/build/reports/androidTests` y `app/build/reports/lint-results-debug.html`. Las capturas locales de revisión se guardan en `.local/qa`; no se incluyen en el repositorio.
