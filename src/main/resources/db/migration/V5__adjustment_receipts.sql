ALTER TABLE dbo.Adjustment ADD receivedAmount decimal(19,2) NULL, changeAmount decimal(19,2) NULL, reference nvarchar(120) NULL;
GO
ALTER TABLE dbo.Adjustment ADD CONSTRAINT CK_Adjustment_CashReceipt CHECK ((receivedAmount IS NULL AND changeAmount IS NULL) OR (kind='ADDITIONAL_PAYMENT' AND method='CASH' AND receivedAmount IS NOT NULL AND changeAmount IS NOT NULL AND receivedAmount>=amount AND changeAmount=receivedAmount-amount));
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
        CASE WHEN d.id IS NULL THEN NULL ELSE (SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, d.kind, d.method, d.amount, d.receivedAmount, d.changeAmount, d.actorId, d.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END,
        CASE WHEN i.id IS NULL THEN NULL ELSE (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, i.kind, i.method, i.amount, i.receivedAmount, i.changeAmount, i.actorId, i.createdAt FOR JSON PATH, INCLUDE_NULL_VALUES, WITHOUT_ARRAY_WRAPPER) END
    FROM inserted i FULL OUTER JOIN deleted d ON i.id = d.id
    WHERE i.id IS NULL OR d.id IS NULL OR EXISTS (SELECT i.id, i.cashSessionId, i.admissionId, i.revisionId, i.expenseId, i.refundId, i.originalAdjustmentId, CONVERT(varbinary(max), i.kind), CONVERT(varbinary(max), i.method), i.amount, i.receivedAmount, i.changeAmount, CONVERT(varbinary(max), i.reference), CONVERT(varbinary(max), i.reason), i.actorId, i.createdAt EXCEPT SELECT d.id, d.cashSessionId, d.admissionId, d.revisionId, d.expenseId, d.refundId, d.originalAdjustmentId, CONVERT(varbinary(max), d.kind), CONVERT(varbinary(max), d.method), d.amount, d.receivedAmount, d.changeAmount, CONVERT(varbinary(max), d.reference), CONVERT(varbinary(max), d.reason), d.actorId, d.createdAt);
END;
GO
