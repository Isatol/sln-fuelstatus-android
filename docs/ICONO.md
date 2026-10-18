# Icono de FuelStatus

El icono del launcher conserva el surtidor original en verde oscuro `#294B2B`. El cuadrado verde claro se sustituye por un aro sin relleno `#D4EEB1`, sobre fondo blanco y con margen hasta el contorno que aplica Android. Corresponde a la propuesta con el aro ampliado aprobada el 18 de octubre de 2026.

## Recursos

- `drawable/ic_fuel.xml`: primer plano vectorial sobre un lienzo de 108 × 108 dp. El surtidor mantiene sus trazos originales; su ventana es un hueco transparente mediante `evenOdd`.
- `mipmap-anydpi/ic_launcher.xml`: icono adaptable con fondo blanco, primer plano y capa `monochrome`. Android 12 y 12L usan las dos primeras capas; Android 13 y posteriores también pueden utilizar la silueta del aro y el surtidor, incluida la ventana transparente, para los iconos temáticos. El sistema determina sus colores cuando se activa esa opción.
- El manifiesto utiliza `@mipmap/ic_launcher` tanto en `android:icon` como en `android:roundIcon`.

El círculo exterior pertenece a la máscara del launcher. El recurso de primer plano no pinta esa máscara ni el fondo blanco; el icono adaptable proporciona una capa de fondo blanca independiente. Otros launchers pueden aplicar un contorno exterior diferente, conservando el aro circular interior.

## Proporciones aprobadas

En el viewport nominal central de 72 dp, el diámetro exterior del aro es 57.6 dp (80 %), su trazo mide aproximadamente 3.064 dp y queda un margen blanco de 7.2 dp por lado bajo una máscara circular. El aro completo cabe en el círculo seguro de 66 dp. El surtidor se transforma con escala `0.84375` y desplazamiento `(33.75, 33.75)` sobre el lienzo de 108 dp, conservando su tamaño relativo en la propuesta.

No se incluyen imágenes rasterizadas ni se añaden dependencias. La elección de colores dentro de Ajustes → Tema es independiente del icono del launcher.

Referencia: [Iconos adaptables de Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
