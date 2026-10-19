# Acuerdos recuperados y alcance

Fuente: chat «Control de gasolina apps» y esta conversación. Fecha de reconstrucción: 16 de octubre de 2026.

## Objetivo

Conservar de Fuel la sencillez, jerarquía de información y rapidez al registrar gasolina, con diseño propio de Android y Material 3 Expressive / Material You. La aplicación debe instalarse en el Pixel 10 y su código fuente debe poder mantenerse y compilarse localmente.

## Acuerdos explícitos

1. Kotlin + Jetpack Compose y Material 3 Expressive / Material You.
2. Cinco destinos: Llenados, Gastos, Estadísticas, Vehículos y Ajustes.
3. Color dinámico del sistema y alternativa con paleta fija; temas claro/oscuro.
4. Room como base de datos local.
5. Crear, editar y eliminar llenados.
6. Calcular km/L, $/L, $/km, promedios y totales.
7. Instalación nueva sin vehículos, llenados ni gastos, con catálogos y ajustes predeterminados, igual que después de borrar todos los datos.
8. Importar y exportar llenados de FuelStatus desde Ajustes. Usar datos sintéticos para validar el inicio vacío.
9. Uso sin cuenta, sin anuncios y sin suscripción.
10. Entregar fuentes y APK, con posteriores iteraciones a partir de pruebas en el Pixel.
11. Dejar Drive, ubicación/mapas e informes/gráficas avanzados para versiones posteriores.

La v0.1 del chat anterior era una base con navegación y datos de demostración; no llegó a compilarse allí. Esta recreación parte de cero y añade persistencia y formularios reales.

## Decisiones de implementación para esta recreación

Estas decisiones concretan el alcance; no se presentan como acuerdos textuales anteriores:

- Nombre visible **FuelStatus**, siguiendo la carpeta actual. FuelDroid se conserva como referencia histórica.
- Moneda MXN, litros y kilómetros, Android 12 o posterior.
- Exportación CSV adicional para los llenados, compatible con el importador de la propia app.
- Gastos con categorías, fecha, importe y notas; creación, edición y eliminación.
- Alta, edición y selección de varios vehículos, sin eliminación de vehículos en esta versión para evitar borrar todo un historial por accidente.
- Importación explícita mediante selector de archivos de Android; detección de duplicados y transacción única. Se conserva el archivo personal fuera del proyecto.
- Promedios ponderados entre tanques completos, con interrupción del cálculo cuando falta una carga o se excluye.
- Visualización sencilla de los últimos intervalos; los informes avanzados quedan pendientes.

## Pendiente para comprobar con el usuario

- Comparación de totales y criterios exactos frente a la pantalla de Fuel; el CSV por sí solo no contiene sus valores calculados.
- Uso real y revisión visual en el Pixel 10: ergonomía de captura, animaciones, legibilidad y colores del fondo.
- Nombre definitivo, ícono definitivo y distribución de versiones de lanzamiento.
- Respaldo completo de gastos y vehículos, Drive y mapas; más adelante pueden necesitar decisiones de permisos y privacidad.
- Compilación en GitHub Actions; el flujo de trabajo actual es local en Android Studio. El repositorio remoto es `Isatol/sln-fuelstatus-android`.


## Ampliación aprobada: copias en Google Drive (0.2)

Siempre guardar el historial en Room y una copia JSON privada, aun sin conexión. Autorizar Google Drive desde Ajustes, crear una carpeta visible FuelStatus, subir copias completas y mostrar cuenta, carpeta, última copia local, última copia confirmada en Drive, intento y detalle de errores. Reintentar al abrir la aplicación con conexión, añadir trabajo persistente y botón manual. Conservar cinco copias en Drive. Restaurar únicamente después de una vista previa y confirmación, guardando previamente una copia de recuperación local. Detectar otro historial antes de subir y pedir elegir cómo continuar. No combinar automáticamente datos de varios teléfonos. La configuración y los límites están en [GOOGLE_DRIVE.md](GOOGLE_DRIVE.md).
