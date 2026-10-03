CREATE INDEX IX_ActivityEvent_Request ON dbo.ActivityEvent(requestId);
CREATE INDEX IX_ActivityEvent_Actor ON dbo.ActivityEvent(actorId,occurredAt DESC);
CREATE INDEX IX_ActivityEvent_Entity ON dbo.ActivityEvent(entity,occurredAt DESC);
GO
