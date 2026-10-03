-- Run as database owner/server administrator ONLY if Lenguajes lacks access.
-- The supplied shared login is used for this demo. No server role is granted.
USE TourismDemo;
GO
IF SUSER_ID(N'Lenguajes') IS NULL
    THROW 51000, 'The supplied server login does not exist.', 1;
IF SUSER_SNAME((SELECT owner_sid FROM sys.databases WHERE name = DB_NAME())) <> N'Lenguajes'
   AND NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE sid = SUSER_SID(N'Lenguajes'))
    EXEC(N'CREATE USER [Lenguajes] FOR LOGIN [Lenguajes]');
IF USER_NAME() <> N'dbo' OR SUSER_SNAME((SELECT owner_sid FROM sys.databases WHERE name = DB_NAME())) <> N'Lenguajes'
BEGIN
    IF DATABASE_PRINCIPAL_ID(N'Lenguajes') IS NOT NULL
        ALTER ROLE db_owner ADD MEMBER [Lenguajes];
END;
GO
