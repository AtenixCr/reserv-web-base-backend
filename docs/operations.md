# Guía de operación, respaldo y traslado

## Estado y responsables

Instalación independiente por negocio. Esta guía no autoriza producción. Antes de abrir: completar la matriz `acceptance-matrix.md`, designar responsable de operación y de SQL, aprobar retención de contactos/auditoría, definir pérdida de datos tolerable (RPO) y tiempo de recuperación (RTO), y ensayar recuperación con el volumen real. No hay purga automática de auditoría en v1. Los respaldos contienen contactos y hashes: protegerlos como la base y restringir su acceso.

## Instalación y configuración

Conservar Java 21, SQL Server compatible (servidor actual: 2019) y versiones de Node/Angular fijadas en los repositorios. En una base vacía ejecutar migraciones con las credenciales de migración mediante `scripts/remote-database.ps1 migrate`; Flyway es el registro de referencia. No copiar a SSMS y volver a ejecutar migraciones ya registradas. No modificar migraciones publicadas ni ejecutar Flyway clean. Para revisar restauraciones, conservar los siete checksums y el historial.

Crear usuarios SQL separados: la aplicación sin permisos administrativos, la migración con permisos DDL sobre su base y el operador de respaldos con los permisos necesarios. En el servidor de demostración se utiliza el login facilitado; antes de producción, el DBA debe crear usuarios exclusivos con privilegios mínimos. Los triggers protegen movimientos y cierres; los privilegios de administrador SQL pueden alterarlos y no equivalen a evidencia inviolable.

Definir DB_URL con cifrado y certificado confiable en producción; DB_USERNAME/DB_PASSWORD y MIGRATION_USERNAME/MIGRATION_PASSWORD fuera del repositorio. La conexión remota de demostración usa cifrado con trustServerCertificate=true, sin validar la identidad del servidor. El modo -LocalHttp solo permite cookies no Secure en loopback; no usarlo en producción. Los scripts de semillas de demostración fueron retirados. El alta real se hace con `scripts/provision-user.ps1` desde terminal interactiva, sin contraseñas de muestra ni captura del secreto en logs.

Servir los dist de cada Angular mediante HTTPS y un proxy de su mismo origen para `/api/v1`. No usar ng serve para producción. El perfil remote escucha en 0.0.0.0:${PORT:8080}; start.ps1 -LocalHttp lo restringe a 127.0.0.1:8080; mantenerla detrás del proxy del mismo servidor o revisar explícitamente la red de destino. Conservar cookies HttpOnly, Secure y SameSite, y protección CSRF. Si un proxy cambia la IP visible, revisar límites por IP antes de habilitar público; los límites actuales viven en memoria y no cubren varias réplicas. Esta entrega no incluye proxy/dominio productivo ya configurado.

PUBLIC_URL apunta al sitio público. BUSINESS_TIME_ZONE debe ser America/Costa_Rica. Configurar negocio, horarios, cupos, contenido es/en/pt, tarifas CRC y SINPE reales con ADMIN. Las tasas USD/BRL son informativas. Mantener reservas deshabilitadas hasta validar la configuración y el recorrido completo.

## Correos y comprobación diaria

Mantener NOTIFICATIONS_ENABLED=false hasta configurar SMTP_HOST, SMTP_PORT, SMTP_FROM, autenticación y STARTTLS. Guardar SMTP_USERNAME/SMTP_PASSWORD como secretos. Ensayar primero con receptor local y direcciones ficticias. SENT significa aceptado por SMTP; no prueba entrega. Reintentos automáticos: 1, 5, 15 y 60 minutos; luego revisar FAILED. Un trabajo PROCESSING de más de diez minutos puede recuperarse; no duplicar manualmente filas de cola.

Al comenzar: revisar salud de API, fecha de Costa Rica, caja anterior abierta y estado del respaldo más reciente. Cerrar una jornada anterior por Finanzas antes de abrir hoy; no modificar CashSession por SQL. Al cerrar: conciliar métodos, registrar contado y diferencia con motivo, descargar PDF/XLSX y revisar la cola. Conservar la instantánea y corregir errores mediante compensaciones vinculadas.

Ante un error de red después de confirmar, usar la recuperación con el mismo usuario, cuerpo y clave. No recargar mientras exista una operación incierta. Si se perdió la pestaña, buscar el código/registro antes de repetir. Registrar traceId, fecha y operación para diagnóstico; nunca contraseñas, cookies o el contenido íntegro de contactos. Investigar errores 500, fallos de reportes y crecimiento de auditoría/cola/disco.

## Respaldo de la base remota

Los scripts exclusivos de recuperación local fueron retirados. Solicitar al administrador del SQL Server una política de respaldos, copia fuera del servidor y una restauración de prueba en una base separada. No restaurar ni ejecutar pruebas destructivas contra TourismDemo. Los informes de fases anteriores son evidencia histórica y no certifican recuperación del servidor remoto.

## Traslado a otro SQL Server

1. Inventariar versión/edición compatible, historial Flyway, usuarios/permisos, tamaño, conteos, triggers, cierres e identificadores de cola. Preparar destino, espacio y certificados. Guardar configuración y secretos por canal privado separado del respaldo.
2. Poner ambos frontend en mantenimiento, detener API y worker; comprobar ausencia de escrituras. No dejar dos bases activas. Registrar hora de corte y respaldo final con CHECKSUM. Conservar el origen intacto.
3. Transferir respaldo protegido. Restaurar con MOVE hacia archivos nuevos y nombre verificado, sin REPLACE sobre bases ajenas. En destinos distintos mapear usuarios a sus logins y aplicar mínimos permisos; el respaldo no recrea por sí mismo los logins del servidor.
4. Ejecutar DBCC CHECKDB, validar historial Flyway y triggers, comparar conteos e instantáneas, revisar roles y consultar reservas/reportes. Arrancar API con SMTP deshabilitado y acceso restringido; verificar cookies/CSRF/HTTPS y recorrido funcional antes de aceptar nuevas escrituras públicas.
5. Cambiar la conexión/proxy de forma controlada; habilitar solo el destino. Activar correo después de revisar estados e intentos: un envío ambiguo puede repetirse. Verificar respaldo del destino y monitorización.
6. Si falla antes de admitir nuevas escrituras, detener destino y volver a conexión del origen. Si ya recibió operaciones, no volver ciegamente al origen: detener ambas entradas y reconciliar datos con el DBA. Nunca fusionar movimientos financieros por copiado informal ni reabrir cierres.

## Pendientes antes de producción

Completar navegadores y PA-19 integral, alta técnica interactiva, restauración en un segundo servidor, configuración real de TLS/proxy/SMTP, política de datos y RPO/RTO. El ensayo local no demuestra rendimiento, recuperación ante pérdida del equipo ni recuperación a un punto en el tiempo.
