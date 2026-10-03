CREATE TRIGGER dbo.TR_BusinessSettings_Audit ON dbo.BusinessSettings
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.singleton, i.defaultCapacity, i.phone, i.email, i.address, i.sinpeNumber, i.enabled EXCEPT SELECT d.id, d.singleton, d.defaultCapacity, d.phone, d.email, d.address, d.sinpeNumber, d.enabled);
END;
GO

CREATE TRIGGER dbo.TR_BusinessTranslation_Audit ON dbo.BusinessTranslation
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.settingsId, i.locale, i.displayName, i.description, i.services EXCEPT SELECT d.id, d.settingsId, d.locale, d.displayName, d.description, d.services);
END;
GO

CREATE TRIGGER dbo.TR_BusinessHour_Audit ON dbo.BusinessHour
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

CREATE TRIGGER dbo.TR_ClosedDate_Audit ON dbo.ClosedDate
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.visitDate, i.reason EXCEPT SELECT d.id, d.visitDate, d.reason);
END;
GO

CREATE TRIGGER dbo.TR_VisitDay_Audit ON dbo.VisitDay
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

CREATE TRIGGER dbo.TR_RatePlan_Audit ON dbo.RatePlan
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

CREATE TRIGGER dbo.TR_ExchangeRate_Audit ON dbo.ExchangeRate
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.currency, i.rate, i.updatedAt EXCEPT SELECT d.id, d.currency, d.rate, d.updatedAt);
END;
GO

CREATE TRIGGER dbo.TR_AppUser_Audit ON dbo.AppUser
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.username, i.passwordHash, i.name, i.email, i.phone, i.role, i.active, i.createdAt EXCEPT SELECT d.id, d.username, d.passwordHash, d.name, d.email, d.phone, d.role, d.active, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Reservation_Audit ON dbo.Reservation
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.code, i.visitDayId, i.ratePlanId, i.name, i.email, i.phone, i.note, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.total, i.locale, i.status, i.statusReason, i.createdAt EXCEPT SELECT d.id, d.code, d.visitDayId, d.ratePlanId, d.name, d.email, d.phone, d.note, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.total, d.locale, d.status, d.statusReason, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Admission_Audit ON dbo.Admission
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.code, i.visitDayId, i.reservationId, i.name, i.email, i.phone, i.note, i.adults, i.children, i.seniors, i.vehicles, i.adultPrice, i.childPrice, i.seniorPrice, i.parkingPrice, i.currency, i.actorId, i.createdAt EXCEPT SELECT d.id, d.code, d.visitDayId, d.reservationId, d.name, d.email, d.phone, d.note, d.adults, d.children, d.seniors, d.vehicles, d.adultPrice, d.childPrice, d.seniorPrice, d.parkingPrice, d.currency, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_CashSession_Audit ON dbo.CashSession
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.businessDate, i.openingFloat, i.status, i.openedBy, i.createdAt EXCEPT SELECT d.id, d.businessDate, d.openingFloat, d.status, d.openedBy, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Payment_Audit ON dbo.Payment
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.cashSessionId, i.method, i.amount, i.receivedAmount, i.changeAmount, i.reference, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.cashSessionId, d.method, d.amount, d.receivedAmount, d.changeAmount, d.reference, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Expense_Audit ON dbo.Expense
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.method, i.amount, i.category, i.description, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.method, d.amount, d.category, d.description, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_AdmissionRevision_Audit ON dbo.AdmissionRevision
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.previousAdults, i.previousChildren, i.previousSeniors, i.previousVehicles, i.adults, i.children, i.seniors, i.vehicles, i.difference, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.previousAdults, d.previousChildren, d.previousSeniors, d.previousVehicles, d.adults, d.children, d.seniors, d.vehicles, d.difference, d.reason, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Refund_Audit ON dbo.Refund
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.admissionId, i.paymentId, i.revisionId, i.cashSessionId, i.method, i.amount, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.admissionId, d.paymentId, d.revisionId, d.cashSessionId, d.method, d.amount, d.reason, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Adjustment_Audit ON dbo.Adjustment
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.reason, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.reason, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_CashClosure_Audit ON dbo.CashClosure
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.countedCash, i.expectedCash, i.difference, i.operatingResult, i.note, i.snapshot, i.recipient, i.locale, i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.countedCash, d.expectedCash, d.difference, d.operatingResult, d.note, d.snapshot, d.recipient, d.locale, d.actorId, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_Task_Audit ON dbo.Task
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.taskDate, i.title, i.description, i.status, i.authorId, i.updatedBy, i.updatedAt, i.createdAt EXCEPT SELECT d.id, d.taskDate, d.title, d.description, d.status, d.authorId, d.updatedBy, d.updatedAt, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_EmailOutbox_Audit ON dbo.EmailOutbox
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.eventType, i.eventId, i.eventVersion, i.recipient, i.locale, i.payload, i.messageId, i.status, i.attempts, i.availableAt, i.lockedAt, i.sentAt, i.lastErrorCode, i.createdAt EXCEPT SELECT d.id, d.eventType, d.eventId, d.eventVersion, d.recipient, d.locale, d.payload, d.messageId, d.status, d.attempts, d.availableAt, d.lockedAt, d.sentAt, d.lastErrorCode, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_EmailAttempt_Audit ON dbo.EmailAttempt
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
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.outboxId, i.attemptNumber, i.result, i.errorCode, i.createdAt EXCEPT SELECT d.id, d.outboxId, d.attemptNumber, d.result, d.errorCode, d.createdAt);
END;
GO

CREATE TRIGGER dbo.TR_RatePlan_Immutable ON dbo.RatePlan
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_Payment_Immutable ON dbo.Payment
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_Expense_Immutable ON dbo.Expense
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_Refund_Immutable ON dbo.Refund
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_Adjustment_Immutable ON dbo.Adjustment
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_AdmissionRevision_Immutable ON dbo.AdmissionRevision
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_CashClosure_Immutable ON dbo.CashClosure
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_EmailAttempt_Immutable ON dbo.EmailAttempt
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51001, 'Immutable record cannot be changed or deleted', 1;
END;
GO

CREATE TRIGGER dbo.TR_CashSession_Closed ON dbo.CashSession
AFTER UPDATE, DELETE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted WHERE status = 'CLOSED')
        THROW 51002, 'Closed session cannot be changed or deleted', 1;
END;
GO
