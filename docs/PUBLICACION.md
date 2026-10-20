# Publicación de FuelStatus 1.0.0

Paquete `mx.fuelstatus`, `versionName` 1.0.0 y `versionCode` 9. Room sigue en esquema 6 y las copias completas privadas en formato 5.

El precio solicitado es **$10 MXN por descarga en México**. Debe configurarse en Play Console como aplicación de pago desde el inicio, revisando el importe admitido, los países y el perfil de pagos. El precio no se programa en el APK ni se ha configurado en la consola desde este proyecto. [Precios en Google Play](https://support.google.com/googleplay/android-developer/answer/6334373).

Esta versión funciona localmente y no utiliza OAuth ni sincronización remota. Los clientes de Google Cloud que se hubieran creado no se modifican desde la aplicación.

Para publicar queda preparar una clave de subida protegida, firmar un AAB release y configurar Play App Signing. Un AAB sin firma generado por el repositorio es un artefacto técnico, pendiente de firma; no está listo para subir. Las instalaciones debug actuales usan otra firma: planificar la conservación del historial antes de retirar una instalación personal, sin desinstalarla durante esta preparación. El CSV no conserva gastos ni todo el catálogo. [Firma de aplicaciones](https://developer.android.com/studio/publish/app-signing).

Completar ficha, capturas, contacto de soporte, clasificación y Seguridad de los datos. La [política de privacidad](privacidad.html) identifica a Isaías Orozco Toledo y el contacto público isaiasot93@gmail.com, y describe el funcionamiento de la versión 1.0.0. Es un HTML autónomo, sin JavaScript ni recursos externos, con estilos para móvil y modo claro/oscuro.

Play requiere una URL pública de la política y acceso a ella dentro de la app. El archivo está preparado, pero subirlo al repositorio no confirma que exista una página publicada. Después de la integración, se puede habilitar GitHub Pages desde `main` y la carpeta `/docs`, verificar el acceso público a `privacidad.html` y usar su URL real en Play Console. El enlace o texto dentro de la aplicación sigue pendiente; esta rama solo modifica documentación. [Datos de usuario](https://support.google.com/googleplay/android-developer/answer/10144311).

Comprobar los requisitos de prueba y revisión de la cuenta: para cuentas personales creadas después del 13 de noviembre de 2023, Google exige una prueba cerrada con al menos 12 participantes inscritos continuamente durante 14 días antes de solicitar acceso a producción. [Requisitos de pruebas](https://support.google.com/googleplay/android-developer/answer/14151465).

La integración en `main` y la publicación en Play corresponden al usuario. Esta preparación no publica la app ni cambia la configuración de ninguna consola.
