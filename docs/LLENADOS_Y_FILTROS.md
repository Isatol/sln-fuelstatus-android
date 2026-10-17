# Iconos y filtro de llenados · 0.7.0

El formulario de Nuevo llenado y Editar llenado incorpora iconos en Combustible, Fecha, Odómetro, Litros, Total pagado, Tanque lleno, Incluir en estadísticas, Falta una carga anterior, Gasolinera y Notas. Los iconos usan los colores del tema y los textos conservan sus etiquetas para accesibilidad. Litros y Total se muestran uno debajo del otro en pantallas estrechas o con texto ampliado.

En **Llenados → TODOS LOS LLENADOS** aparece un selector con icono de filtro:

- **3 meses**, seleccionado inicialmente.
- **6 meses**.
- **1 año**.
- **Personalizado**, con fecha de inicio y fin.

Los periodos relativos cuentan meses de calendario hacia atrás desde hoy y terminan al finalizar el día actual, según la zona horaria del dispositivo. El filtro muestra sus fechas exactas y cuántos llenados se ven respecto al total del vehículo. Las fechas personalizadas incluyen ambos días completos y admiten un solo día. El calendario permite elegir el rango o escribir las fechas; Aplicar solo está disponible con fechas válidas. Cancelar conserva el filtro anterior.

El filtro se aplica únicamente a las tarjetas del historial. Rendimiento, combustible total, conteo general y rendimiento de cada tarjeta siguen calculándose con el historial completo del vehículo; un intervalo de tanque lleno puede comenzar antes de las fechas visibles. No se eliminan ni modifican registros. Si el periodo no tiene llenados, se muestra un mensaje para elegir otro rango.

La selección se conserva al cambiar de pestaña y al recrearse la Activity. Una sesión nueva comienza en tres meses; Borrar todos los datos también restablece ese periodo. Los periodos relativos se actualizan al volver a la pantalla y al cambiar el día.

No cambia Room (versión 6), ni el formato de copias (5), ni la importación o exportación. Las copias conservan todo el historial, independientemente del filtro visible.

APK debug: `.local/releases/FuelStatus-0.7.0-iconos-filtro-llenados.apk`, versionCode **7**, con la misma firma debug de las entregas anteriores.
