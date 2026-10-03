# Conexión remota verificada — 2026-09-28

Destino: servidor privado (dirección omitida), TourismDemo, SQL Server 2019 (15.0.2190.7).

## Resultados

- JDBC conecta con las credenciales privadas de .env.remote.
- Spring Boot arranca con perfil remote y Hibernate valida el esquema.
- Flyway valida las siete migraciones instaladas desde SSMS; versión 7, ninguna migración pendiente. No se modificaron sus checksums ni se usó repair/baseline.
- La consulta directa encontró configuración, dos usuarios activos, tres reservas y dos tareas. Los hashes BCrypt coinciden con las contraseñas de demostración.
- API en 127.0.0.1:8080: health UP, negocio público y disponibilidad mensual correctos.
- ADMIN y STAFF: login con CSRF, sesión persistente, dashboard, calendario y logout correctos.
- Configuración: ADMIN obtiene 200 y STAFF obtiene 403.
- Cotización pública: dos adultos, un niño, un adulto mayor y un vehículo = 17000.00 CRC. No se confirmó una reserva.
- Maven clean verify: BUILD SUCCESS; dos pruebas unitarias aprobadas y 30 pruebas SQL omitidas intencionalmente. Las pruebas de integración solo deben ejecutarse en una TourismIntegrationTest separada, nunca contra la demo.

Las pruebas de login/logout dejan eventos normales de actividad. No se recargaron semillas, no se crearon reservas ni movimientos financieros y no se enviaron correos. El proceso iniciado para la comprobación usa el modo -LocalHttp, solo en loopback. Esto no constituye un despliegue público ni una validación completa por navegador.

## Seguridad y limpieza

DB_URL utiliza encrypt=true y trustServerCertificate=true, excepción limitada a esta conexión de demostración. El transporte está cifrado pero no se valida la identidad del servidor. Para producción se requiere certificado confiable, trustServerCertificate=false, login SQL exclusivo y contraseñas únicas para los usuarios de aplicación.

Se retiraron .env y .env.example locales, application-local.yaml, provision-local.ps1, database.ps1, LocalSeed.java, DatabaseMigration.java, grant-application.sql y verify-local-recovery.sql. La antigua semilla de prueba se transformó en un fixture exclusivo de src/test/resources con guardas de base y transacción. No se borraron bases de SQL Server, respaldos ni datos. Los informes de fases anteriores se conservan como historia, no como instrucciones de conexión actuales.

## Arranque

Desde business-api: `./scripts/start.ps1 -LocalHttp`. Los frontends conservan su proxy /api hacia 127.0.0.1:8080; se inician con sus scripts habituales. No hace falta ejecutar más SQL para esta instalación.
