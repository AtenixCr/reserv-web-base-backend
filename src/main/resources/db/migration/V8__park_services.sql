CREATE TABLE dbo.ParkService (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    settingsId bigint NOT NULL
        REFERENCES dbo.BusinessSettings(id),
    imageUrl nvarchar(2048) NULL,
    iconUrl nvarchar(2048) NULL,
    sortOrder int NOT NULL DEFAULT 0
        CHECK (sortOrder >= 0),
    enabled bit NOT NULL DEFAULT 0,
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.ParkServiceTranslation (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    serviceId bigint NOT NULL
        REFERENCES dbo.ParkService(id),
    locale varchar(2) NOT NULL
        CHECK (locale IN ('es', 'en', 'pt')),
    name nvarchar(120) NOT NULL,
    description nvarchar(4000) NOT NULL,
    imageAlt nvarchar(250) NOT NULL,
    CONSTRAINT UQ_ParkServiceTranslation
        UNIQUE (serviceId, locale)
);
GO

CREATE INDEX IX_ParkService_Public
ON dbo.ParkService(settingsId, enabled, sortOrder, id);
GO

-- Auditoría usando el mecanismo existente del proyecto.
DECLARE @tableName sysname;
DECLARE @sql nvarchar(max);

DECLARE service_tables CURSOR LOCAL FAST_FORWARD FOR
    SELECT name
    FROM sys.tables
    WHERE schema_id = SCHEMA_ID('dbo')
      AND name IN ('ParkService', 'ParkServiceTranslation');

OPEN service_tables;
FETCH NEXT FROM service_tables INTO @tableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'
    CREATE OR ALTER TRIGGER dbo.'
        + QUOTENAME('TR_' + @tableName + '_Audit') + N'
    ON dbo.' + QUOTENAME(@tableName) + N'
    AFTER INSERT, UPDATE, DELETE
    AS
    BEGIN
        SET NOCOUNT ON;

        INSERT dbo.AuditLog (
            entity, entityId, operation,
            actorId, origin, requestId,
            beforeData, afterData
        )
        SELECT
            N''' + @tableName + N''',
            COALESCE(i.id, d.id),
            CASE
                WHEN d.id IS NULL THEN ''INSERT''
                WHEN i.id IS NULL THEN ''DELETE''
                ELSE ''UPDATE''
            END,
            TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
            CASE
                WHEN SESSION_CONTEXT(N''requestId'') IS NULL
                    THEN ''EXTERNAL''
                ELSE ''APPLICATION''
            END,
            TRY_CONVERT(
                uniqueidentifier,
                SESSION_CONTEXT(N''requestId'')
            ),
            CASE WHEN d.id IS NULL THEN NULL ELSE (
                SELECT d.*
                FOR JSON PATH, INCLUDE_NULL_VALUES,
                WITHOUT_ARRAY_WRAPPER
            ) END,
            CASE WHEN i.id IS NULL THEN NULL ELSE (
                SELECT i.*
                FOR JSON PATH, INCLUDE_NULL_VALUES,
                WITHOUT_ARRAY_WRAPPER
            ) END
        FROM inserted i
        FULL OUTER JOIN deleted d ON i.id = d.id;
    END;';

    EXEC sys.sp_executesql @sql;
    FETCH NEXT FROM service_tables INTO @tableName;
END;

CLOSE service_tables;
DEALLOCATE service_tables;
GO

-- Permisos para el usuario restringido utilizado por el proyecto.
IF DATABASE_PRINCIPAL_ID(N'tourism_app_local') IS NOT NULL
BEGIN
    GRANT SELECT, INSERT, UPDATE
        ON dbo.ParkService TO tourism_app_local;

    GRANT SELECT, INSERT, UPDATE, DELETE
        ON dbo.ParkServiceTranslation TO tourism_app_local;
END;
GO