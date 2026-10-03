-- Execute in SSMS against master. Creates ONLY TourismDemo; never overwrites it.
USE master;
GO
IF DB_ID(N'TourismDemo') IS NOT NULL
    THROW 51000, 'TourismDemo already exists. Verify ownership before using it; nothing was changed.', 1;
IF COALESCE(HAS_PERMS_BY_NAME(NULL, NULL, 'CREATE ANY DATABASE'), 0) = 0
   AND IS_SRVROLEMEMBER('sysadmin') <> 1 AND IS_SRVROLEMEMBER('dbcreator') <> 1
    THROW 51000, 'Ask the server administrator to create TourismDemo and grant database-scoped access.', 1;
EXEC(N'CREATE DATABASE [TourismDemo]');
GO
