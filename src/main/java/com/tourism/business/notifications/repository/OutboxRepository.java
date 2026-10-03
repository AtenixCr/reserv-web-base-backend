package com.tourism.business.notifications.repository;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class OutboxRepository {
    public record Job(long id,String type,String recipient,String locale,String payload,String messageId,int attempt) {}
    private final JdbcTemplate jdbc;
    public OutboxRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional
    public Job claim() {
        jdbc.update("INSERT dbo.EmailAttempt(outboxId,attemptNumber,result,errorCode) SELECT o.id,o.attempts,'ABANDONED','LEASE_EXPIRED' FROM dbo.EmailOutbox o WITH(UPDLOCK,HOLDLOCK) WHERE o.status='PROCESSING' AND o.lockedAt<DATEADD(minute,-10,SYSUTCDATETIME()) AND NOT EXISTS(SELECT 1 FROM dbo.EmailAttempt a WHERE a.outboxId=o.id AND a.attemptNumber=o.attempts)");
        jdbc.update("UPDATE dbo.EmailOutbox SET status=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'PENDING' END,availableAt=SYSUTCDATETIME(),lockedAt=NULL,lastErrorCode='LEASE_EXPIRED' WHERE status='PROCESSING' AND lockedAt<DATEADD(minute,-10,SYSUTCDATETIME())");
        var ids=jdbc.queryForList("SELECT TOP(1) id FROM dbo.EmailOutbox WITH(UPDLOCK,READPAST,ROWLOCK) WHERE status='PENDING' AND availableAt<=SYSUTCDATETIME() AND eventType IN ('RESERVATION_CONFIRMED','RESERVATION_CHANGED','RESERVATION_CANCELLED','HOURS_CHANGED','CASH_CLOSED') ORDER BY availableAt,id",Long.class);
        if(ids.isEmpty()) return null;long id=ids.getFirst();
        jdbc.update("UPDATE dbo.EmailOutbox SET status='PROCESSING',lockedAt=SYSUTCDATETIME(),attempts=attempts+1 WHERE id=?",id);
        var job=jdbc.queryForObject("SELECT id,eventType,recipient,locale,payload,messageId,attempts FROM dbo.EmailOutbox WHERE id=?",(r,n)->new Job(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getInt(7)),id);
        return job;
    }
    @Transactional
    public void finish(Job job,boolean accepted,String error) {
        int delay=switch(job.attempt()) { case 1->1;case 2->5;case 3->15;default->60; };
        int count=jdbc.update("UPDATE dbo.EmailOutbox SET status=?,availableAt=DATEADD(minute,?,SYSUTCDATETIME()),lockedAt=NULL,sentAt=CASE WHEN ?=1 THEN SYSUTCDATETIME() ELSE NULL END,lastErrorCode=? WHERE id=? AND status='PROCESSING' AND attempts=?",accepted?"SENT":job.attempt()>=5?"FAILED":"PENDING",delay,accepted?1:0,error,job.id(),job.attempt());
        if(count==0) return;
        jdbc.update("INSERT dbo.EmailAttempt(outboxId,attemptNumber,result,errorCode) VALUES(?,?,?,?)",job.id(),job.attempt(),accepted?"ACCEPTED":"FAILED",error);
        var context=com.tourism.business.audit.service.AuditContext.current();jdbc.update("INSERT dbo.ActivityEvent(eventType,entity,entityId,requestId,outcome) VALUES('EMAIL_RESULT','EmailOutbox',?,?,?)",job.id(),context==null?null:context.requestId().toString(),accepted?"SMTP_ACCEPTED":"FAILED");
    }
}
