# Publicación de FuelStatus 1.0.0

Preparación del 18 de octubre de 2026. Paquete `mx.fuelstatus`, `versionName` 1.0.0 y `versionCode` 9. La base funcional corresponde al inicio limpio integrado en `main` mediante el PR #19. Se conservan Room 6, respaldo 5 y el CSV de FuelStatus.

El usuario confirmó que dispone de Play Console y del proyecto Google Cloud en su cuenta personal. No se han inspeccionado ni modificado esas consolas. Cambiar la versión en el repositorio no publica la aplicación ni acredita que OAuth o Drive funcionen en producción.

## Google Drive y OAuth

1. Conservar el proyecto Google Cloud actual y confirmar que Google Drive API está habilitada. Mantener juntos los clientes debug y de distribución para dar continuidad al acceso a los archivos; verificar esa continuidad con una copia sintética.
2. En **Google Auth platform → Branding**, completar nombre FuelStatus, correo de soporte, contacto del desarrollador y los enlaces públicos de la aplicación, privacidad y condiciones que solicite Google. Verificar la propiedad del dominio si la consola lo requiere. Completar la verificación de marca que corresponda.
3. En **Data Access**, declarar únicamente el permiso que pide el código: `https://www.googleapis.com/auth/drive.file`. Google lo clasifica como no sensible; permite trabajar con los archivos creados o autorizados para la app. No sustituirlo por el acceso completo `drive`. Este alcance requiere el proceso básico de verificación aplicable, sin justificar por ello una auditoría de scopes restringidos.
4. En **Audience**, usar público **External** y pasar de **Testing** a **In production** cuando esté preparada la configuración. Publicar OAuth y obtener la verificación de marca son pasos distintos. En Testing se limita el acceso a los usuarios de prueba y las autorizaciones de Drive caducan a los siete días.
5. En **Clients**, añadir un cliente de tipo Android con paquete `mx.fuelstatus` y SHA-1 del **certificado de firma de aplicaciones de Play App Signing**. Obtenerlo en la sección de firma/integridad de Play Console. El certificado de subida del AAB y el certificado debug no sustituyen al de distribución de Play. Conservar el cliente debug; si se distribuyen APK release por otra vía, registrar también su firma cuando sea distinta.
6. Instalar desde una pista interna de Play y validar autorización, subida, consulta y restauración con una cuenta de prueba y datos sintéticos. Comprobar también reapertura, trabajo sin conexión y acceso a copias creadas por la versión anterior. El borrado remoto queda fuera de esta prueba inicial. No utilizar historiales personales.

La app usa `AuthorizationClient` directamente desde Android y no tiene un backend de OAuth. No necesita incorporar secretos de cliente al APK. Las pruebas de Drive simulado documentadas anteriormente no sustituyen esta validación real con la firma de Play.

Revisar las cuotas y condiciones vigentes de la API antes de abrir el servicio al público. La documentación de Drive ya describe umbrales diarios y posibles cargos por uso superior; no se debe asumir uso ilimitado gratuito. Las copias se alojan en el Drive de cada usuario.

Referencias oficiales: [alcances de Drive](https://developers.google.com/workspace/drive/api/guides/api-specific-auth), [público y estado de OAuth](https://support.google.com/cloud/answer/15549945), [configuración Android y SHA-1](https://developer.android.com/identity/authorization), [verificación de marca](https://developers.google.com/identity/verification/authentication-policy-compliance), [cuotas de Drive](https://developers.google.com/workspace/drive/api/guides/limits).

## Google Play y precio

- Modelo solicitado: descarga de pago, **$15 MXN en México**. Configurarlo en **Monetizar con Play → Productos → Precios de aplicaciones**, comprobar el precio final que acepta la consola y los países de distribución. La selección de países y precios adicionales está pendiente. El precio no se programa en el APK.
- Configurar la app como **de pago desde el principio**. Google permite pasar de pago a gratuita, pero una app ofrecida como gratuita no puede convertirse después en una app de pago con el mismo paquete.
- Completar el perfil de pagos y la información comercial requerida para cobrar. El importe mostrado al comprador no equivale necesariamente al ingreso neto; revisar las condiciones de comisiones e impuestos que muestre la cuenta.
- Preparar un Android App Bundle **release firmado** con una clave de subida protegida y activar Play App Signing. El repositorio aún no configura una clave release; un AAB generado sin ella es un artefacto técnico, pendiente de firma y no listo para subir. Mantener claves y contraseñas fuera del repositorio.
- Completar ficha, capturas, clasificación de contenido, declaraciones y **Seguridad de los datos**. Publicar una política de privacidad accesible y añadir su acceso en la aplicación. Declarar el tratamiento real del correo de la cuenta, los respaldos y los SDK; no deducir las respuestas solo de que el historial principal se guarde localmente.
- Si la cuenta personal fue creada después del 13 de noviembre de 2023, comprobar el requisito de prueba cerrada: al menos 12 participantes inscritos continuamente durante 14 días antes de solicitar acceso a producción. Tener una cuenta no acredita que ese requisito esté satisfecho.

Referencias oficiales: [precios de aplicaciones](https://support.google.com/googleplay/android-developer/answer/6334373), [firma de aplicaciones](https://developer.android.com/studio/publish/app-signing), [datos del usuario y privacidad](https://support.google.com/googleplay/android-developer/answer/10144311), [Seguridad de los datos](https://support.google.com/googleplay/android-developer/answer/10787469), [pruebas de cuentas personales](https://support.google.com/googleplay/android-developer/answer/14151465).

## Historial de la instalación debug

La instalación actual utiliza un certificado debug. Si la versión entregada por Play usa una firma distinta, Android no la acepta como actualización directa del mismo paquete. Preparar y comprobar un respaldo completo que permita recuperar también gastos, catálogos y ajustes antes de planificar el cambio de instalación. Un CSV de llenados no sustituye ese respaldo. No desinstalar ni borrar el teléfono personal durante la preparación de la distribución.

## Pendientes antes del lanzamiento

Confirmar el estado real de ambas consolas, cerrar la configuración y prueba de Drive, disponer de la firma release y del AAB firmado, publicar privacidad y soporte, completar la ficha y las declaraciones de Play, fijar el precio y superar los requisitos de pruebas y revisión que correspondan. La integración en `main` y la publicación externa siguen a cargo del usuario.
