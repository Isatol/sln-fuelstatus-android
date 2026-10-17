# Validación 0.7.0 · Iconos y filtro de llenados

16 de octubre de 2026, America/Monterrey. Rama `20261016-iconos-y-filtro-de-llenados`, Gradle 9.4.1, JDK de Android Studio y emulador Medium Phone API 37 en modo desechable. Gradle y el emulador se ejecutaron por separado.

- **APK debug generado y firmado:** `.local/releases/FuelStatus-0.7.0-iconos-filtro-llenados.apk` (37 605 034 bytes), paquete `mx.fuelstatus`, versionCode 7.
- **74 pruebas JVM aprobadas:** las 67 anteriores y 7 de rangos de fechas. Se omitió únicamente la comprobación opcional que requiere un CSV privado; 75 casos registrados, cero fallos y cero errores.
- **37 pruebas Android aprobadas en la suite completa:** las 32 anteriores y 5 de formulario, periodos, rango personalizado, cancelación y restauración del estado.
- **Android Lint:** cero errores y 19 avisos existentes.
- **Firma compatible con las entregas anteriores:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `866d9e4d337c3526970b5220f12e5db66262de50180ef7c80422efc3f49e0eaf`.

Las pruebas de rangos verifican meses de calendario, años bisiestos, límites exactos de inicio y fin, un solo día, zona horaria local y cambios de horario estacional. Se rechazan rangos invertidos y se conserva el orden de los registros, sin mutarlos ni cambiar sus cálculos de consumo.

La interfaz comprueba tres meses por defecto, seis meses y un año; el conteo visible cambia mientras los indicadores conservan los totales completos. El rango personalizado se aplica mediante entrada de fechas y se conserva al restaurar el estado de la pantalla. Fechas inválidas deshabilitan Aplicar; cancelar conserva el filtro anterior. Un periodo vacío muestra una explicación y permite recuperar el historial eligiendo otro rango. El formulario sigue guardando odómetro, litros, importe, gasolinera, notas y los tres interruptores con sus valores correctos.

Se revisaron las capturas del calendario, selector, rango aplicado y formulario en `.local/qa/iconos-filtro-llenados/`. La revisión inicial detectó que el icono reducía el espacio de Total pagado en teléfonos; se amplió la distribución vertical de Litros y Total. Después del ajuste se repitieron y aprobaron la navegación y los cinco casos nuevos con el APK final; también se aprobaron los cinco casos nuevos con el tamaño de texto real de Android al 130 %. Se comprobaron temas claro y oscuro. Los iconos son recursos locales; los nuevos de filtro y notas son Material Symbols Rounded con su aviso Apache 2.0 incluido. Las sesiones de emulador se cerraron y su almacenamiento fue desechable.

No cambia Room (6), ni el formato de respaldo (5), ni la importación o exportación. El filtro no recorta las copias ni modifica el historial guardado. Las pruebas de Drive siguen usando un servicio simulado, sin operar sobre las copias de la cuenta real.

---

# Validación histórica 0.6.0 · Métodos de pago en gastos

16 de octubre de 2026, America/Monterrey. Rama `20261016-metodos-de-pago-en-gastos`, Gradle 9.4.1, JDK de Android Studio y emulador Medium Phone API 37 en modo desechable. Gradle y el emulador se ejecutaron por separado.

- **APK debug generado y firmado:** `.local/releases/FuelStatus-0.6.0-pagos-en-gastos.apk` (36 691 291 bytes), paquete `mx.fuelstatus`, versionCode 6.
- **67 pruebas JVM aprobadas:** 10 de estadísticas, 13 de CSV, 30 del motor de respaldo, 3 de métodos de pago, 4 de categorías, 3 de pagos en gastos y 4 del contrato REST de Drive. Se omitió únicamente la comprobación opcional que requiere un CSV privado; 68 casos registrados, cero fallos y cero errores.
- **32 pruebas Android aprobadas en la suite completa**, incluidas dos nuevas de migración/persistencia de pagos en gastos y tres de sus formularios.
- **Android Lint:** cero errores y 19 avisos existentes.
- **Firma compatible con las entregas anteriores:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `ca65428b200a34f9ed69d01cfa48a768ff14fc800ebde004d37f6d66d3264098`.
- Se comprobó que el APK no incluye CSV, bases de datos, historiales ni archivos de firma privados.

La migración 5→6 conserva los gastos originales y deja su pago sin especificar; las migraciones anteriores se mantienen y se probaron las rutas 1→6, 2→6, 3→6 y 4→6. Se verificaron asignaciones a tarjetas y Efectivo, persistencia al reabrir Room, cambio o eliminación explícita de una asignación, rechazo de métodos inexistentes sin avanzar la revisión y protección de tarjetas referenciadas. La restauración repone métodos y gastos conjuntamente; el borrado elimina sus referencias y conserva Efectivo al reiniciar.

El formato de respaldo 5 conserva pagos de gastos con tarjeta, Efectivo y Sin especificar. Se verificó la lectura de formatos 1 a 4 sin inferir pagos históricos ni cambiar importes, categorías o notas, y el rechazo de referencias de pago inválidas.

Los formularios comprobaron Efectivo por defecto, tarjeta elegida, conservación de pagos al editar notas y selección explícita de Sin especificar. El recorrido completo crea un gasto con una tarjeta, recrea la Activity antes de guardarlo, comprueba el nombre del método en el historial, edita su categoría y pago a Efectivo, y comprueba el borrado de datos. También se repitieron y aprobaron los tres casos de formulario con el tamaño de texto real de Android al 130 %. Se revisaron las capturas de claro, oscuro e historial en `.local/qa/pagos-en-gastos/`; las sesiones de emulador se cerraron y su almacenamiento fue desechable.

**Límite de esta validación:** Drive usa un servicio simulado. Se verificaron serialización y restauración sin modificar las copias de la cuenta real. El APK mantiene paquete y firma para actualizar la instalación anterior.

---

# Validación histórica 0.5.0 · Categorías de gastos

16 de octubre de 2026, America/Monterrey. Rama `20261016-categorias-de-gastos`, Gradle 9.4.1, JDK de Android Studio y emulador Medium Phone API 37 en modo desechable. Gradle y el emulador se ejecutaron por separado para limitar el consumo de memoria.

- **APK debug generado y firmado:** `.local/releases/FuelStatus-0.5.0-categorias-de-gastos.apk` (37 441 266 bytes), paquete `mx.fuelstatus`, versionCode 5.
- **64 pruebas JVM aprobadas:** 10 de estadísticas, 13 de CSV, 30 del motor de respaldo, 3 de métodos de pago, 4 del contrato REST de Drive y 4 de categorías/búsqueda/copias. Se omitió únicamente la comprobación opcional que requiere proporcionar un CSV privado; 65 casos registrados, cero errores y cero fallos.
- **27 casos Android aprobados:** 10 de Room general, 3 de métodos de pago, 3 de categorías y migración, 5 de diálogos, 2 de Ajustes de Drive, 3 de interfaz de categorías y 1 recorrido completo. La ejecución de suite aprobó los 26 casos independientes; NavigationTest se aprobó por separado después de corregir sus selectores y esperas. También se repitieron y aprobaron los tres casos de interfaz con el tamaño de texto real de Android al 130 %.
- **Android Lint:** cero errores y 19 avisos existentes.
- **Firma compatible con entregas anteriores:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `e4b0452cc2a08089a338ac6c5e96234d839040c2d45933d44a78446db74a018d`.
- Se verificaron los 30 recursos vectoriales y sus licencias dentro del APK. No contiene CSV, bases de datos, historiales ni archivos de firma privados.

La migración 4→5 conserva gastos, nombres históricos, importes y notas; incorpora las siete categorías predeterminadas y los nombres antiguos adicionales con un icono genérico. Las pruebas anteriores también verifican las rutas 1→5, 2→5 y 3→5. Se comprobó persistencia al reabrir Room, orden de creación antes de Otros, rechazo de nombres vacíos/duplicados con distintas mayúsculas o acentos, límite de nombre, iconos desconocidos, referencias inválidas y revisión de respaldo sin cambios después de operaciones rechazadas.

Las copias de formato 4 conservan categorías, iconos y gastos. Se verificó la adaptación del formato 3, manteniendo las pruebas de formatos 1 y 2; las categorías históricas conservan sus nombres. La restauración y el borrado se comprobaron junto con métodos de pago e historial: borrar elimina las personalizadas y deja las siete categorías iniciales.

El recorrido crea una categoría, busca un icono por su sinónimo “neumático”, conserva nombre e icono al recrear la Activity, crea un gasto y cambia su categoría al editarlo. Después borra los datos y verifica la desaparición de la categoría personalizada. La prueba se ajustó para seleccionar el campo dentro del formulario de edición, evitando confundirlo con el texto del historial, y para esperar a que desaparezca el aviso temporal antes de pulsar el botón de borrado.

Se revisaron las capturas reales en `.local/qa/categorias-gastos/`: lista, selector de iconos, búsqueda, formulario de gasto, Ajustes y confirmación de borrado. Las celdas del catálogo se ampliaron para evitar partir nombres largos; las comprobaciones de claro, oscuro y texto al 130 % verifican su legibilidad. El almacenamiento de las sesiones de emulador fue desechable y se cerraron al terminar.

**Límite de esta validación:** Drive usa un servicio simulado en las pruebas. Se comprobaron serialización y restauración sin modificar copias de la cuenta real. La instalación en el teléfono se realiza con el APK indicado, conservando el paquete y la firma.

---

# Validación histórica 0.4.0 · Métodos de pago y Ajustes

16 de octubre de 2026. Rama `20261016-metodos-pago-ajustes`, Gradle 9.4.1, JDK integrado de Android Studio, emulador Medium Phone API 37 en modo desechable.

- **APK debug generado y firmado:** `.local/releases/FuelStatus-0.4.0-metodos-de-pago.apk` (37 188 655 bytes), paquete `mx.fuelstatus`, versionCode 4.
- **61 pruebas JVM aprobadas, sin omisiones:** 10 de estadísticas, 14 de CSV (incluida la comprobación opcional del archivo privado), 30 del motor de respaldo, 3 del respaldo de métodos de pago y 4 del contrato REST de Drive.
- **21 pruebas instrumentadas aprobadas:** 10 de Room general, 3 de persistencia/migración de métodos de pago, 5 de diálogos, 2 de Ajustes de Drive y 1 del recorrido completo por la app.
- **Android Lint:** cero errores y 19 avisos. El logo PNG de Visa está en `drawable-nodpi` para conservar sus proporciones sin escalado por densidad.
- **Firma compatible con las entregas anteriores:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `29ab70967dbf703c970287b259046761bffe9194955d44a76399d6deb5ba6691`.
- Se comprobó que el APK incluye los avisos de los recursos visuales y no contiene CSV, bases de datos ni historiales privados.

Las pruebas de datos verifican las migraciones 1→4, 2→4 y 3→4, conservación de importes/notas/combustible, método histórico sin especificar, Efectivo disponible por defecto, integridad de referencias, rechazo de métodos inexistentes sin avanzar la revisión, nombres duplicados, persistencia después de reabrir Room, restauración conjunta de métodos e historial y borrado completo que vuelve a dejar únicamente Efectivo.

Las pruebas de importación verifican que el método elegido se aplique a los llenados nuevos, que repetir un CSV con otra selección no cambie duplicados y que el CSV exportado conserve el nombre y marca. Los respaldos de formato 3 conservan tarjetas y referencias; se verificó la conversión de formato 2 y se mantiene la prueba del formato 1, sin deducir pagos históricos. Se rechazan referencias rotas, marcas inválidas y métodos duplicados.

El recorrido de interfaz crea un vehículo, abre Mis métodos de pago, comprueba la validación del nombre vacío, agrega VISA/Mastercard/American Express, recrea la Activity, selecciona una tarjeta en Nuevo llenado y comprueba que aparece debajo del importe. Después borra los datos y verifica que desaparecen las tarjetas personalizadas y permanece Efectivo. El diálogo de importación se comprobó con Efectivo inicialmente seleccionado y con una tarjeta elegida.

Se revisaron las capturas reales de la pantalla completa, selector y etiqueta de pago en `.local/qa/metodos-pago/metodos-lista.png`, `nuevo-llenado.png` y `llenado-pago.png`. Se usó un historial sintético adicional para comprobar nombres largos, método sin especificar, tema claro/oscuro y letra al 130 %; capturas `historial-claro.png`, `historial-oscuro.png` e `historial-texto130.png`. El emulador se cerró y su almacenamiento desechable se descartó al terminar.

**Límite de esta validación:** las pruebas remotas usan un servicio de Drive simulado. La serialización, restauración y contrato REST se comprobaron sin operar sobre respaldos de la cuenta real. La instalación y autorización reales en el teléfono quedan como comprobación externa.

---

# Validación histórica visual · Iconos de Llenados

16 de octubre de 2026. Rama `20261016-iconos-llenados`, emulador Medium Phone API 37 en modo desechable.

- **APK debug generado:** `.local/releases/FuelStatus-0.3.0-iconos-llenados.apk` (36 981 775 bytes), con la misma firma de las entregas anteriores.
- **Compilación y Android Lint aprobados:** cero errores y 19 avisos. No se añadieron dependencias ni se cambiaron cálculos o persistencia.
- **Prueba existente de navegación aprobada:** `NavigationTest`, una prueba instrumentada que recorre las secciones, guarda un llenado, cambia el tema y comprueba el borrado y reinicio de la app.
- **Revisión visual:** tarjetas con tanque lleno y carga parcial, iconos de kilometraje/litros/precio/rendimiento, tema claro, tema oscuro y texto al 130 %. Los estados se distinguen y el kilometraje y estado del tanque pasan a otra línea cuando es necesario.
- Los datos para esta revisión son sintéticos y solo se cargaron en el emulador. Las capturas se conservan en `.local/qa/iconos-llenados/claro.png`, `oscuro.png` y `oscuro-texto130.png`; no se publican bases de datos de prueba ni historiales en el APK o repositorio.
- **SHA-256 del APK:** `69263846768338c249b0c0f3711645cb011f4d5feb5632149cb875e72558c460`.

Los seis iconos oficiales de Material Symbols Rounded se incluyen como VectorDrawable y usan los colores del tema. Los textos visibles conservan el significado para accesibilidad; los iconos decorativos no añaden anuncios duplicados. La licencia Apache 2.0 se incluye en los recursos del APK. No se añadieron pruebas nuevas para este cambio visual ni se repitieron las pruebas de negocio de la entrega anterior.

---

# Validación 0.3.0 · Datos, combustible e importación

16 de octubre de 2026. Android Studio/JDK de esta Mac, Gradle 9.4.1, SDK 37 y emulador Medium Phone API 37 en modo desechable.

- **APK debug 0.3.0 compilado y firmado:** `app/build/outputs/apk/debug/app-debug.apk`; entrega en `.local/releases/FuelStatus-0.3.0-datos-combustible.apk` (36 892 446 bytes).
- **56 pruebas JVM aprobadas, sin omisiones:** 10 de estadísticas, 12 de CSV (incluida la comprobación opcional del archivo privado), 30 de respaldo/sincronización/restauración/borrado y 4 del contrato REST de Drive.
- **17 pruebas instrumentadas aprobadas:** 10 de Room, 4 de diálogos de borrado/importación, 2 de Ajustes de Drive y 1 del recorrido completo por la app.
- **Android Lint:** cero errores y 19 avisos. No se suprimieron avisos para obtener este resultado.
- **Firma compatible con la versión anterior:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `098a17cbf66770ab1112d38b6b20c0fbf165ea26ea9d6be89e78173eb67bc8ad`.
- Se comprobó que el APK no contiene CSV, bases de datos ni archivos de historial privados.

La validación de Room cubre migraciones 1→3 y 2→3 conservando el historial, combustible predeterminado y desconocido en registros antiguos, importación a un destino elegido, creación del vehículo encontrado, selección de una fuente entre varios vehículos, duplicados, conservación de ediciones realizadas con versiones anteriores, separación por vehículo y reinicio sin datos después de volver a inicializar.

Las pruebas del motor de respaldo cubren borrado local sin conexión conservando Drive, limpieza de archivos de recuperación, reinicio de apariencia y sesión, borrado remoto completo, fallo de red, borrado parcial con detalle del avance, cuenta incorrecta, reintento y coordinación con una subida que ya estaba en curso. El contrato REST verifica el uso de `PATCH` con `trashed=true` para las copias seleccionadas.

La prueba de interfaz crea un vehículo Premium, comprueba que el nuevo llenado lo selecciona de forma predeterminada, guarda ese llenado con Magna y verifica que el vehículo mantiene Premium. También cancela y confirma el borrado, comprueba que desaparece el vehículo y recrea la Activity para verificar que no reaparece. Los cuatro diálogos verifican, entre otros casos, la casilla de Drive desactivada por defecto y la importación cuando aún no hay vehículos.

La primera ejecución del recorrido completo no encontraba el botón de borrado porque la lista de Ajustes todavía no había compuesto ese elemento fuera de pantalla. Se corrigió el desplazamiento de la prueba; la ejecución completa posterior aprobó las 17 pruebas. Se revisaron las capturas de la tarjeta roja y del diálogo en `.local/qa/feature-ajustes-borrar.png` y `.local/qa/feature-confirmacion-borrar.png`.

**Límite de la validación:** las operaciones de Drive usan un servicio simulado. No se probó el borrado remoto con la cuenta real ni se eliminaron respaldos del usuario. Queda pendiente la comprobación del APK en el teléfono con la autorización real de Google.

---

# Validación histórica 0.2.0 · Google Drive

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
