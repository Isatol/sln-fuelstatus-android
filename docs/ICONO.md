# Icono de FuelStatus

El icono del launcher conserva el surtidor original en verde oscuro `#294B2B`. El cuadrado verde claro se sustituye por un aro sin relleno `#D4EEB1`, sobre fondo blanco y con margen hasta el contorno que aplica Android. Corresponde a la propuesta con el aro ampliado aprobada el 18 de octubre de 2026.

## Recursos

- `drawable/ic_fuel.xml`: primer plano vectorial sobre un lienzo de 108 × 108 dp. El surtidor mantiene sus trazos originales; su ventana es un hueco transparente mediante `evenOdd`.
- `mipmap-anydpi/ic_launcher.xml`: icono adaptable con fondo blanco, primer plano y capa `monochrome`. Android 12 y 12L usan las dos primeras capas; Android 13 y posteriores también pueden utilizar la silueta del aro y el surtidor, incluida la ventana transparente, para los iconos temáticos. El sistema determina sus colores cuando se activa esa opción.
- El manifiesto utiliza `@mipmap/ic_launcher_android` como icono de aplicación y redondo. Siete `activity-alias` apuntan a `MainActivity`; solo una está habilitada. Android es la opción habilitada por defecto, sin esperar a la primera apertura.

El círculo exterior pertenece a la máscara del launcher. El recurso de primer plano no pinta esa máscara ni el fondo blanco; el icono adaptable proporciona una capa de fondo blanca independiente. Otros launchers pueden aplicar un contorno exterior diferente, conservando el aro circular interior.

## Proporciones aprobadas

En el viewport nominal central de 72 dp, el diámetro exterior del aro es 57.6 dp (80 %), su trazo mide aproximadamente 3.064 dp y queda un margen blanco de 7.2 dp por lado bajo una máscara circular. El aro completo cabe en el círculo seguro de 66 dp. El surtidor se transforma con escala `0.84375` y desplazamiento `(33.75, 33.75)` sobre el lienzo de 108 dp, conservando su tamaño relativo en la propuesta.

No se incluyen imágenes rasterizadas ni se añaden dependencias. La elección de colores dentro de Ajustes → Tema es independiente del icono del launcher.

## Color del aro del icono

Ajustes → Apariencia incluye una tarjeta debajo de Tema. Ofrece **Android**, **Verde FuelStatus**, **Azul**, **Violeta**, **Rosa**, **Naranja** y **Gris**. Cambia exclusivamente el trazo del aro en el icono a color; el surtidor, su ventana, el fondo blanco y las proporciones permanecen iguales.

Android resuelve `@android:color/system_accent1_100`, un tono claro de la paleta activa del sistema disponible desde Android 12. No depende del tema de FuelStatus ni requiere leer el fondo de pantalla. Verde usa el recurso original exacto `#D4EEB1`; los demás toman `primaryContainer` de las paletas claras de Colores de la app. `tools/generate-launcher-ring-icons.py` regenera los seis pares de recursos a partir del vector original y las paletas existentes.

**Iconos temáticos:** todos los iconos adaptables usan exactamente `@drawable/ic_fuel` en `monochrome`. Se conserva así la silueta dinámica ya aprobada; Android y el launcher deciden dónde aplicarla y con qué colores. La preferencia de aro se aprecia en las superficies que muestran el icono a color, como el cajón del launcher del usuario; la app no puede imponer reglas distintas por escritorio y cajón a todos los launchers.

**Cambio de componente:** Android 13+ cambia las aliases en una operación atómica; Android 12/12L habilita primero la nueva y después deshabilita la anterior, con recuperación ante errores. `MainActivity` permanece habilitada para intentos explícitos existentes. El launcher puede tardar en refrescar el icono y algunos launchers pueden retirar un acceso del escritorio al deshabilitar su alias; en ese caso hay que agregarlo de nuevo. Conservar la capa monocromática no garantiza conservar la posición de un acceso fijado a otra alias.

## Persistencia y compatibilidad

`Preferences.iconRingColor` se guarda como `icon_ring` y se reconcilia con las aliases al abrir la aplicación. Android es el valor inicial para instalaciones nuevas, preferencias anteriores sin elección y borrado completo de datos. Una elección guardada se mantiene al reiniciar y no cambia al elegir un tema interno.

Si el launcher rechaza un cambio, no se guarda la nueva preferencia. Durante una restauración o un borrado local, ese fallo también revierte la transacción de Room para conservar el historial anterior.

Los respaldos completos conservan `appearance.iconRingColor` como campo opcional de formato 5. Las copias anteriores sin él utilizan Android; los códigos desconocidos se rechazan. Versiones anteriores ignoran el campo. No cambia Room 6 ni el CSV.

Referencias: [aliases de actividad](https://developer.android.com/guide/topics/manifest/activity-alias-element), [paletas dinámicas](https://source.android.com/docs/core/display/dynamic-color) e [Iconos adaptables de Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
