# Instalación desde SQL Server Management Studio

Todos los archivos de esta carpeta son T-SQL para SSMS normal: no requieren SQLCMD, PowerShell ni Java para instalar la base.

Conectar a `sql-server.example.invalid` con autenticación SQL y el acceso facilitado. Abrir y ejecutar **cada archivo completo, por separado**, en este orden. Si hay un error, detenerse y no ejecutar el siguiente.

1. `01-crear-base.sql`: crea TourismDemo. Si ya existe se detiene; no la borra. Solo omitir este paso si el administrador confirma que esa base está vacía y asignada al proyecto.
2. `02-esquema.sql`: crea las tablas, índices, restricciones y triggers de V1–V7. Requiere una base vacía; todo el esquema y su historial se instalan en una sola transacción.
3. `04-verificar.sql`: consulta resultados sin modificar nada. Esperados: siete migraciones y 23 tablas de negocio; los conteos de datos dependen del uso de la aplicación.

`05-permisos-opcional.sql` es **solo para el administrador del servidor**, si debe crear la base por ti o conceder acceso antes del paso 2. Concede db_owner únicamente en TourismDemo al login indicado, apropiado solo para este acceso de demostración. No ejecutar contra otras bases.

## Datos y usuarios

Las semillas de demostración fueron retiradas. Para una base ya configurada, conserva los datos y usuarios existentes y no repitas esta instalación. Para dar de alta usuarios nuevos, utiliza scripts/provision-user.ps1 desde una terminal interactiva.

## Compatibilidad con la API

`02-esquema.sql` es una instantánea exacta de los siete scripts de migración actuales, ejecutados en lotes mediante sp_executesql para conservar las reglas de CREATE TRIGGER y revertir toda la instalación ante errores. Registra nombres, versiones y checksums CRC32 en flyway_schema_history **en la misma transacción**. No utiliza baseline ni repair. Si cambian las migraciones del repositorio, se debe regenerar y revisar esta instantánea antes de instalar una base nueva; nunca modificar el historial para ocultar diferencias.

La API ya tiene `.env.remote` privado apuntando a TourismDemo. Tras instalar con SSMS, no ejecutar los comandos create/migrate anteriores: basta arrancar la API con el perfil remote y sus variables. Flyway valida el historial al arrancar y aplica futuras migraciones. Los frontends hablan con la API; no llevan las credenciales SQL.

La conexión de la API se configuró con cifrado y `trustServerCertificate=true` para esta demostración. Esto acepta el certificado sin verificar su identidad; no es una configuración de producción. Para producción, solicitar certificado confiable y nombre DNS correcto, y volver a `trustServerCertificate=false`.

La dirección sql-server.example.invalid es un marcador: sustituirla por el servidor real en la configuración privada.
