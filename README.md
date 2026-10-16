# FuelStatus para Android

Repositorio: [Isatol/sln-fuelstatus-android](https://github.com/Isatol/sln-fuelstatus-android).

Aplicación personal de combustible y gastos de vehículos inspirada en Fuel, con interfaz nativa para Android. Nombre anterior de trabajo: **FuelDroid**.

## Versión 0.3

Borrado de datos con confirmación y opción de borrar las copias de Drive, combustible por vehículo y por llenado, e importación con selección del vehículo de destino. Las instalaciones nuevas comienzan sin vehículos; las actualizaciones conservan el historial existente. Detalles en [docs/DATOS_Y_COMBUSTIBLE.md](docs/DATOS_Y_COMBUSTIBLE.md).

## Lo que definimos

- **Plataforma:** Android nativo con Kotlin y Jetpack Compose; uso principal en un Pixel 10.
- **Diseño:** Material 3 Expressive y Material You, colores del fondo de pantalla o paleta verde propia, tema del sistema/claro/oscuro, interfaz edge-to-edge, tarjetas, navegación inferior flotante y respuesta háptica al navegar y guardar.
- **Llenados:** alta, consulta, edición y eliminación; fecha, odómetro, litros, importe total, precio por litro calculado, tanque completo/parcial, gasolinera y notas. Opciones para excluir una carga de estadísticas o indicar una carga anterior faltante.
- **Gastos:** registro, edición y eliminación de servicios, mantenimiento, seguros, estacionamientos, peajes, lavados y otros gastos.
- **Estadísticas:** km/L, precio medio por litro, costo de combustible por kilómetro, volumen, combustible pagado y gastos totales; cálculos a partir de datos registrados.
- **Vehículos:** historial separado para cada auto, selección, alta, edición y combustible predeterminado. Los vehículos existentes se conservan al actualizar.
- **Ajustes:** apariencia e importación del historial CSV de Fuel. Se agregó exportación CSV de llenados para poder recuperar ese historial.
- **Datos:** persistencia local con Room; funcionamiento sin conexión, sin cuenta, anuncios ni suscripción. Moneda MXN, distancias en km, volumen en litros.
- **Entrega:** proyecto fuente y APK debug instalable. Compilación local con Android Studio; GitHub Actions queda pendiente de configurar.

**Versión 0.2:** copias completas locales y en Google Drive, autorización con Google, reintento sin conexión, sincronización manual, detalle de errores y restauración con revisión previa. Guía: [docs/GOOGLE_DRIVE.md](docs/GOOGLE_DRIVE.md). Ubicación, mapas e informes avanzados siguen pendientes.

## Estado de esta recreación

La base de la implementación contiene las cinco secciones, formularios, Room, cálculos e importación/exportación de llenados. No se incluyen datos de demostración ni el CSV personal dentro del APK. Las instalaciones nuevas comienzan sin vehículos y permiten crearlos o importar uno desde Ajustes.

Se utiliza un CSV privado externo al proyecto como comprobación local opcional; no se copia al repositorio. La comparación visual con Fuel y sus totales sigue pendiente de verificar en el teléfono.

**Validado en 0.3:** APK debug compilado y firmado, 56 pruebas JVM y 17 pruebas instrumentadas aprobadas, Android Lint sin errores (19 avisos). El borrado de Drive se comprobó con un servicio simulado; no se eliminaron copias de la cuenta real. Detalles en [docs/VALIDACION.md](docs/VALIDACION.md).

## Abrir y ejecutar

1. En Android Studio: **Open** y selecciona esta carpeta `sln-FuelStatus`.
2. Espera la sincronización de Gradle. El proyecto usa el JDK integrado en Android Studio, Gradle 9.4.1 y Android Gradle Plugin 9.2.1.
3. Instala **Android SDK Platform 37.0** y **Build Tools 36.0.0** desde SDK Manager si el IDE los solicita.
4. Selecciona el Pixel con depuración USB habilitada o un emulador y pulsa **Run**.

Requiere Android 12 o posterior (API 31). Compila con SDK 37 y apunta a API 36. `local.properties` contiene la ruta local del SDK y no debe subirse a Git.

Desde una terminal con Java configurado:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

En esta Mac puedes usar el Java incluido con Android Studio:

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Las pruebas de cálculos y CSV usan datos sintéticos. Para verificar el archivo privado disponible localmente:

```sh
./gradlew :app:testDebugUnitTest -PfuelCsv='/ruta/al/archivo.csv'
```

Con un emulador o dispositivo conectado: `./gradlew :app:connectedDebugAndroidTest` prueba navegación, guardado y la base de datos.

## Criterios de cálculo

Se usa el método **tanque lleno a tanque lleno**. La primera carga completa fija el odómetro inicial; los litros y el costo de esa primera carga cuentan en el historial, pero no se consideran combustible consumido en un intervalo conocido. Se acumulan las cargas posteriores, incluidas las parciales, hasta el siguiente tanque lleno.

- Rendimiento: suma de kilómetros de intervalos completos / suma de litros de esos intervalos.
- Costo/km: combustible pagado en esos intervalos / kilómetros de esos intervalos.
- Precio medio/L: combustible pagado en cargas incluidas / litros de cargas incluidas.
- Una carga excluida o una carga faltante corta el intervalo; un nuevo tanque lleno permite retomar el cálculo.
- Los totales de estadísticas excluyen registros con `showInStatistic=false`. El conteo del historial muestra todas las cargas.
- Las cargas parciales finales cuentan en el gasto, pero aún no generan un intervalo de consumo.
- Dinero en centavos, volumen en mililitros y distancia en metros para la persistencia; conversión a unidades visibles al presentar los datos.

## Notas técnicas

- Material 3 **1.5.0-beta01** permite usar `MaterialExpressiveTheme`; es una dependencia beta deliberada por el requisito Expressive. [Notas oficiales de Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3).
- [Compatibilidad de AGP 9.2](https://developer.android.com/build/releases/agp-9-2-0-release-notes).
- Paquete Android: `mx.fuelstatus`; base de datos: `fuelstatus.db`, esquema 3, con migraciones que conservan el historial de versiones 1 y 2. El esquema de Room se conserva en `app/schemas` para futuras migraciones.
- Se declaran Internet y estado de red para la copia opcional en Drive. Android backup automático está deshabilitado; Room y el JSON privado se conservan en el dispositivo.
- El CSV incluye **llenados**; el respaldo JSON de Drive incluye también vehículos, gastos y apariencia. Desinstalar elimina los datos y copias locales, pero conserva las copias ya confirmadas en Drive.

El alcance y las decisiones pendientes se detallan en [docs/DEFINICION.md](docs/DEFINICION.md). El contrato del archivo está en [docs/IMPORTACION.md](docs/IMPORTACION.md).
