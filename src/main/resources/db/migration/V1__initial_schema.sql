CREATE TABLE dbo.BusinessSettings (
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
GO

CREATE TABLE dbo.BusinessTranslation (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    settingsId bigint NOT NULL REFERENCES dbo.BusinessSettings(id),
    locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt')),
    displayName nvarchar(120) NOT NULL,
    description nvarchar(4000) NOT NULL,
    services nvarchar(4000) NOT NULL,
    version rowversion NOT NULL,
    UNIQUE (settingsId, locale)
);
GO

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
GO

CREATE TABLE dbo.ClosedDate (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    visitDate date NOT NULL UNIQUE,
    reason nvarchar(500) NOT NULL,
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.VisitDay (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    visitDate date NOT NULL UNIQUE,
    capacityOverride int NULL CHECK (capacityOverride >= 0),
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.RatePlan (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    adultPrice decimal(19,2) NOT NULL CHECK (adultPrice >= 0),
    childPrice decimal(19,2) NOT NULL CHECK (childPrice >= 0),
    seniorPrice decimal(19,2) NOT NULL CHECK (seniorPrice >= 0),
    parkingPrice decimal(19,2) NOT NULL CHECK (parkingPrice >= 0),
    effectiveAt datetime2(7) NOT NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dbo.ExchangeRate (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    currency varchar(3) NOT NULL UNIQUE CHECK (currency IN ('USD','BRL')),
    rate decimal(19,8) NOT NULL CHECK (rate > 0),
    updatedAt datetime2(7) NOT NULL,
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.AppUser (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    username nvarchar(80) NOT NULL UNIQUE,
    passwordHash varchar(100) NOT NULL,
    name nvarchar(120) NOT NULL,
    email nvarchar(254) NOT NULL,
    phone nvarchar(40) NOT NULL,
    role varchar(5) NOT NULL CHECK (role IN ('ADMIN','STAFF')),
    active bit NOT NULL DEFAULT 1,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
GO

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
    currency varchar(3) NOT NULL DEFAULT 'CRC' CHECK (currency = 'CRC'),
    total decimal(19,2) NOT NULL CHECK (total >= 0),
    locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt')),
    status varchar(10) NOT NULL CHECK (status IN ('CONFIRMED','CHECKED_IN','CANCELLED','NO_SHOW')),
    statusReason nvarchar(1000) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    CHECK (CAST(adults AS bigint) + children + seniors > 0),
    CHECK (total = CAST(adults AS decimal(19,2)) * adultPrice + CAST(children AS decimal(19,2)) * childPrice + CAST(seniors AS decimal(19,2)) * seniorPrice + CAST(vehicles AS decimal(19,2)) * parkingPrice)
);
GO

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
    currency varchar(3) NOT NULL DEFAULT 'CRC' CHECK (currency = 'CRC'),
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    CHECK (CAST(adults AS bigint) + children + seniors > 0)
);
GO

CREATE TABLE dbo.CashSession (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    businessDate date NOT NULL UNIQUE,
    openingFloat decimal(19,2) NOT NULL CHECK (openingFloat >= 0),
    status varchar(6) NOT NULL CHECK (status IN ('OPEN','CLOSED')),
    openedBy bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.Payment (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admissionId bigint NOT NULL UNIQUE REFERENCES dbo.Admission(id),
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    receivedAmount decimal(19,2) NULL,
    changeAmount decimal(19,2) NULL,
    reference nvarchar(120) NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK ((method = 'CASH' AND receivedAmount IS NOT NULL AND changeAmount IS NOT NULL AND receivedAmount >= amount AND changeAmount = receivedAmount - amount) OR (method <> 'CASH' AND receivedAmount IS NULL AND changeAmount IS NULL))
);
GO

CREATE TABLE dbo.Expense (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    category nvarchar(120) NOT NULL,
    description nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

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
GO

CREATE TABLE dbo.Refund (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    admissionId bigint NOT NULL REFERENCES dbo.Admission(id),
    paymentId bigint NULL REFERENCES dbo.Payment(id),
    revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id),
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD')),
    amount decimal(19,2) NOT NULL CHECK (amount > 0),
    reason nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dbo.Adjustment (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cashSessionId bigint NOT NULL REFERENCES dbo.CashSession(id),
    admissionId bigint NULL REFERENCES dbo.Admission(id),
    revisionId bigint NULL REFERENCES dbo.AdmissionRevision(id),
    expenseId bigint NULL REFERENCES dbo.Expense(id),
    refundId bigint NULL REFERENCES dbo.Refund(id),
    originalAdjustmentId bigint NULL REFERENCES dbo.Adjustment(id),
    kind varchar(20) NOT NULL CHECK (kind IN ('ADDITIONAL_PAYMENT','EXPENSE_COMPENSATION','REFUND_COMPENSATION','ADJUSTMENT_REVERSAL')),
    method varchar(5) NOT NULL CHECK (method IN ('CASH','SINPE','CARD')),
    amount decimal(19,2) NOT NULL CHECK (amount <> 0),
    reason nvarchar(1000) NOT NULL,
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK ((kind = 'ADDITIONAL_PAYMENT' AND admissionId IS NOT NULL AND revisionId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'EXPENSE_COMPENSATION' AND expenseId IS NOT NULL AND amount > 0 AND refundId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'REFUND_COMPENSATION' AND refundId IS NOT NULL AND amount > 0 AND expenseId IS NULL AND originalAdjustmentId IS NULL) OR (kind = 'ADJUSTMENT_REVERSAL' AND originalAdjustmentId IS NOT NULL AND expenseId IS NULL AND refundId IS NULL))
);
GO

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
    locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt')),
    actorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    CHECK (difference = countedCash - expectedCash),
    CHECK (difference = 0 OR (note IS NOT NULL AND LEN(LTRIM(RTRIM(note))) > 0))
);
GO

CREATE TABLE dbo.Task (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    taskDate date NOT NULL,
    title nvarchar(120) NOT NULL CHECK (LEN(LTRIM(RTRIM(title))) >= 1),
    description nvarchar(2000) NOT NULL CHECK (LEN(LTRIM(RTRIM(description))) >= 1),
    status varchar(9) NOT NULL CHECK (status IN ('PENDING','COMPLETED')),
    authorId bigint NOT NULL REFERENCES dbo.AppUser(id),
    updatedBy bigint NOT NULL REFERENCES dbo.AppUser(id),
    updatedAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL
);
GO

CREATE TABLE dbo.EmailOutbox (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    eventType varchar(40) NOT NULL,
    eventId bigint NOT NULL,
    eventVersion int NOT NULL CHECK (eventVersion >= 1),
    recipient nvarchar(254) NOT NULL,
    locale varchar(2) NOT NULL DEFAULT 'es' CHECK (locale IN ('es','en','pt')),
    payload nvarchar(max) NOT NULL CHECK (ISJSON(payload) = 1),
    messageId varchar(120) NOT NULL UNIQUE,
    status varchar(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PROCESSING','SENT','FAILED')),
    attempts int NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    availableAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    lockedAt datetime2(7) NULL,
    sentAt datetime2(7) NULL,
    lastErrorCode varchar(80) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    version rowversion NOT NULL,
    UNIQUE (eventType, eventId, eventVersion)
);
GO

CREATE TABLE dbo.EmailAttempt (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    outboxId bigint NOT NULL REFERENCES dbo.EmailOutbox(id),
    attemptNumber int NOT NULL CHECK (attemptNumber >= 1),
    result varchar(20) NOT NULL,
    errorCode varchar(80) NULL,
    createdAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    UNIQUE (outboxId, attemptNumber)
);
GO

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
GO

CREATE TABLE dbo.AuditLog (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    entity varchar(80) NOT NULL,
    entityId bigint NOT NULL,
    operation varchar(6) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE')),
    occurredAt datetime2(7) NOT NULL DEFAULT SYSUTCDATETIME(),
    actorId bigint NULL REFERENCES dbo.AppUser(id),
    origin varchar(12) NOT NULL CHECK (origin IN ('APPLICATION','EXTERNAL')),
    requestId uniqueidentifier NULL,
    beforeData nvarchar(max) NULL CHECK (ISJSON(beforeData) = 1),
    afterData nvarchar(max) NULL CHECK (ISJSON(afterData) = 1)
);
GO

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
GO

CREATE UNIQUE INDEX UX_Admission_Reservation ON dbo.Admission(reservationId) WHERE reservationId IS NOT NULL;
CREATE UNIQUE INDEX UX_CashSession_Open ON dbo.CashSession(status) WHERE status = 'OPEN';
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
GO
