# Evidencia de fase 6: calendario, tareas y actividad

Validación local del 26 de septiembre de 2026. Datos ficticios; SQL Server real. SMTP deshabilitado en TourismLocal. No se publicó ni se enviaron mensajes externos.

## Entrega

Calendario mensual con cambio de mes/año, Hoy del servidor, leyenda y marcadores de reservas CONFIRMED y todas las tareas. Detalle por día con reservas de todos los estados y tareas paginadas. Tareas compartidas por ADMIN y STAFF, creación/edición/estado/eliminación, autor y último editor, Cancelar, confirmación de borrado, rowversion y recuperación idempotente.

Actividad de solo lectura para ADMIN con filtros por fecha de Costa Rica, actor, tipo y entidad. Agrupa cambios por solicitud, con detalle relacionado paginado. No devuelve documentos JSON internos, secretos, contactos ni descripciones privadas. Los nombres corresponden al usuario actual. V7 añade índices a ActivityEvent; conserva las 23 tablas de negocio. OpenAPI 0.6.0 actualizado y validado.

## Resultados reproducibles

- `./scripts/verify.ps1`: 32 pruebas Java, cero fallos, errores u omisiones; BUILD SUCCESS. Log local `target/phase-six-verify.log`.
- Después de ampliar las aserciones de permisos y auditoría múltiple y agregar respuesta 405 segura: `./mvnw.cmd -B -ntp "-Dtest=FoundationTest,OrganizationTest" verify`, con entorno cargado: seis pruebas aprobadas, BUILD SUCCESS. Log `target/phase-six-final.log`. No son seis pruebas adicionales al total: reejecutan dos clases de la suite.
- Panel, copia NTFS documentada: `npm.cmd test`, 16 pruebas aprobadas; `npm.cmd run build`, compilación de producción correcta (438,20 kB iniciales). Último build incluye el texto vacío del día seleccionado.
- Flyway validó siete migraciones y aplicó V7 sobre TourismLocal al arrancar la API; se comprobó creación y edición con su usuario de aplicación.

## Cobertura de aceptación

PA-16 y PA-17: triggers sobre tareas, incluido UPDATE y DELETE de múltiples filas; atribución del actor y requestId, agrupación con eventos, filtros y paginación, límites diarios de Costa Rica, exclusión de campos privados. La suite previa conserva comprobación de limpieza del contexto de conexiones. STAFF recibe 403 en actividad y opciones; ADMIN recibe 200 en lecturas y 405 al intentar POST de actividad. CSRF ausente devuelve 403.

PA-21: paso diciembre/enero, febrero bisiesto, marcadores CONFIRMED frente a CANCELLED/CHECKED_IN, tareas completadas visibles, traslado entre fechas, conservación del autor, último editor, rechazo de versión antigua, eliminación auditada y repetición idempotente. Pruebas del panel cubren Cancelar, rechazo de confirmación de borrado, conflicto y reintento con cuerpo/clave idénticos.

## Recorrido en navegador

En localhost:4201 se inició sesión como admin.local, abrió calendario y comprobó las dos reservas ficticias del día en estados Cancelada e Ingresada, sin contarlas como confirmadas en el marcador. Se creó la tarea ficticia #3, «Verificación local fase 6», el 26 de septiembre y se cambió a Completada. Se conservó el marcador de una tarea y el autor Demo Administrator.

Actividad filtrada por Tarea mostró creación y modificación separadas, con hora local y actor correcto; al abrir la modificación mostró «Tarea #3», resultado Modificado y origen Aplicación, sin exponer título ni descripción. No se eliminó esta tarea desde el navegador: el borrado se verificó en SQL de pruebas con rollback y en pruebas del panel.

Se comprobaron etiquetas en español, inglés y portugués. En anchos solicitados de 360, 768 y 1440 píxeles, clientWidth y scrollWidth coincidieron (345, 753 y 1425 por la barra vertical). La vista móvil usa selector de navegación; calendario conserva siete columnas. Captura: `phase-six-calendar.png`. Se restableció el tamaño normal del navegador.

## Límites y siguiente fase

La fase 7 debe completar la matriz integral de aceptación, navegadores Chrome/Edge/Firefox/Safari, operación, respaldo/restauración y configuración de despliegue. Esta evidencia no certifica producción. No se verificó entrega de correo externo ni se habilitó SMTP. Las pruebas de fechas extremas y roles STAFF se realizaron automáticamente; el recorrido manual usó ADMIN. Las claves de operaciones inciertas se conservan en memoria de la pestaña: resolver el reintento antes de recargar o cerrar. El proyecto no se declara finalizado.
