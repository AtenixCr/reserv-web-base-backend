# Instalación de la demostración en SQL Server remoto

**Si vas a instalar desde SSMS**, utiliza los archivos de [../ssms/README.md](../ssms/README.md): incluyen el esquema completo en T-SQL puro. Esa ruta reemplaza los comandos create/migrate de esta guía; no mezclar ambas instalaciones.

Destino: `sql-server.example.invalid:1433`, base nueva y exclusiva **TourismDemo**.
El puerto 1433 es el predeterminado y debe confirmarse con el administrador si la conexión falla.
No se toca ninguna otra base existente. No ejecutar pruebas de integración contra este servidor compartido.

## Base ya configurada

Si la base ya está instalada, utiliza scripts/start.ps1 con la configuración privada existente. No repitas la instalación. Se retiraron las semillas de demostración; los datos existentes se conservan.

## Instalación en una base nueva (PowerShell, desde business-api)

Requiere Java 21 y acceso al servidor. `.env.remote` contiene las credenciales facilitadas y está excluido de Git.
En otra computadora, copiar `.env.remote.example` a `.env.remote` y completar el servidor, los usuarios y las contraseñas de SQL.

```powershell
./scripts/remote-database.ps1 check
./scripts/remote-database.ps1 create
./scripts/remote-database.ps1 migrate
./scripts/start.ps1 -EnvFile .env.remote
```

`check` es de solo lectura: comprueba versión, acceso y permiso de crear bases. `create` ejecuta `sql/ssms/01-crear-base.sql` y se detiene si TourismDemo ya existe. Si ya la creó el administrador, omitir `create` después de comprobar que es la base dedicada a este proyecto.

`migrate` ejecuta las siete migraciones originales de `src/main/resources/db/migration/`, en orden V1–V7, creando tablas, índices, restricciones y triggers. **Estos son los scripts de esquema completos.** Flyway registra sus versiones y checksums y permite repetir `migrate` sin volver a crear las tablas. No ejecutarlos manualmente en SSMS ni usar baseline/repair para ocultar diferencias.

## Alternativa para CREATE DATABASE en SSMS

Conectar al servidor con autenticación SQL y ejecutar `sql/ssms/01-crear-base.sql` una sola vez. Si Lenguajes carece de permisos, entregar al administrador ese archivo y `sql/ssms/05-permisos-opcional.sql`. Este último concede db_owner únicamente en TourismDemo para permitir las migraciones y es exclusivo de esta demostración. Después ejecutar `migrate` con PowerShell, como arriba. No ejecutar `create` de nuevo.

`sql/ssms/04-verificar.sql` permite comprobar la instalación desde SSMS sin mostrar hashes: se esperan siete migraciones exitosas y 23 tablas de negocio; los conteos de datos dependen del uso de la aplicación.

## Conexión desde la aplicación y el alojamiento

La API carga `.env.remote` por defecto al usar `start.ps1`. El archivo `.env` de la BD local y sus scripts de aprovisionamiento fueron retirados. Para probar en este equipo por HTTP, usar `start.ps1 -LocalHttp`. Los Angular continúan usando `/api/v1`; sus credenciales de SQL nunca deben configurarse en el navegador.

En Render u otro alojamiento, configurar como secretos las variables de `.env.remote`: `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MIGRATION_USERNAME`, `MIGRATION_PASSWORD` y `NOTIFICATIONS_ENABLED`. El archivo .env no se carga automáticamente por Spring Boot en el alojamiento. El perfil `remote` escucha en `0.0.0.0` y en `${PORT:8080}`. Configurar también el proxy `/api` de ambos frontends antes de publicarlos.

Las cookies son seguras por defecto para HTTPS. Solo para probar los frontends locales por HTTP, antes de iniciar la API:

```powershell
./scripts/start.ps1 -LocalHttp
```

No configurar esa excepción en el alojamiento HTTPS. El despliegue de la API y los frontends es un paso aparte; cambiar la base de datos no publica las aplicaciones.

## Certificado, permisos y servidor compartido

La conexión de demostración usa `encrypt=true;trustServerCertificate=true`: cifra el tráfico pero no verifica la identidad del servidor. La excepción está limitada a DB_URL. Para producción, solicitar al administrador el nombre DNS correcto y una cadena de confianza válida y restablecer `trustServerCertificate=false`. Un timeout puede requerir VPN o autorizar la IP de salida del alojamiento; una base accesible desde esta computadora no garantiza acceso desde Render.

El login Lenguajes se usa para migrar y para la API porque es el acceso proporcionado. Si es compartido, no ofrece aislamiento frente a otros usuarios de ese login. Antes de publicar datos reales, solicitar al administrador un login exclusivo y otro de aplicación con permisos limitados, y renovar el secreto compartido por chat. No introducir las credenciales en repositorios, SQL públicos o Angular.

La dirección sql-server.example.invalid es un marcador: sustituirla por el servidor real en la configuración privada.
