SET XACT_ABORT ON;
DECLARE @outboxId bigint = $(OutboxId);
IF @outboxId <= 0 THROW 51010, 'Invalid outbox identifier', 1;
BEGIN TRANSACTION;
UPDATE dbo.EmailOutbox WITH(UPDLOCK)
SET status='PENDING', availableAt=SYSUTCDATETIME(), lockedAt=NULL
WHERE id=@outboxId AND status='FAILED';
IF @@ROWCOUNT <> 1
BEGIN
    ROLLBACK;
    THROW 51011, 'Expected one failed email', 1;
END;
INSERT dbo.ActivityEvent(eventType,entity,entityId,outcome)
VALUES('EMAIL_MANUAL_RETRY','EmailOutbox',@outboxId,'QUEUED');
COMMIT;
SELECT id,status,attempts FROM dbo.EmailOutbox WHERE id=@outboxId;
