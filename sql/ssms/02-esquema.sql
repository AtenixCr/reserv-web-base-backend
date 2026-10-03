-- Ejecutar completo en SSMS normal (no requiere modo SQLCMD).
-- Instalacion NUEVA exclusivamente. Las siete migraciones y su historial son atomicos.
USE [TourismDemo];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'TourismDemo' THROW 51000, 'Seleccione TourismDemo.', 1;
IF @@TRANCOUNT <> 0 THROW 51000, 'Cierre cualquier transaccion anterior.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
        THROW 51000, 'La base no esta vacia. No se modifico el esquema.', 1;
    CREATE TABLE dbo.flyway_schema_history (
        installed_rank int NOT NULL CONSTRAINT flyway_schema_history_pk PRIMARY KEY,
        version nvarchar(50) NULL,
        description nvarchar(200) NOT NULL,
        type nvarchar(20) NOT NULL,
        script nvarchar(1000) NOT NULL,
        checksum int NULL,
        installed_by nvarchar(100) NOT NULL,
        installed_on datetime NOT NULL DEFAULT GETDATE(),
        execution_time int NOT NULL,
        success bit NOT NULL
    );
    CREATE INDEX flyway_schema_history_s_idx ON dbo.flyway_schema_history(success);

    -- V1__initial_schema.sql
    EXEC sys.sp_executesql N'CREATE TABLE dbo.BusinessSettings (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    singleton tinyint NOT NULL DEFAULT 1 UNIQUE CHECK (singleton = 1),
    defaultCapacity int NOT NULL CHECK (defaultCapacity >= 0),
    phone nvarchar(40) NOT NULL,
    email nvarchar(254) NOT NULL,
    address nvarchar(500) NOT NULL,
    sinpeNumber nvarchar(40) NOT NULL,
    enabled bit NOT NULL DEFAULT 0,
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.BusinessTranslation (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    settingsId bigint NOT NULL REFERENCES dbo.BusinessSettings(id),
    locale varchar(2) NOT NULL DEFAULT ''es'' CHECK (locale IN (''es'',''en'',''pt'')),
    displayName nvarchar(120) NOT NULL,
    description nvarchar(4000) NOT NULL,
    services nvarchar(4000) NOT NULL,
    version rowversion NOT NULL,
    UNIQUE (settingsId, locale)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.BusinessHour (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    settingsId bigint NOT NULL REFERENCES dbo.BusinessSettings(id),
    weekday tinyint NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    opensAt time(0) NOT NULL,
    closesAt time(0) NOT NULL,
    version rowversion NOT NULL,
    CHECK (opensAt < closesAt),
    UNIQUE (settingsId, weekday, opensAt)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.ClosedDate (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    visitDate date NOT NULL UNIQUE,
    reason nvarchar(500) NOT NULL,
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.VisitDay (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    visitDate date NOT NULL UNIQUE,
    capacityOverride int NULL CHECK (capacityOverride >= 0),
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.RatePlan (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0),
    childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0),
    seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0),
    parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0),
    effectiveAt datetime2(7) NOT NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.ExchangeRate (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    currency varchar(3) NOT NULL UNIQUE CHECK (currency IN (''USD'',''BRL'')),
    rate decimal(19,8) NOT NULL CHECK (rate > 0),
    updatedAt datetime2(7) NOT NULL,
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.AppUser (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username nvarchar(80) NOT NULL UNIQUE,
    passwordHash varchar(100) NOT NULL,
    name nvarchar(120) NOT NULL,
    email nvarchar(254) NOT NULL,
    phone nvarchar(40) NOT NULL,
    role varchar(5) NOT NULL CHECK (role IN (''ADMIN'',''STAFF'')),
    active bit NOT NULL DEFAULT 1,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Reservation (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    visitDayId bigint NOT NULL REFERENCES dbo.VisitDay(id),
    ratePlanId bigint NOT NULL REFERENCES dbo.RatePlan(id),
    name nvarchar(120) NOT NULL,
    email nvarchar(254) NOT NULL,
    phone nvarchar(40) NOT NULL,
    note nvarchar(1000) NULL,
    adults int NOT NULL CHECK (adults >= 0),
    children int NOT NULL CHECK (children >= 0),
    seniors int NOT NULL CHECK (seniors >= 0),
    vehicles int NOT NULL CHECK (vehicles >= 0),
    adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0),
    childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0),
    seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0),
    parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0),
    currency varchar(3) NOT NULL DEFAULT ''CRC'' CHECK (currency = ''CRC''),
    total decimal(19,2) NOT NULL CHECK (total >= 0),
    locale varchar(2) NOT NULL DEFAULT ''es'' CHECK (locale IN (''es'',''en'',''pt'')),
    status varchar(10) NOT NULL CHECK (status IN (''CONFIRMED'',''CHECKED_IN'',''CANCELLED'',''NO_SHOW'')),
    statusReason nvarchar(1000) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    CHECK (CAST(adults AS bigint) + children + seniors > 0),
    CHECK (total = CAST(adults AS decimal(19,2)) * adultPrice + CAST(children AS decimal(19,2)) * childPrice + CAST(seniors AS decimal(19,2)) * seniorPrice + CAST(vehicles AS decimal(19,2)) * parkingPrice)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Admission (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    visitDayId bigint NOT NULL REFERENCES dbo.VisitDay(id),
    reservationId bigint NULL REFERENCES dbo.Reservation(id),
    name nvarchar(120) NULL,
    email nvarchar(254) NULL,
    phone nvarchar(40) NULL,
    note nvarchar(1000) NULL,
    adults int NOT NULL CHECK (adults >= 0),
    children int NOT NULL CHECK (children >= 0),
    seniors int NOT NULL CHECK (seniors >= 0),
    vehicles int NOT NULL CHECK (vehicles >= 0),
    adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0),
    childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0),
    seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0),
    parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0),
    currency varchar(3) NOT NULL DEFAULT ''CRC'' CHECK (currency = ''CRC''),
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    CHECK (CAST(adults AS bigint) + children + seniors > 0)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.CashSession (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    businessDate date NOT NULL UNIQUE,
    openingFloat decimal(19,2) NOT NULL CHECK (openingFloat >= 0),
    status varchar(6) NOT NULL CHECK (status IN (''OPEN'',''CLOSED'')),
    openedBy bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Payment (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admissionId bigint NOT NULL UNIQUE REFERENCES dbo.Admission(id),
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN (''CASH'',''SINPE'',''CARD'')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    receivedAmount decimal(19,2) NULL,
    changeAmount decimal(19,2) NULL,
    reference nvarchar(120) NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK ((method = ''CASH'' AND receivedAmount IS NOT NULL AND changeAmount IS NOT NULL AND receivedAmount >= amount AND changeAmount = receivedAmount - amount) OR (method <> ''CASH'' AND receivedAmount IS NULL AND changeAmount IS NULL))
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Expense (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN (''CASH'',''SINPE'',''CARD'')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    category nvarchar(120) NOT NULL,
    description nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.AdmissionRevision (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admissionId bigint NOT NULL REFERENCES dbo.Admission(id),
    previousAdults int NOT NULL CHECK (previousAdults >= 0),
    previousChildren int NOT NULL CHECK (previousChildren >= 0),
    previousSeniors int NOT NULL CHECK (previousSeniors >= 0),
    previousVehicles int NOT NULL CHECK (previousVehicles >= 0),
    adults int NOT NULL CHECK (adults >= 0),
    children int NOT NULL CHECK (children >= 0),
    seniors int NOT NULL CHECK (seniors >= 0),
    vehicles int NOT NULL CHECK (vehicles >= 0),
    difference decimal(19,2) NOT NULL,
    reason nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (CAST(adults AS bigint) + children + seniors > 0)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Refund (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admissionId bigint NOT NULL REFERENCES dbo.Admission(id),
    paymentId bigint NULL REFERENCES dbo.Payment(id),
    revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id),
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN (''CASH'',''SINPE'',''CARD'')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    reason nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Adjustment (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    admissionId bigint NULL REFERENCES dbo.Admission(id),
    revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id),
    expenseId bigint NULL REFERENCES dbo.Expense(id),
    refundId bigint NULL REFERENCES dbo.Refund(id),
    originalAdjustmentId bigint NULL REFERENCES dbo.Adjustment(id),
    kind varchar(20) NOT NULL CHECK (kind IN (''ADDITIONAL_PAYMENT'',''EXPENSE_COMPENSATION'',''REFUND_COMPENSATION'',''ADJUSTMENT_REVERSAL'')),
    method varchar(5) NOT NULL CHECK (method IN (''CASH'',''SINPE'',''CARD'')),
    amount decimal(19,2) NOT NULL CHECK (amount <> 0),
    reason nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK ((kind = ''ADDITIONAL_PAYMENT'' AND admissionId IS NOT NULL AND revisionId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = ''EXPENSE_COMPENSATION'' AND expenseId IS NOT NULL AND amount > 0 AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = ''REFUND_COMPENSATION'' AND refundId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND originalAdjustmentId IS NULL) OR (kind = ''ADJUSTMENT_REVERSAL'' AND originalAdjustmentId IS NOT NULL AND expenseId IS NULL AND refundId IS NULL))
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.CashClosure (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cashSessionId bigint NOT NULL UNIQUE REFERENCES dbo.CashSession(id),
    countedCash decimal(19,2) NOT NULL CHECK (countedCash >= 0),
    expectedCash decimal(19,2) NOT NULL,
    difference decimal(19,2) NOT NULL,
    operatingResult decimal(19,2) NOT NULL,
    note nvarchar(1000) NULL,
    snapshot nvarchar(max) NOT NULL CHECK (ISJSON(snapshot) = 1),
    recipient nvarchar(254) NOT NULL,
    locale varchar(2) NOT NULL DEFAULT ''es'' CHECK (locale IN (''es'',''en'',''pt'')),
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (difference = countedCash - expectedCash),
    CHECK (difference = 0 OR (note IS NOT NULL AND LEN(LTRIM(RTRIM(note))) > 0))
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.Task (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    taskDate date NOT NULL,
    title nvarchar(120) NOT NULL CHECK (LEN(LTRIM(RTRIM(title))) >= 1),
    description nvarchar(2000) NOT NULL CHECK (LEN(LTRIM(RTRIM(description))) >= 1),
    status varchar(9) NOT NULL CHECK (status IN (''PENDING'',''COMPLETED'')),
    authorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    updatedBy bigint NOT NULL REFERENCES dbo.AppUser(id),
    updatedAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.EmailOutbox (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    eventType varchar(40) NOT NULL,
    eventId bigint NOT NULL,
    eventVersion int NOT NULL CHECK (eventVersion >= 1),
    recipient nvarchar(254) NOT NULL,
    locale varchar(2) NOT NULL DEFAULT ''es'' CHECK (locale IN (''es'',''en'',''pt'')),
    payload nvarchar(max) NOT NULL CHECK (ISJSON(payload) = 1),
    messageId varchar(120) NOT NULL UNIQUE,
    status varchar(10) NOT NULL DEFAULT ''PENDING'' CHECK (status IN (''PENDING'',''PROCESSING'',''SENT'',''FAILED'')),
    attempts int NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    availableAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    lockedAt datetime2(7) NULL,
    sentAt datetime2(7) NULL,
    lastErrorCode varchar(80) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    UNIQUE (eventType, eventId, eventVersion)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.EmailAttempt (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    outboxId bigint NOT NULL REFERENCES dbo.EmailOutbox(id),
    attemptNumber int NOT NULL CHECK (attemptNumber >= 1),
    result varchar(20) NOT NULL,
    errorCode varchar(80) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (outboxId, attemptNumber)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.IdempotencyRecord (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    operation varchar(80) NOT NULL,
    keyHash char(64) NOT NULL,
    requestHash char(64) NOT NULL,
    responseStatus int NOT NULL CHECK (responseStatus BETWEEN 200 AND 599),
    responseBody nvarchar(max) NOT NULL CHECK (ISJSON(responseBody) = 1),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    expiresAt datetime2(7) NOT NULL,
    UNIQUE (operation, keyHash),
    CHECK (expiresAt >= DATEADD(hour, 48, createdAt))
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.AuditLog (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    entity varchar(80) NOT NULL,
    entityId bigint NOT NULL,
    operation varchar(6) NOT NULL CHECK (operation IN (''INSERT'',''UPDATE'',''DELETE'')),
    occurredAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    actorId bigint NULL REFERENCES dbo.AppUser(id),
    origin varchar(12) NOT NULL CHECK (origin IN (''APPLICATION'',''EXTERNAL'')),
    requestId uniqueidentifier NULL,
    beforeData nvarchar(max) NULL CHECK (ISJSON(beforeData) = 1),
    afterData nvarchar(max) NULL CHECK (ISJSON(afterData) = 1)
);
';
    EXEC sys.sp_executesql N'
CREATE TABLE dbo.ActivityEvent (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    eventType varchar(80) NOT NULL,
    entity varchar(80) NULL,
    entityId bigint NULL,
    actorId bigint NULL REFERENCES dbo.AppUser(id),
    requestId uniqueidentifier NULL,
    outcome varchar(20) NOT NULL,
    occurredAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
';
    EXEC sys.sp_executesql N'
CREATE UNIQUE INDEX UX_Admission_Reservation ON dbo.Admission(reservationId) WHERE reservationId IS NOT NULL;
CREATE UNIQUE INDEX UX_CashSession_Open ON dbo.CashSession(status) WHERE status = ''OPEN'';
CREATE UNIQUE INDEX UX_Refund_Revision ON dbo.Refund(revisionId) WHERE revisionId IS NOT NULL;
CREATE UNIQUE INDEX UX_Adjustment_Revision ON dbo.Adjustment(revisionId) WHERE revisionId IS NOT NULL;
CREATE INDEX IX_Reservation_Day_Status ON dbo.Reservation(visitDayId, status) INCLUDE (adults, children, seniors);
CREATE INDEX IX_Reservation_Name ON dbo.Reservation(name);
CREATE INDEX IX_Admission_Day ON dbo.Admission(visitDayId) INCLUDE (adults, children, seniors, vehicles);
CREATE INDEX IX_Admission_Name ON dbo.Admission(name);
CREATE INDEX IX_RatePlan_Effective ON dbo.RatePlan(effectiveAt DESC, id DESC);
CREATE INDEX IX_Task_Date_Status ON dbo.Task(taskDate, status);
CREATE INDEX IX_Outbox_Work ON dbo.EmailOutbox(status, availableAt) INCLUDE (lockedAt, attempts);
CREATE INDEX IX_Idempotency_Expiry ON dbo.IdempotencyRecord(expiresAt);
CREATE INDEX IX_AuditLog_Time ON dbo.AuditLog(occurredAt DESC, id DESC);
CREATE INDEX IX_AuditLog_Request ON dbo.AuditLog(requestId);
CREATE INDEX IX_AuditLog_Actor ON dbo.AuditLog(actorId, occurredAt DESC);
CREATE INDEX IX_AuditLog_Entity ON dbo.AuditLog(entity, entityId, occurredAt DESC);
CREATE INDEX IX_ActivityEvent_Time ON dbo.ActivityEvent(occurredAt DESC, id DESC);
CREATE INDEX IX_Payment_Session ON dbo.Payment(cashSessionId);
CREATE INDEX IX_Expense_Session ON dbo.Expense(cashSessionId);
CREATE INDEX IX_Refund_Session ON dbo.Refund(cashSessionId);
CREATE INDEX IX_Adjustment_Session ON dbo.Adjustment(cashSessionId);
CREATE INDEX IX_Refund_Admission ON dbo.Refund(admissionId);
CREATE INDEX IX_Adjustment_Admission ON dbo.Adjustment(admissionId);
CREATE INDEX IX_AdmissionRevision_Admission ON dbo.AdmissionRevision(admissionId);
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(1,N'1',N'initial schema',N'SQL',N'V1__initial_schema.sql',989750783,LEFT(SUSER_SNAME(),100),0,1);

    -- V2__audit_and_immutability.sql
    EXEC sys.sp_executesql N'CREATE TRIGGER dbo.TR_BusinessSettings_Audit ON dbo.BusinessSettings
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessSettings'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.singleton, d.defaultCapacity, d.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.singleton, i.defaultCapacity, i.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.singleton, i.defaultCapacity, i.phone, i.email, i.address, i.sinpeNumber, i.enabled EXCEPT SELECT d.id, d.singleton, d.defaultCapacity, d.phone, d.email, d.address, d.sinpeNumber, d.enabled);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_BusinessTranslation_Audit ON dbo.BusinessTranslation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessTranslation'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.locale, d.displayName, d.description, d.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.locale, i.displayName, i.description, i.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, i.locale, i.displayName, i.description, i.services EXCEPT SELECT d.id, d.settingsId, d.locale, d.displayName, d.description, d.services);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_BusinessHour_Audit ON dbo.BusinessHour
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessHour'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt EXCEPT SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_ClosedDate_Audit ON dbo.ClosedDate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''ClosedDate'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, i.reason EXCEPT SELECT d.id, d.visitDate, d.reason);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_VisitDay_Audit ON dbo.VisitDay
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''VisitDay'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate, d.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate, i.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, i.capacityOverride EXCEPT SELECT d.id, d.visitDate, d.capacityOverride);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_RatePlan_Audit ON dbo.RatePlan
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''RatePlan'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt EXCEPT SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_ExchangeRate_Audit ON dbo.ExchangeRate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''ExchangeRate'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.currency, d.rate, d.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.currency, i.rate, i.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.currency, i.rate, i.updatedAt EXCEPT SELECT d.id, d.currency, d.rate, d.updatedAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_AppUser_Audit ON dbo.AppUser
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''AppUser'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.role, d.active, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.role, i.active, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.username, i.passwordHash, i.name, i.email, i.phone, i.role, i.active, i.createdAt EXCEPT SELECT d.id, d.username, d.passwordHash, d.name, d.email, d.phone, d.role, d.active, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Reservation_Audit ON dbo.Reservation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Reservation'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.ratePlanId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.total, d.locale, d.status, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.ratePlanId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.total, i.locale, i.status, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.code, i.visitDayId, i.ratePlanId, i.name, i.email, i.phone, i.note, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.total, i.locale, i.status, i.statusReason, i.createdAt EXCEPT SELECT d.id, d.code, d.visitDayId, d.ratePlanId, d.name, d.email, d.phone, d.note, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.total, d.locale, d.status, d.statusReason, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Admission_Audit ON dbo.Admission
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Admission'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.reservationId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.reservationId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.code, i.visitDayId, i.reservationId, i.name, i.email, i.phone, i.note, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.actorId, i.createdAt EXCEPT SELECT d.id, d.code, d.visitDayId, d.reservationId, d.name, d.email, d.phone, d.note, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_CashSession_Audit ON dbo.CashSession
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''CashSession'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.businessDate, d.openingFloat, d.status, d.openedBy, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.businessDate, i.openingFloat, i.status, i.openedBy, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.businessDate, i.openingFloat, i.status, i.openedBy, i.createdAt EXCEPT SELECT d.id, d.businessDate, d.openingFloat, d.status, d.openedBy, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Payment_Audit ON dbo.Payment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Payment'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.cashSessionId, d.method, d.amount, d.receivedAmount, d.changeAmount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.cashSessionId, i.method, i.amount, i.receivedAmount, i.changeAmount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.cashSessionId, i.method, i.amount, i.receivedAmount, i.changeAmount, i.reference, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.cashSessionId, d.method, d.amount, d.receivedAmount, d.changeAmount, d.reference, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Expense_Audit ON dbo.Expense
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Expense'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.method, d.amount, d.category, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.method, i.amount, i.category, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.method, i.amount, i.category, i.description, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.method, d.amount, d.category, d.description, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_AdmissionRevision_Audit ON dbo.AdmissionRevision
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''AdmissionRevision'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, d.reason, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Refund_Audit ON dbo.Refund
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Refund'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, i.method, i.amount, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, d.method, d.amount, d.reason, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Adjustment_Audit ON dbo.Adjustment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Adjustment'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.reason, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_CashClosure_Audit ON dbo.CashClosure
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''CashClosure'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, d.locale, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, i.locale, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, i.note, i.snapshot, i.recipient, i.locale, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, d.note, d.snapshot, d.recipient, d.locale, d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Task_Audit ON dbo.Task
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Task'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.taskDate, d.status, d.authorId, d.updatedBy, d.updatedAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.taskDate, i.status, i.authorId, i.updatedBy, i.updatedAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.taskDate, i.title, i.description, i.status, i.authorId, i.updatedBy, i.updatedAt, i.createdAt EXCEPT SELECT d.id, d.taskDate, d.title, d.description, d.status, d.authorId, d.updatedBy, d.updatedAt, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_EmailOutbox_Audit ON dbo.EmailOutbox
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''EmailOutbox'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.eventType, d.eventId, d.eventVersion, d.locale, d.status, d.attempts, d.availableAt, d.lockedAt, d.sentAt, d.lastErrorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.eventType, i.eventId, i.eventVersion, i.locale, i.status, i.attempts, i.availableAt, i.lockedAt, i.sentAt, i.lastErrorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.eventType, i.eventId, i.eventVersion, i.recipient, i.locale, i.payload, i.messageId, i.status, i.attempts, i.availableAt, i.lockedAt, i.sentAt, i.lastErrorCode, i.createdAt EXCEPT SELECT d.id, d.eventType, d.eventId, d.eventVersion, d.recipient, d.locale, d.payload, d.messageId, d.status, d.attempts, d.availableAt, d.lockedAt, d.sentAt, d.lastErrorCode, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_EmailAttempt_Audit ON dbo.EmailAttempt
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''EmailAttempt'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.outboxId, d.attemptNumber, d.result, d.errorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.outboxId, i.attemptNumber, i.result, i.errorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.outboxId, i.attemptNumber, i.result, i.errorCode, i.createdAt EXCEPT SELECT d.id, d.outboxId, d.attemptNumber, d.result, d.errorCode, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_RatePlan_Immutable ON dbo.RatePlan
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Payment_Immutable ON dbo.Payment
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Expense_Immutable ON dbo.Expense
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Refund_Immutable ON dbo.Refund
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_Adjustment_Immutable ON dbo.Adjustment
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_AdmissionRevision_Immutable ON dbo.AdmissionRevision
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_CashClosure_Immutable ON dbo.CashClosure
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_EmailAttempt_Immutable ON dbo.EmailAttempt
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, ''Immutable record cannot be changed or deleted'', 1;
END;
';
    EXEC sys.sp_executesql N'
CREATE TRIGGER dbo.TR_CashSession_Closed ON dbo.CashSession
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted WHERE status = ''CLOSED'')
        THROW 51002, ''Closed session cannot be changed or deleted'', 1;
END;
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(2,N'2',N'audit and immutability',N'SQL',N'V2__audit_and_immutability.sql',-240893265,LEFT(SUSER_SNAME(),100),0,1);

    -- V3__precise_audit_changes.sql
    EXEC sys.sp_executesql N'CREATE OR ALTER TRIGGER dbo.TR_BusinessSettings_Audit ON dbo.BusinessSettings
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessSettings'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.singleton, d.defaultCapacity, d.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.singleton, i.defaultCapacity, i.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.singleton, i.defaultCapacity, CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.address), CONVERT(varbinary(max), i.sinpeNumber), i.enabled EXCEPT SELECT d.id, d.singleton, d.defaultCapacity, CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.address), CONVERT(varbinary(max), d.sinpeNumber), d.enabled);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_BusinessTranslation_Audit ON dbo.BusinessTranslation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessTranslation'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.locale, d.displayName, d.description, d.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.locale, i.displayName, i.description, i.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.displayName), CONVERT(varbinary(max), i.description), CONVERT(varbinary(max), i.services) EXCEPT SELECT d.id, d.settingsId, CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.displayName), CONVERT(varbinary(max), d.description), CONVERT(varbinary(max), d.services));
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_BusinessHour_Audit ON dbo.BusinessHour
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''BusinessHour'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt EXCEPT SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_ClosedDate_Audit ON dbo.ClosedDate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''ClosedDate'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, CONVERT(varbinary(max), i.reason) EXCEPT SELECT d.id, d.visitDate, CONVERT(varbinary(max), d.reason));
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_VisitDay_Audit ON dbo.VisitDay
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''VisitDay'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate, d.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate, i.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, i.capacityOverride EXCEPT SELECT d.id, d.visitDate, d.capacityOverride);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_RatePlan_Audit ON dbo.RatePlan
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''RatePlan'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt EXCEPT SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_ExchangeRate_Audit ON dbo.ExchangeRate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''ExchangeRate'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.currency, d.rate, d.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.currency, i.rate, i.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.currency), i.rate, i.updatedAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.currency), d.rate, d.updatedAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_AppUser_Audit ON dbo.AppUser
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''AppUser'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.role, d.active, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.role, i.active, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.username), CONVERT(varbinary(max), i.passwordHash), CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.role), i.active, i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.username), CONVERT(varbinary(max), d.passwordHash), CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.role), d.active, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Reservation_Audit ON dbo.Reservation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Reservation'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.ratePlanId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.total, d.locale, d.status, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.ratePlanId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.total, i.locale, i.status, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.code), i.visitDayId, i.ratePlanId, CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.note), i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, CONVERT(varbinary(max), i.currency), i.total, CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.status), CONVERT(varbinary(max), i.statusReason), i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.code), d.visitDayId, d.ratePlanId, CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.note), d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, CONVERT(varbinary(max), d.currency), d.total, CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.status), CONVERT(varbinary(max), d.statusReason), d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Admission_Audit ON dbo.Admission
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Admission'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.reservationId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.reservationId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.code), i.visitDayId, i.reservationId, CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.note), i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, CONVERT(varbinary(max), i.currency), i.actorId, i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.code), d.visitDayId, d.reservationId, CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.note), d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, CONVERT(varbinary(max), d.currency), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_CashSession_Audit ON dbo.CashSession
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''CashSession'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.businessDate, d.openingFloat, d.status, d.openedBy, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.businessDate, i.openingFloat, i.status, i.openedBy, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.businessDate, i.openingFloat, CONVERT(varbinary(max), i.status), i.openedBy, i.createdAt EXCEPT SELECT d.id, d.businessDate, d.openingFloat, CONVERT(varbinary(max), d.status), d.openedBy, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Payment_Audit ON dbo.Payment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Payment'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.cashSessionId, d.method, d.amount, d.receivedAmount, d.changeAmount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.cashSessionId, i.method, i.amount, i.receivedAmount, i.changeAmount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, i.receivedAmount, i.changeAmount, CONVERT(varbinary(max), i.reference), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, d.receivedAmount, d.changeAmount, CONVERT(varbinary(max), d.reference), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Expense_Audit ON dbo.Expense
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Expense'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.method, d.amount, d.category, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.method, i.amount, i.category, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.category), CONVERT(varbinary(max), i.description), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.category), CONVERT(varbinary(max), d.description), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_AdmissionRevision_Audit ON dbo.AdmissionRevision
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''AdmissionRevision'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Refund_Audit ON dbo.Refund
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Refund'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Adjustment_Audit ON dbo.Adjustment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Adjustment'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, CONVERT(varbinary(max), i.kind), CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, CONVERT(varbinary(max), d.kind), CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_CashClosure_Audit ON dbo.CashClosure
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''CashClosure'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, d.locale, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, i.locale, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, CONVERT(varbinary(max), i.note), CONVERT(varbinary(max), i.snapshot), CONVERT(varbinary(max), i.recipient), CONVERT(varbinary(max), i.locale), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, CONVERT(varbinary(max), d.note), CONVERT(varbinary(max), d.snapshot), CONVERT(varbinary(max), d.recipient), CONVERT(varbinary(max), d.locale), d.actorId, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Task_Audit ON dbo.Task
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Task'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.taskDate, d.status, d.authorId, d.updatedBy, d.updatedAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.taskDate, i.status, i.authorId, i.updatedBy, i.updatedAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.taskDate, CONVERT(varbinary(max), i.title), CONVERT(varbinary(max), i.description), CONVERT(varbinary(max), i.status), i.authorId, i.updatedBy, i.updatedAt, i.createdAt EXCEPT SELECT d.id, d.taskDate, CONVERT(varbinary(max), d.title), CONVERT(varbinary(max), d.description), CONVERT(varbinary(max), d.status), d.authorId, d.updatedBy, d.updatedAt, d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_EmailOutbox_Audit ON dbo.EmailOutbox
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''EmailOutbox'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.eventType, d.eventId, d.eventVersion, d.locale, d.status, d.attempts, d.availableAt, d.lockedAt, d.sentAt, d.lastErrorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.eventType, i.eventId, i.eventVersion, i.locale, i.status, i.attempts, i.availableAt, i.lockedAt, i.sentAt, i.lastErrorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.eventType), i.eventId, i.eventVersion, CONVERT(varbinary(max), i.recipient), CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.payload), CONVERT(varbinary(max), i.messageId), CONVERT(varbinary(max), i.status), i.attempts, i.availableAt, i.lockedAt, i.sentAt, CONVERT(varbinary(max), i.lastErrorCode), i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.eventType), d.eventId, d.eventVersion, CONVERT(varbinary(max), d.recipient), CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.payload), CONVERT(varbinary(max), d.messageId), CONVERT(varbinary(max), d.status), d.attempts, d.availableAt, d.lockedAt, d.sentAt, CONVERT(varbinary(max), d.lastErrorCode), d.createdAt);
END;
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_EmailAttempt_Audit ON dbo.EmailAttempt
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''EmailAttempt'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.outboxId, d.attemptNumber, d.result, d.errorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.outboxId, i.attemptNumber, i.result, i.errorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.outboxId, i.attemptNumber, CONVERT(varbinary(max), i.result), CONVERT(varbinary(max), i.errorCode), i.createdAt EXCEPT SELECT d.id, d.outboxId, d.attemptNumber, CONVERT(varbinary(max), d.result), CONVERT(varbinary(max), d.errorCode), d.createdAt);
END;
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(3,N'3',N'precise audit changes',N'SQL',N'V3__precise_audit_changes.sql',-1434710715,LEFT(SUSER_SNAME(),100),0,1);

    -- V4__configuration_permissions.sql
    EXEC sys.sp_executesql N'IF DATABASE_PRINCIPAL_ID(N''tourism_app_local'') IS NOT NULL
    GRANT DELETE ON dbo.ExchangeRate TO tourism_app_local;
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(4,N'4',N'configuration permissions',N'SQL',N'V4__configuration_permissions.sql',572530039,LEFT(SUSER_SNAME(),100),0,1);

    -- V5__adjustment_receipts.sql
    EXEC sys.sp_executesql N'ALTER TABLE dbo.Adjustment ADD receivedAmount decimal(19,2) NULL, changeAmount decimal(19,2) NULL, reference nvarchar(120) NULL;
';
    EXEC sys.sp_executesql N'
ALTER TABLE dbo.Adjustment ADD CONSTRAINT CK_Adjustment_CashReceipt CHECK ((receivedAmount IS NULL AND changeAmount IS NULL) OR (kind=''ADDITIONAL_PAYMENT'' AND method=''CASH'' AND receivedAmount IS NOT NULL AND changeAmount IS NOT NULL AND receivedAmount>=amount AND changeAmount=receivedAmount-amount));
';
    EXEC sys.sp_executesql N'
CREATE OR ALTER TRIGGER dbo.TR_Adjustment_Audit ON dbo.Adjustment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT ''Adjustment'', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN ''INSERT'' WHEN i.id IS NULL THEN ''DELETE'' ELSE ''UPDATE'' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N''actorId'')),
        CASE WHEN SESSION_CONTEXT(N''requestId'') IS NULL THEN ''EXTERNAL'' ELSE ''APPLICATION'' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N''requestId'')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.receivedAmount, d.changeAmount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.receivedAmount, i.changeAmount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, CONVERT(varbinary(max), i.kind), CONVERT(varbinary(max), i.method), i.amount, i.receivedAmount, i.changeAmount, CONVERT(varbinary(max), i.reference), CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, CONVERT(varbinary(max), d.kind), CONVERT(varbinary(max), d.method), d.amount, d.receivedAmount, d.changeAmount, CONVERT(varbinary(max), d.reference), CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(5,N'5',N'adjustment receipts',N'SQL',N'V5__adjustment_receipts.sql',387604568,LEFT(SUSER_SNAME(),100),0,1);

    -- V6__unique_compensations.sql
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_Adjustment_Reversal ON dbo.Adjustment(originalAdjustmentId) WHERE originalAdjustmentId IS NOT NULL;
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(6,N'6',N'unique compensations',N'SQL',N'V6__unique_compensations.sql',1447958618,LEFT(SUSER_SNAME(),100),0,1);

    -- V7__activity_search_indexes.sql
    EXEC sys.sp_executesql N'CREATE INDEX IX_ActivityEvent_Request ON dbo.ActivityEvent(requestId);
CREATE INDEX IX_ActivityEvent_Actor ON dbo.ActivityEvent(actorId,occurredAt DESC);
CREATE INDEX IX_ActivityEvent_Entity ON dbo.ActivityEvent(entity,occurredAt DESC);
';
    INSERT dbo.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success)
    VALUES(7,N'7',N'activity search indexes',N'SQL',N'V7__activity_search_indexes.sql',1087680199,LEFT(SUSER_SNAME(),100),0,1);
    COMMIT TRANSACTION;
    PRINT N'Esquema V1-V7 instalado. Ejecute 04-verificar.sql.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
