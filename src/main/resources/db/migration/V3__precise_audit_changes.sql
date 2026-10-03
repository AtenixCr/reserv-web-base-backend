CREATE OR ALTER TRIGGER dbo.TR_BusinessSettings_Audit ON dbo.BusinessSettings
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'BusinessSettings', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.singleton, d.defaultCapacity, d.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.singleton, i.defaultCapacity, i.enabled FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.singleton, i.defaultCapacity, CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.address), CONVERT(varbinary(max), i.sinpeNumber), i.enabled EXCEPT SELECT d.id, d.singleton, d.defaultCapacity, CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.address), CONVERT(varbinary(max), d.sinpeNumber), d.enabled);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_BusinessTranslation_Audit ON dbo.BusinessTranslation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'BusinessTranslation', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.locale, d.displayName, d.description, d.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.locale, i.displayName, i.description, i.services FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.displayName), CONVERT(varbinary(max), i.description), CONVERT(varbinary(max), i.services) EXCEPT SELECT d.id, d.settingsId, CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.displayName), CONVERT(varbinary(max), d.description), CONVERT(varbinary(max), d.services));
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_BusinessHour_Audit ON dbo.BusinessHour
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'BusinessHour', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, i.weekday, i.opensAt, i.closesAt EXCEPT SELECT d.id, d.settingsId, d.weekday, d.opensAt, d.closesAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_ClosedDate_Audit ON dbo.ClosedDate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'ClosedDate', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, CONVERT(varbinary(max), i.reason) EXCEPT SELECT d.id, d.visitDate, CONVERT(varbinary(max), d.reason));
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_VisitDay_Audit ON dbo.VisitDay
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'VisitDay', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.visitDate, d.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.visitDate, i.capacityOverride FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, i.capacityOverride EXCEPT SELECT d.id, d.visitDate, d.capacityOverride);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_RatePlan_Audit ON dbo.RatePlan
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'RatePlan', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.effectiveAt, i.createdAt EXCEPT SELECT d.id, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.effectiveAt, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_ExchangeRate_Audit ON dbo.ExchangeRate
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'ExchangeRate', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.currency, d.rate, d.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.currency, i.rate, i.updatedAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.currency), i.rate, i.updatedAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.currency), d.rate, d.updatedAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_AppUser_Audit ON dbo.AppUser
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'AppUser', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.role, d.active, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.role, i.active, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.username), CONVERT(varbinary(max), i.passwordHash), CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.role), i.active, i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.username), CONVERT(varbinary(max), d.passwordHash), CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.role), d.active, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Reservation_Audit ON dbo.Reservation
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Reservation', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.ratePlanId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.total, d.locale, d.status, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.ratePlanId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.total, i.locale, i.status, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.code), i.visitDayId, i.ratePlanId, CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.note), i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, CONVERT(varbinary(max), i.currency), i.total, CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.status), CONVERT(varbinary(max), i.statusReason), i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.code), d.visitDayId, d.ratePlanId, CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.note), d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, CONVERT(varbinary(max), d.currency), d.total, CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.status), CONVERT(varbinary(max), d.statusReason), d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Admission_Audit ON dbo.Admission
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Admission', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.code, d.visitDayId, d.reservationId, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.code, i.visitDayId, i.reservationId, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.code), i.visitDayId, i.reservationId, CONVERT(varbinary(max), i.name), CONVERT(varbinary(max), i.email), CONVERT(varbinary(max), i.phone), CONVERT(varbinary(max), i.note), i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, CONVERT(varbinary(max), i.currency), i.actorId, i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.code), d.visitDayId, d.reservationId, CONVERT(varbinary(max), d.name), CONVERT(varbinary(max), d.email), CONVERT(varbinary(max), d.phone), CONVERT(varbinary(max), d.note), d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, CONVERT(varbinary(max), d.currency), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_CashSession_Audit ON dbo.CashSession
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'CashSession', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.businessDate, d.openingFloat, d.status, d.openedBy, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.businessDate, i.openingFloat, i.status, i.openedBy, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.businessDate, i.openingFloat, CONVERT(varbinary(max), i.status), i.openedBy, i.createdAt EXCEPT SELECT d.id, d.businessDate, d.openingFloat, CONVERT(varbinary(max), d.status), d.openedBy, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Payment_Audit ON dbo.Payment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Payment', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.cashSessionId, d.method, d.amount, d.receivedAmount, d.changeAmount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.cashSessionId, i.method, i.amount, i.receivedAmount, i.changeAmount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, i.receivedAmount, i.changeAmount, CONVERT(varbinary(max), i.reference), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, d.receivedAmount, d.changeAmount, CONVERT(varbinary(max), d.reference), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Expense_Audit ON dbo.Expense
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Expense', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.method, d.amount, d.category, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.method, i.amount, i.category, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.category), CONVERT(varbinary(max), i.description), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.category), CONVERT(varbinary(max), d.description), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_AdmissionRevision_Audit ON dbo.AdmissionRevision
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'AdmissionRevision', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Refund_Audit ON dbo.Refund
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Refund', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Adjustment_Audit ON dbo.Adjustment
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Adjustment', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, CONVERT(varbinary(max), i.kind), CONVERT(varbinary(max), i.method), i.amount, CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, CONVERT(varbinary(max), d.kind), CONVERT(varbinary(max), d.method), d.amount, CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_CashClosure_Audit ON dbo.CashClosure
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'CashClosure', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, d.locale, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, i.locale, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, CONVERT(varbinary(max), i.note), CONVERT(varbinary(max), i.snapshot), CONVERT(varbinary(max), i.recipient), CONVERT(varbinary(max), i.locale), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, CONVERT(varbinary(max), d.note), CONVERT(varbinary(max), d.snapshot), CONVERT(varbinary(max), d.recipient), CONVERT(varbinary(max), d.locale), d.actorId, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_Task_Audit ON dbo.Task
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'Task', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.taskDate, d.status, d.authorId, d.updatedBy, d.updatedAt, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.taskDate, i.status, i.authorId, i.updatedBy, i.updatedAt, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.taskDate, CONVERT(varbinary(max), i.title), CONVERT(varbinary(max), i.description), CONVERT(varbinary(max), i.status), i.authorId, i.updatedBy, i.updatedAt, i.createdAt EXCEPT SELECT d.id, d.taskDate, CONVERT(varbinary(max), d.title), CONVERT(varbinary(max), d.description), CONVERT(varbinary(max), d.status), d.authorId, d.updatedBy, d.updatedAt, d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_EmailOutbox_Audit ON dbo.EmailOutbox
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'EmailOutbox', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.eventType, d.eventId, d.eventVersion, d.locale, d.status, d.attempts, d.availableAt, d.lockedAt, d.sentAt, d.lastErrorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.eventType, i.eventId, i.eventVersion, i.locale, i.status, i.attempts, i.availableAt, i.lockedAt, i.sentAt, i.lastErrorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, CONVERT(varbinary(max), i.eventType), i.eventId, i.eventVersion, CONVERT(varbinary(max), i.recipient), CONVERT(varbinary(max), i.locale), CONVERT(varbinary(max), i.payload), CONVERT(varbinary(max), i.messageId), CONVERT(varbinary(max), i.status), i.attempts, i.availableAt, i.lockedAt, i.sentAt, CONVERT(varbinary(max), i.lastErrorCode), i.createdAt EXCEPT SELECT d.id, CONVERT(varbinary(max), d.eventType), d.eventId, d.eventVersion, CONVERT(varbinary(max), d.recipient), CONVERT(varbinary(max), d.locale), CONVERT(varbinary(max), d.payload), CONVERT(varbinary(max), d.messageId), CONVERT(varbinary(max), d.status), d.attempts, d.availableAt, d.lockedAt, d.sentAt, CONVERT(varbinary(max), d.lastErrorCode), d.createdAt);
END;
GO


CREATE OR ALTER TRIGGER dbo.TR_EmailAttempt_Audit ON dbo.EmailAttempt
AFTER INSERT, UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.AuditLog(entity, entityId, operation, actorId, origin, requestId, beforeData, afterData)
    SELECT 'EmailAttempt', COALESCE(i.id, d.id),
        CASE WHEN d.id IS NULL THEN 'INSERT' WHEN i.id IS NULL THEN 'DELETE' ELSE 'UPDATE' END,
        TRY_CONVERT(bigint, SESSION_CONTEXT(N'actorId')),
        CASE WHEN SESSION_CONTEXT(N'requestId') IS NULL THEN 'EXTERNAL' ELSE 'APPLICATION' END,
        TRY_CONVERT(uniqueidentifier, SESSION_CONTEXT(N'requestId')),
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.outboxId, d.attemptNumber, d.result, d.errorCode, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.outboxId, i.attemptNumber, i.result, i.errorCode, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.outboxId, i.attemptNumber, CONVERT(varbinary(max), i.result), CONVERT(varbinary(max), i.errorCode), i.createdAt EXCEPT SELECT d.id, d.outboxId, d.attemptNumber, CONVERT(varbinary(max), d.result), CONVERT(varbinary(max), d.errorCode), d.createdAt);
END;
GO
