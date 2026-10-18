# Tema y colores

En Ajustes → Tema se elige por separado el modo **Sistema, Claro u Oscuro** y la paleta de colores de la aplicación. Las muestras circulares muestran el color principal arriba y los contenedores secundario y terciario abajo, según el modo de apariencia actual. El borde y el estado accesible de selección identifican la opción activa.

- **Android:** utiliza la paleta activa del sistema mediante `dynamicLightColorScheme` y `dynamicDarkColorScheme`. Sigue la elección de colores de Android, que puede proceder del fondo de pantalla o de un color elegido en los ajustes del dispositivo.
- **FuelStatus, Azul, Violeta, Rosa, Naranja y Gris:** paletas propias con versiones claras y oscuras. Tocarlas desactiva el color dinámico en una sola actualización. Android vuelve a activarlo. La selección se aplica inmediatamente y se conserva al reiniciar.

La fila de muestras se desplaza horizontalmente. Los controles Sistema/Claro/Oscuro pueden distribuirse en varias filas cuando falta espacio. Los controles de apariencia se deshabilitan mientras hay una operación en curso.

## Color del aro del icono

Debajo de Tema hay una tarjeta independiente para el aro del icono a color del launcher. Android sigue la paleta del sistema; Verde FuelStatus conserva el aro original y las otras cinco opciones reutilizan la paleta de la aplicación. No cambia el tema interno ni la capa monocromática de los iconos temáticos. Android es el valor predeterminado cuando aún no existe una elección guardada. Los detalles y límites de refresco y accesos del escritorio están en [Icono de FuelStatus](ICONO.md).

## Alcance de Material You

Se lee la paleta que Android expone a las aplicaciones. No se lee la imagen del fondo ni se solicitan permisos para acceder a ella. Las API públicas no proporcionan la lista completa de propuestas del selector de fondos del sistema; las otras seis opciones son paletas de FuelStatus. Elegirlas solo cambia esta aplicación.

Las paletas fijas se generan con Material Color Utilities 0.3.0, estilo Tonal Spot y contraste estándar; Gris usa Monochrome. `tools/generate-theme-palettes.mjs` documenta cómo reproducir los valores sin añadir una dependencia en ejecución. La paleta verde conserva los colores explícitos anteriores de FuelStatus y completa sus demás roles con los valores generados.

## Persistencia y respaldos

`Preferences.colorPalette` guarda el código estable de la última paleta fija; `dynamicColor` mantiene su significado anterior. Las preferencias existentes conservan el modo y la elección de color dinámico y usan verde como paleta fija predeterminada. Volver a Android conserva la última paleta fija en las preferencias.

Las copias completas de formato 5 incluyen `appearance.colorPalette` como campo adicional opcional. Las copias anteriores sin ese campo se normalizan a `green`, conservando su valor de `dynamicColor`. Los códigos de paleta inválidos se rechazan durante la validación. Las versiones anteriores de FuelStatus ignoran este campo adicional y, cuando el color dinámico está desactivado, muestran su verde original. No cambian Room 6, los datos de vehículos/llenados/gastos ni el CSV.

Referencias: [Material 3 en Compose](https://developer.android.com/develop/ui/compose/designsystems/material3#color-scheme), [paletas dinámicas de Android](https://source.android.com/docs/core/display/dynamic-color) y [Material Color Utilities](https://github.com/material-foundation/material-color-utilities).
