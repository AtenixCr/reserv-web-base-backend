USE TourismDemo;
GO
SET NOCOUNT ON;
SELECT DB_NAME() AS database_name, SERVERPROPERTY('ProductVersion') AS server_version;
SELECT version, description, success FROM dbo.flyway_schema_history ORDER BY installed_rank;
SELECT COUNT(*) AS tables_count FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND name <> 'flyway_schema_history';
SELECT username, role, active FROM dbo.AppUser;
SELECT enabled, defaultCapacity FROM dbo.BusinessSettings;
SELECT COUNT(*) AS reservations_count FROM dbo.Reservation;
SELECT COUNT(*) AS tasks_count FROM dbo.Task;
SELECT COUNT(*) AS email_jobs FROM dbo.EmailOutbox;
GO
