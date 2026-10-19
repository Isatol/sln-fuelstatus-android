# Validación 1.0.0 · Ajustes y borrado exclusivamente locales

19 de octubre de 2026, America/Monterrey. Rama `20261019-retirar-google-drive`, creada desde `main` limpia en `1ff71e6` después de `pull --ff-only` con el remoto SSH personal.

- **Versión:** 1.0.0, `versionCode` 9, paquete `mx.fuelstatus`. El pie de Ajustes muestra 1.0.0. Room continúa en esquema 6 y las copias completas privadas en formato 5.
- **Interfaz:** se retiraron la card, conexión, cuenta, sincronización y restauración remotas. Ajustes y Borrar todos los datos no mencionan Drive ni presentan su casilla de borrado. Se conserva la confirmación local, cancelar y el bloqueo mientras la app está ocupada. El texto del CSV aclara que exporta llenados y que gastos, catálogos y ajustes permanecen en el teléfono.
- **Retiro efectivo:** se eliminaron autorización, transporte de red y dependencias de Google. Los manifiestos generados debug/release no incluyen Internet ni estado de red; tampoco aparecen el SDK de autorización ni el alcance `drive.file` en los DEX del APK. El motor conserva únicamente las copias privadas locales. La clase y nombre del worker anterior se mantienen como identificadores de compatibilidad: se cancela su trabajo y cualquier ejecución retenida termina sin operaciones remotas.
- **98 pruebas JVM aprobadas:** 99 registradas, una omitida por requerir el CSV privado. Se conservaron los casos del códec, formatos anteriores y catálogos; se retiraron los casos exclusivos de la integración remota eliminada. Los nuevos casos cubren actualización/reutilización de la copia privada, regeneración de archivos dañados, fallo del archivo auxiliar sin perder el guardado de Room, cancelación, borrado de historial/recuperación/catálogos, preservación de copias ante fallo del reinicio y coordinación entre copia y borrado.
- **6 casos Android dirigidos aprobados**, en una sola ejecución (`OK (6 tests)`): migración de configuración con trabajo antiguo pendiente, conservación de historial/ajustes/archivo privado/identidad de dispositivo, cancelación efectiva del trabajo, ausencia de permisos de red; borrado desde MainActivity con cancelar, confirmar y reabrir vacío; Ajustes/CSV en claro y oscuro con texto al 130 %, y confirmación/bloqueo de borrado. La prueba de actualización usa una base y preferencias aisladas con datos sintéticos.
- **Revisión final:** después de ajustar únicamente el texto explicativo del CSV se recompilaron APK/AAB y se repitió solo el caso de Ajustes afectado (`OK (1 test)`). Se revisaron las capturas definitivas en claro y oscuro al 130 %, la card de borrado y su confirmación. No se volvieron a ejecutar los otros cinco casos ni las pruebas JVM, ya aprobadas sobre la misma lógica de datos.
- **Compilación y Lint:** APK de aplicación, APK de pruebas y AAB release generados. Android Lint sin errores y con 8 avisos; la compilación release también superó `lintVitalRelease`. No se repitieron las suites Android completas ni la navegación general.
- **APK:** `.local/releases/FuelStatus-1.0.0-sin-drive.apk`, SHA-256 `614c1737d5540a3f2013d216d36571a027386285328430246222a04a5b915783`. Firma debug anterior conservada, SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **AAB release sin firma:** `.local/releases/FuelStatus-1.0.0-sin-drive-release-sin-firma.aab`, SHA-256 `72eb3c489965d15282789b2ddfb3be3f588ef0953ae15593f3dbfeab94b494a8`. Es un artefacto técnico pendiente de firma de subida y configuración de Play; no se publicó ni se configuró el precio en la tienda.

Registros y capturas en `.local/qa/retirar-google-drive/`. Gradle y el emulador API 37 se ejecutaron por separado; el emulador se utilizó read-only, sin snapshots y en modo avión, cerrado al terminar. Se usaron datos sintéticos y no se operó sobre cuentas, archivos remotos o teléfonos personales. La versión conserva el CSV de llenados y el formato completo privado, pero no incorpora botones nuevos para exportar o restaurar copias completas; sus límites se describen en [COPIAS_LOCALES.md](COPIAS_LOCALES.md).

---

# Validación histórica 0.8.0 · Inicio limpio y retiro de importación desde Fuel

18 de octubre de 2026, America/Monterrey. Rama `20261018-inicio-limpio-sin-card-fuel`, desde `main` en `c0f1542` (PR #18).

- **Interfaz:** se retiraron la card de importación desde Fuel, su botón, el selector de archivos asociado y las referencias de los estados vacíos de Llenados y Vehículos. El diálogo de importación queda dedicado a FuelStatus; se retiraron los controles exclusivos del flujo anterior. Ajustes conserva importar y exportar llenados de FuelStatus.
- **Inicio limpio:** la inicialización existente ya crea cero vehículos, llenados y gastos, solo Efectivo y las siete categorías iniciales, con ajustes predeterminados. No fue necesario modificar la lógica de inicialización ni de borrado. Se comprobó además el primer arranque real de la app, extrayendo la base del emulador y verificando esas tablas.
- **8 pruebas Android dirigidas aprobadas**, en una sola ejecución (`OK (8 tests)`). La prueba de reinicio compara la copia completa de una base nueva con el resultado de borrar historial, tarjetas y categorías personalizadas, una categoría inicial eliminada y apariencia modificada, y de volver a inicializar dos veces. Otros casos cubren las dos acciones CSV de Ajustes en claro y oscuro con texto al 130 %, ausencia de la card y botón anteriores, controles deshabilitados, selección de vehículo coincidente u otro destino, creación sin vehículos, conservación de combustible/pagos/precisión/indicadores y duplicados sin recrear tarjetas eliminadas ni reemplazar ediciones.
- **Compilación aprobada:** APK de aplicación y APK de pruebas. Android Lint: cero errores y 11 avisos existentes.
- **Revisión visual:** primer arranque sin vehículos ni llenados, Vehículos vacío con su botón para añadir el primero y Ajustes sin la card retirada en claro y oscuro con texto al 130 %. Registros, capturas y estado inicial en `.local/qa/inicio-limpio/`.
- **Compatibilidad:** Room 6, respaldo completo 5, formato CSV de FuelStatus y datos existentes conservados. No cambian cálculos, migraciones ni el comportamiento de restauración.
- **APK:** `.local/releases/FuelStatus-0.8.0-inicio-limpio.apk`, SHA-256 `3625d9988989206423321f0881dfa125847f238a5ade81bab9add066a0b51a3f`. Firma debug anterior conservada, SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

Se usaron exclusivamente datos sintéticos en el emulador API 37 read-only, sin snapshots y en modo avión, cerrado al terminar. Gradle y emulador se ejecutaron por separado. No se repitieron las suites JVM/Android completas ni la navegación general; se ejecutaron los casos afectados por este cambio. No se operó sobre Drive real ni teléfonos personales. El historial de validaciones que sigue conserva las características y resultados de versiones anteriores.

---

# Validación histórica 0.8.0 · Textos, formularios y catálogo Material

18 de octubre de 2026, America/Monterrey. Rama `20261018-iconos-formularios-categorias`, desde `main` en `d5986e8` (PR #17).

- **APK debug compilado y firmado:** versión 0.8.0, versionCode 8. Android Lint: cero errores y 11 avisos existentes.
- **116 pruebas JVM aprobadas:** 117 registradas y una omitida por requerir el CSV privado. El caso nuevo verifica que los 45 códigos Material se conserven en respaldos de formato 5.
- **11 de 12 pruebas Android dirigidas aprobadas:** vehículo, catálogo, persistencia/restauración de categorías, gastos y Ajustes/CSV. La prueba restante confundió el texto editable `credit_card` con el resultado del mismo nombre. Se corrigió su selector para excluir campos editables; no se repitió la ejecución, atendiendo la petición del usuario de cerrar la entrega. No se atribuye ese fallo a la app ni se afirma una ejecución de 12 casos aprobados.
- **Revisión visual:** formulario de vehículo en claro y oscuro con texto al 130 %, formulario de gasto en claro y oscuro, catálogo con nombres originales en claro y búsqueda en español en oscuro con texto al 130 %. Se revisaron los nombres largos y los iconos de fecha, total y notas. La prueba de vehículo comprobó que el selector conserva Premium y los datos escritos al guardar.
- **Revisión manual:** encabezado con FuelStatus sin «En tu teléfono» y final de Ajustes sin el bloque «Tu auto, tus datos» ni su párrafo. Capturas y árboles de UI guardados en `.local/qa/iconos-formularios-categorias/`.
- **Catálogo:** se verificó que los 45 códigos coinciden exactamente con todos los recursos Material locales distintos y sus referencias. Se conservan los 30 códigos anteriores y se agregan 15. No cambian Room 6, respaldo 5 ni CSV. Versiones antiguas necesitan actualizarse para restaurar copias que utilicen los códigos nuevos.
- **Entrega:** `.local/releases/FuelStatus-0.8.0-iconos-formularios-categorias.apk`, SHA-256 `3b28020af882c2f3e8bd07f30b01ea2b04f026221ed2acf2528e91f6953f10ac`. Firma anterior conservada: SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

El emulador API 37 se usó read-only, sin snapshots y en modo avión; se cerró al terminar. Un aviso de System UI durante la revisión manual se resolvió con Esperar. Gradle y emulador se ejecutaron por separado. Se usaron datos sintéticos; no se operó sobre Drive real ni teléfonos personales. No se repitió la navegación general ni la suite Android completa. El ajuste final afecta únicamente al selector de la prueba; el APK entregado es el mismo compilado y revisado.

---

# Validación histórica 0.8.0 · Color del aro del icono

18 de octubre de 2026, America/Monterrey. Rama `20261018-color-aro-icono`, creada desde `main` en `f531006` (PR #16), con el remoto SSH personal.

- **Compilación aprobada:** APK debug 0.8.0, versionCode 8 y APK de pruebas. Android Lint: cero errores y 11 avisos existentes.
- **115 pruebas JVM aprobadas:** 116 registradas y una omitida por requerir el CSV privado. Se añadieron casos de las siete opciones en respaldos de formato 5, copias anteriores sin el campo nuevo y rechazo de colores inválidos o nulos.
- **21 pruebas Android dirigidas aprobadas** en una ejecución (`OK (21 tests)`): cuatro nuevas del aro, tres de paletas internas, dos de Ajustes/CSV, dos de interfaz de Drive y diez generales de Room/importación/migraciones/restauración/borrado. Las nuevas comprueban independencia entre aro y tema, texto al 130 % en claro y oscuro, controles deshabilitados, persistencia, reconciliación inicial y recuperación ante fallos sintéticos del launcher. También verifican que restaurar o borrar conserva el historial anterior si falla el cambio del icono.
- **Siete aliases comprobadas en PackageManager:** cada elección publica una sola actividad de launcher, mantiene habilitada MainActivity y admite reaplicación sin duplicados. Se compararon los píxeles del surtidor y de la capa monocromática de cada variante con el original: idénticos. Solo cambia el aro del primer plano a color.
- **Revisión manual en Pixel Launcher / API 37:** antes de abrir la app se publicó únicamente AndroidIcon. Desde Ajustes se eligió Azul, se comprobó BlueIcon como única entrada y se revisó su aro azul en el cajón. Después de detener el proceso, tocar ese icono abrió MainActivity y mantuvo `icon_ring=blue`; el tema interno siguió en Sistema con color dinámico. Captura `drawer-blue.png`, preferencias y consultas del launcher conservadas junto a los registros.
- **APK de entrega:** `.local/releases/FuelStatus-0.8.0-color-aro-icono.apk`, 38 094 354 bytes. SHA-256 `23f425a00ea99983bee44d401f0a9cf639ace0ea54d652b8a93a7a71fdc2e351`. Firma debug anterior conservada, SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

Registros y capturas: `.local/qa/color-aro-icono/`. Se revisaron las capturas de Azul en claro y Gris en oscuro con texto al 130 %. El emulador se ejecutó read-only, sin snapshots y en modo avión, por separado de Gradle, y se cerró al terminar. Durante la revisión manual aparecieron avisos de proceso del sistema y System UI sin respuesta, resueltos con Esperar; la comprobación del launcher se completó después. Una lectura de UI durante la transición de reapertura devolvió raíz nula; se repitió tras finalizar la transición y se confirmó la actividad visible.

**Límites:** no se ejecutaron Android 12/12L ni otros launchers, ni se probó activar iconos temáticos o cambiar el fondo de pantalla en ejecución. La conservación de la silueta temática sí se comprobó por píxeles para todas las variantes. La app no controla qué superficies tematiza cada launcher ni la conservación de accesos fijados a una alias que se deshabilita. Estos límites se explican en Ajustes y en [Icono de FuelStatus](ICONO.md). No se repitió la suite Android completa ni la navegación general. Se usaron datos sintéticos; no se operó sobre Drive real ni teléfonos personales. Se conservan Room 6, respaldo 5 y CSV; el campo nuevo es opcional para respaldos anteriores.

---

# Validación histórica 0.8.0 · Icono con aro verde ampliado

18 de octubre de 2026, America/Monterrey. Rama `20261018-icono-aro-verde`, creada desde `main` en `6c055c2` (PR #15), verificada contra el remoto SSH personal.

- **Icono aprobado:** surtidor original verde oscuro, aro verde claro sin relleno y margen blanco exterior. El aro ocupa el 80 % del viewport circular nominal y queda dentro de la zona segura del icono adaptable. El manifiesto utiliza el mismo recurso para el icono normal y el redondo; la capa monocromática conserva la ventana transparente del surtidor.
- **Compilación limpia aprobada:** APK debug 0.8.0, versionCode 8. Se regeneraron los intermediarios después de simplificar las variantes del recurso.
- **Android Lint aprobado:** cero errores y 11 avisos anteriores, sin avisos nuevos del icono. El análisis final también se repitió después de eliminar las carpetas vacías de las variantes anteriores.
- **Revisión visual manual en Android 37:** instalación con `-r`, sin borrar datos, y comprobación del icono en el cajón de aplicaciones del launcher. Captura completa `launcher-drawer.png` y recorte sin retoques `launcher-icono.png`, en `.local/qa/icono-aro-verde/`.
- **Correspondencia del APK final:** las capturas se tomaron antes de unificar las variantes de recursos. Se compararon por SHA-256 los XML binarios del primer plano y del icono adaptable; los recursos del APK final son idénticos a los revisados en Android 37. Los resultados están en `visual-apk-resource-hashes.json` y `final-apk-resource-hashes.json`.
- **APK:** `.local/releases/FuelStatus-0.8.0-icono-aro-verde.apk`. SHA-256: `2bd528617ba908e41dc13184bc619cd9f6497e69cbad913b7a133fdad662b6af`. Conserva la firma debug anterior, SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

El emulador Medium Phone API 37 se utilizó read-only, sin snapshots y sin conexión; se cerró antes de recompilar. Al arrancar mostró un aviso de System UI sin respuesta, resuelto con Esperar; la captura inicial se conserva como `launcher-system-ui-anr.png`. La revisión del launcher se completó después de esa recuperación.

El cambio solo afecta recursos del icono y referencias del manifiesto. No se añadieron pruebas ni se ejecutaron las suites JVM o Android instrumentadas, navegación o restauraciones. No se verificaron en ejecución Android 12/12L, otros launchers ni la activación de iconos temáticos; la capa monocromática se incluyó y validó durante el empaquetado. No cambian datos, Room 6, respaldo 5, CSV ni el tema dentro de la app. No se operó sobre Drive real ni dispositivos personales. Detalles del recurso en [Icono de FuelStatus](ICONO.md).

---

# Validación histórica 0.8.0 · Selector de paletas de Tema

18 de octubre de 2026, America/Monterrey. Rama `20261018-selector-paletas-tema`, creada desde `main` en `06e0f2b`, verificada contra el remoto SSH personal.

- **113 pruebas JVM aprobadas:** 114 registradas, una omitida porque requiere el CSV privado. Los dos casos nuevos verifican la paleta en respaldos de formato 5, el rechazo de códigos inválidos y la restauración de copias anteriores sin perder su elección de color dinámico.
- **7 casos Android dirigidos aprobados, en ejecuciones separadas:** tres nuevos de paletas, persistencia y contraste; dos de Ajustes/CSV y dos de la interfaz de Drive. Se comprobaron aplicación inmediata de Azul, cambio a oscuro, Gris con texto al 130 %, regreso a la paleta activa de Android, controles deshabilitados, preferencias anteriores y contraste mínimo de 4.5:1 en los pares de texto evaluados para las seis paletas en claro y oscuro.
- **Compilación y Android Lint aprobados:** APK de aplicación y de pruebas generados, cero errores y 11 avisos existentes. Las paletas están generadas como constantes; no se añaden dependencias en ejecución.
- **Revisión visual:** cuatro capturas de Android y Azul en claro, Gris en oscuro con texto al 130 % y Android en oscuro al 130 % con controles deshabilitados. Registros y capturas en `.local/qa/selector-paletas/`.
- **Compatibilidad:** se conservan Room 6 y respaldo 5. `appearance.colorPalette` es opcional para las copias anteriores; los clientes anteriores ignoran ese campo y muestran su verde cuando el color dinámico está desactivado. No cambia el CSV. Detalles en [Tema y colores](APARIENCIA.md).
- **APK:** `.local/releases/FuelStatus-0.8.0-selector-paletas.apk`, versión 0.8.0, versionCode 8. SHA-256: `5ab4492cf5042d22ff9d1c8c06d8c09351b9467cc7b5a6e75e842dfd89706ba8`. Conserva la firma debug anterior, SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

En las dos ejecuciones iniciales del grupo de siete, seis casos aprobaron y el caso de botones CSV falló al intentar actuar sobre un elemento fuera de pantalla. Se actualizó esa prueba para buscar el botón a través de la lista con `performScrollToNode`, comprobar su visibilidad y leer los contadores con `runOnIdle`. Su repetición final aprobó con `OK (1 test)` en `android-scroll.txt`; los registros de los intentos iniciales se conservan. No se volvió a ejecutar el grupo completo después de ese ajuste exclusivo de la prueba.

Se usaron datos sintéticos, sin operaciones sobre Drive real ni dispositivos personales. El emulador Medium Phone API 37 se ejecutó read-only y sin snapshots, por separado de Gradle, y se cerró al terminar. No se repitieron la suite Android completa ni la navegación general.

---

# Validación histórica 0.8.0 · Iconos de pago, CSV y Ajustes

18 de octubre de 2026, America/Monterrey. Rama `20261018-iconos-tarjetas-csv-ajustes`, creada desde `main` actualizada al merge `85978f9` (PR #13).

- **111 pruebas JVM aprobadas:** 112 registradas, una omitida por requerir el CSV privado. El mensaje de CSV inválido identifica FuelStatus cuando se usa ese flujo; se conservan las pruebas existentes de precisión, metadatos, formatos y respaldos.
- **29 pruebas Android dirigidas aprobadas:** dos nuevas de importación nativa, dos nuevas de Ajustes/vista previa, cinco de diálogos, diez generales de Room/importación/migraciones, tres de métodos de pago, tres de pagos en gastos y cuatro de eliminación de catálogos. No se repitieron la suite Android completa ni la navegación general.
- **Las dos pruebas nuevas de interfaz también aprobaron con el texto Android al 130 %** sobre el APK final, después del ajuste singular del contador.
- **CSV de FuelStatus:** selección de vehículo del archivo y destino existente/nuevo; conservación de combustible por fila, Efectivo, las tres marcas de tarjeta y Sin especificar; reutilización de tarjetas por marca y nombre sin distinguir mayúsculas; precisión monetaria, odómetro, notas multilínea, gasolinera y todos los indicadores. Los duplicados editados no se sobrescriben ni recrean tarjetas eliminadas. El flujo de Fuel mantiene su combustible y método comunes opcionales.
- **Interfaz:** los tres botones CSV llaman a acciones distintas y se desactivan mientras está ocupado. La vista previa de FuelStatus propone un vehículo coincidente o crearlo al cambiar el origen. Se revisaron capturas claras/oscuras y texto ampliado en Ajustes y en el modal. Se corrigió el contador singular «1 llenado».
- **Iconos:** el componente compartido de pagos usa Material para las tarjetas, Efectivo y Sin especificar. Se eliminaron las tres imágenes de marcas y su aviso específico. La inspección del APK confirmó que no incluye esos recursos; no quedan referencias a sus identificadores, `PaymentLogo` ni al subtítulo eliminado en `app/src/main`.
- **Compilación y Android Lint aprobados:** cero errores y 11 avisos existentes. Se mantiene Room 6, respaldo 5 y el formato CSV exportado.
- **APK:** `.local/releases/FuelStatus-0.8.0-iconos-csv-ajustes.apk`, versión 0.8.0, versionCode 8. SHA-256 `9616f883641cc5dfcac7a9e571e02b84095f2d3012a8612240419f0944fe9df3`. Firma debug anterior verificada: SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

Registros y capturas locales en `.local/qa/iconos-csv-ajustes/`. Se usaron datos sintéticos y un emulador Medium Phone API 37 read-only, sin snapshots, cerrado al terminar; Gradle y el emulador se ejecutaron por separado. No se hicieron operaciones sobre Drive real ni dispositivos personales. El CSV importa un vehículo elegido por operación y contiene llenados; los gastos, ajustes y el catálogo completo se conservan mediante respaldos completos.

---

# Validación histórica 0.8.0 · Eliminar tarjetas y categorías

18 de octubre de 2026, America/Monterrey. Rama `20261018-eliminar-tarjetas-categorias`. Mis métodos de pago usa el icono Material de tarjeta de crédito. Las listas permiten deslizar hacia la izquierda para pedir confirmación de eliminación; el modal muestra asociaciones y explica la reasignación a Efectivo u Otros. Ambas opciones están protegidas.

- **110 pruebas JVM aprobadas:** 111 registradas, una omitida por requerir el CSV privado. Incluye respaldo con categorías iniciales eliminadas y con solo Otros, sin cambiar el formato 5.
- **22 casos Android dirigidos aprobados:** cuatro nuevos de datos, cuatro nuevos de interfaz y catorce existentes de categorías, pagos, migraciones, persistencia, restauración y formularios. La validación se realizó por grupos, sin repetir la suite Android completa ni la navegación general.
- **Las cuatro pruebas nuevas de interfaz también aprobaron con el texto de Android al 130 %** en la repetición final. El cierre con Atrás se volvió a comprobar con texto normal tras ajustar el envío de la tecla.
- **Datos:** reasignación en llenados y gastos de varios vehículos, conservación de todos los demás campos, métodos ajenos y Sin especificar; eliminación sin asociaciones; protección también en repositorio/DAO; una revisión por eliminación; reversión íntegra ante un error provocado; categorías eliminadas ausentes tras reabrir, inicializar y restaurar; borrado completo restablece el catálogo inicial.
- **Interfaz:** gesto izquierdo, gesto derecho sin eliminación, cancelar y repetir, confirmación explícita, Atrás, bloqueo mientras está ocupado, categorías iniciales y personalizadas, protección de Efectivo/Otros y Nuevo gasto en Otros cuando Servicio ya no existe.
- **Compilación y Android Lint aprobados:** cero errores y 11 avisos existentes. Room continúa en versión 6; no se cambian columnas ni migraciones.
- **Revisión visual:** iconos y modales en claro y oscuro, incluido texto ampliado al 130 %. Capturas y registros locales en `.local/qa/eliminar-tarjetas-categorias/`.
- **APK:** `.local/releases/FuelStatus-0.8.0-eliminar-tarjetas-categorias.apk`, versión 0.8.0, versionCode 8. SHA-256 `f94af7c2df4564080ad50fa267a1106fd6b55b3eb776c7c458447ced726be113`. Firma debug anterior verificada: SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.

Las primeras pruebas del modal tuvieron tres fallos por un selector ambiguo que también encontraba el texto del fondo del gesto. Se acotó el selector al diálogo; las cuatro pruebas pasaron después. La repetición ampliada detectó además una pérdida de foco del selector de ventana de Espresso al enviar Atrás; la prueba ahora envía la tecla al sistema.

Las pruebas usan datos sintéticos y un emulador Medium Phone API 37 read-only, sin snapshots, cerrado al terminar. Gradle y el emulador se ejecutan por separado. No se hicieron operaciones sobre Drive real ni dispositivos personales. Esta versión sigue leyendo las copias anteriores; las versiones antiguas que exigían todas las categorías iniciales no admiten restaurar un catálogo reducido.

---

# Validación histórica 0.8.0 · Información por tarjeta de Estadísticas

18 de octubre de 2026, America/Monterrey. Rama `20261018-informacion-estadisticas`. Las 16 tarjetas disponibles de Estadísticas incluyen un botón ⓘ que abre una explicación propia con la fuente de datos, fórmula, requisitos y alcance del cálculo. Se elimina el bloque general “Cómo calculamos” de la lista.

- **Compilación y Android Lint aprobados:** cero errores y 11 avisos existentes. Se generaron los APK de aplicación y pruebas.
- **10 pruebas Android dirigidas aprobadas con texto normal y las mismas 10 al 130 %**, veinte ejecuciones finales: tres nuevas de información, cuatro de Estadísticas y tres de proyecciones. Se verificaron las 16 ayudas, sus descripciones accesibles, botones de 48 dp, texto desplazable, cierre con Entendido y Atrás, conservación de apertura con `StateRestorationTester`, ayuda sin datos y ausencia del bloque general.
- **Revisión visual:** claro, oscuro, títulos de tarjetas estrechas y modales con texto al 130 %. Capturas y registros locales en `.local/qa/info-estadisticas/`.
- **Entorno:** emulador Medium Phone API 37 read-only, sin snapshots, cerrado al terminar. Gradle y emulador ejecutados por separado.
- **APK:** `.local/releases/FuelStatus-0.8.0-info-estadisticas.apk`, versión 0.8.0, versionCode 8. SHA-256: `41086a33e915441f36a74e79ec9511753503c1a7696f3948467e2eb896764e06`.

El cambio se limita a interfaz y explicaciones. No se modifican cálculos, datos, migraciones, Room 6 ni respaldo 5. No se repitieron las pruebas JVM, la suite Android completa ni la navegación general; se actualizó el selector de esta última para usar Últimos intervalos en lugar del bloque eliminado. Las pruebas usaron datos sintéticos, sin operaciones sobre Drive real.

---

# Validación histórica 0.8.0 · Proyecciones y métricas recientes

17 de octubre de 2026, America/Monterrey. Rama `20261017-proyecciones-estadisticas`. Se incorporan seis tarjetas: próximo llenado estimado, alcance de una carga habitual, costo reciente por 100 km, tendencia del precio por litro, ahorro potencial y gasto estimado del mes. Costo y precio comparten fila cuando hay espacio; las explicaciones largas ocupan una fila completa.

- **109 pruebas JVM aprobadas:** 110 registradas, una omitida por requerir el CSV privado. Los 16 casos nuevos verifican ponderación, volumen habitual con cargas parciales, frecuencia y rango de fechas, mediana de los últimos seis intervalos, huecos/exclusiones/retrocesos, cero días, cambio de horario, zona local, registros futuros, combustible comparable, ahorro/mejora/estabilidad, meses bisiestos, inicio del historial y cierre mensual.
- **7 pruebas Android dirigidas aprobadas con texto normal** (tres nuevas de proyecciones y cuatro existentes de Estadísticas) y **las mismas 7 aprobadas con texto Android al 130 % en la repetición final**. Se ajustó el desplazamiento de la prueba de frecuencia para llevar cada valor a la vista antes de comprobarlo. No se repitió la suite Android completa ni la navegación general.
- **Compilación y Android Lint aprobados:** cero errores y 11 avisos existentes. Gradle y el emulador se ejecutaron por separado.
- **Revisión visual:** claro, oscuro, tarjetas de costo/precio en una fila y adaptación a una columna al 130 %. Capturas y registros locales en `.local/qa/proyecciones/`. Emulador Medium Phone API 37 read-only, sin snapshots, cerrado al terminar.
- **APK:** `.local/releases/FuelStatus-0.8.0-proyecciones.apk`, versión 0.8.0, versionCode 8. SHA-256: `ee8a24de93a9c341add0ce93624eb8dc744f80da2940cd1317ae394afc33ed9e`.

Las proyecciones son condicionales a los hábitos registrados. No se infiere combustible restante ni capacidad del tanque; los rangos de fechas no se presentan como probabilidades. Se conservan datos, migraciones, Room 6 y respaldo 5. Las pruebas usan datos sintéticos y servicios de respaldo simulados, sin operaciones sobre Drive real.

---

# Validación histórica 0.8.0 · Equivalencias de tendencia de consumo

17 de octubre de 2026, America/Monterrey. Rama `20261017-equivalencias-tendencia-consumo`. La tarjeta de tendencia muestra el rendimiento anterior y reciente en km/L, la diferencia estimada de litros para la distancia reciente y la comparación de kilómetros dentro de la misma tarjeta, conservando las fechas de ambos periodos.

- **93 pruebas JVM aprobadas:** 94 registradas, una omitida por requerir el CSV privado, cero fallos y errores. Los cuatro casos nuevos comprueban litros equivalentes con distancias diferentes, ahorro con más litros totales, rendimientos iguales y uso de cifras sin redondear el porcentaje; también se amplió el caso de ponderación para km/L.
- **4 pruebas Android dirigidas aprobadas**, repetidas y aprobadas con texto Android al 130 %: ocho ejecuciones en total. Comprueban la redacción en km/L, litros adicionales y de ahorro, distancias iguales/mayores/menores, fechas, recálculo al editar/eliminar, vehículo seleccionado y estados sin datos, estable y aumento importante. No se repitió la suite Android completa ni la navegación general.
- **Compilación y Android Lint aprobados:** cero errores y 11 avisos existentes. Gradle y el emulador se ejecutaron por separado.
- **Revisión visual:** claro, oscuro y texto al 130 %, con capturas en `.local/qa/equivalencias-tendencia/`. Emulador Medium Phone API 37 read-only, sin snapshots, cerrado al terminar.
- **APK:** `.local/releases/FuelStatus-0.8.0-equivalencias-tendencia.apk`, versión 0.8.0, versionCode 8. SHA-256: `6d588e07c4aa52d6dd541036fb7aa0d4a6a37d2c7e780be03af3541ad0e93ec3`.

Se conservan los umbrales y periodos de la tendencia. No se modifican datos guardados, migraciones, Room 6 ni formato de respaldo 5. Las pruebas usan datos sintéticos; no se realizaron operaciones sobre Drive real.

---

# Validación histórica 0.8.0 · Formulario de llenados en filas

17 de octubre de 2026, America/Monterrey. Rama `20261017-formulario-llenados-en-filas`. El formulario compartido por Nuevo llenado y Editar llenado coloca Combustible y Método de pago en una fila, y Litros y Total pagado en otra. Las etiquetas aparecen encima de los cuatro controles.

- **Compilación y Android Lint aprobados:** cero errores y 11 avisos. Se generaron los APK de la aplicación y de pruebas, con Gradle y el emulador ejecutados por separado.
- **Prueba dirigida aprobada en la versión final:** `RefuelHistoryTest#refuelFormStillSavesInputsAndSwitchesWithIcons`, una vez con texto normal y otra con texto Android al 130 %. Comprueba entradas, opciones, notas y guardado de importes. No se añadieron casos nuevos ni se repitieron las pruebas JVM o la suite Android completa.
- **Revisión visual:** las capturas de ambas escalas muestran las dos parejas completas y alineadas. Archivos locales en `.local/qa/formulario-llenados-filas/`. Emulador Medium Phone API 37 en modo read-only, sin snapshots, cerrado al terminar.
- **Límite de validación:** el recorrido general `NavigationTest` falló en dos intentos durante las iteraciones previas al esperar `Vehículo guardado` (`NavigationTest.kt:28`), antes de abrir Llenados. No se considera aprobado ese recorrido ni se ha determinado la causa de la espera agotada.
- **APK:** `.local/releases/FuelStatus-0.8.0-formulario-llenados-filas.apk`, versión 0.8.0, versionCode 8. SHA-256: `7c9269631d44e1ae50c08d2931c29088604d751d956a0a04faa5ae55c71207c6`.

No se modifican cálculos, datos, Room (versión 6), migraciones ni formato de respaldo (5).

---

# Validación histórica 0.8.0 · Estadísticas de consumo

16 de octubre de 2026, America/Monterrey. Rama `20261016-estadisticas-de-consumo`, Gradle 9.4.1, JDK de Android Studio y emulador Medium Phone API 37 desechable. Gradle y el emulador se ejecutaron por separado.

- **APK debug generado y firmado:** `.local/releases/FuelStatus-0.8.0-estadisticas.apk` (37 543 034 bytes), paquete `mx.fuelstatus`, versionCode 8.
- **82 pruebas JVM aprobadas:** se añadieron 15 casos para las nuevas métricas. Se omitió únicamente la comprobación opcional que requiere un CSV privado; 83 casos registrados, cero fallos y cero errores.
- **4 pruebas Android aprobadas en la ejecución dirigida:** tres nuevas de estadísticas y el recorrido general de navegación. Se adaptó el desplazamiento de este último a la lista ampliada; no se repitió la suite Android completa.
- **Texto al 130 %:** se repitieron y aprobaron los tres casos de estadísticas con el tamaño real de texto de Android.
- **Android Lint:** cero errores y 12 avisos existentes en el análisis local sin conexión.
- **Firma compatible con entregas anteriores:** SHA-1 `81:BA:D0:FD:EB:A0:29:7E:6B:3E:F9:04:FF:6B:1D:AD:80:33:1F:36`.
- **SHA-256 del APK:** `c42bb4e9ad2784fffa54ed9ffe2942cb2409ffc714bebb5b21f6e5a0b38f3f38`.

Los casos de cálculo comprueban la ponderación por distancia, acumulación de cargas parciales, exclusión del tanque inicial, mínimo de seis intervalos completos y comparación de los seis más recientes. Cubren los cuatro estados, límites y redondeo del porcentaje, fechas compartidas entre periodos, interrupciones por cargas faltantes/excluidas o retrocesos del odómetro y una carga parcial con fecha inconsistente. También verifican distancia entre cargas usando N−1 intervalos, medias de días de calendario, cargas el mismo día, cambio de horario y separación por vehículo.

La interfaz comprueba porcentajes y fechas legibles, actualización al editar o eliminar cargas, cambio de medias al seleccionar otro vehículo, estados sin datos y los textos de las métricas existentes. Se revisaron capturas en claro, oscuro y texto al 130 % en `.local/qa/estadisticas/`; las parejas de cards se apilan con texto ampliado. El emulador desechable se cerró al terminar.

Room permanece en versión 6 y el respaldo en formato 5; no se modifican datos guardados ni migraciones. Se verificó que el APK no incluye CSV, bases de datos, historiales ni archivos privados de firma. Las pruebas del motor de Drive utilizan servicios simulados; no se modificó la cuenta real.

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
