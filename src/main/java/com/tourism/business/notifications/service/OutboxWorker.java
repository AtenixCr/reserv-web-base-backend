package com.tourism.business.notifications.service;
import com.tourism.business.audit.service.AuditContext;
import com.tourism.business.notifications.repository.OutboxRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.UUID;
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="notifications.enabled",havingValue="true")
public class OutboxWorker {
    private final OutboxRepository outbox;
    private final MailTemplates templates;
    private final JavaMailSender sender;
    private final String from;
    private final com.tourism.business.reports.service.ClosureReports reports;
    private final tools.jackson.databind.ObjectMapper mapper;
    public OutboxWorker(OutboxRepository outbox,MailTemplates templates,JavaMailSender sender,com.tourism.business.reports.service.ClosureReports reports,tools.jackson.databind.ObjectMapper mapper,@Value("${SMTP_FROM}") String from) { this.outbox=outbox;this.templates=templates;this.sender=sender;this.from=from;this.reports=reports;this.mapper=mapper; }
    @Scheduled(fixedDelayString="${notifications.poll-millis:5000}")
    public void process() {
        try(var ignored=AuditContext.open(null,UUID.randomUUID())) {
            var job=outbox.claim();if(job==null)return;
            boolean accepted=false;String error=null;
            try {
                var rendered=templates.render(job.type(),job.locale(),job.payload());
                var message=new MimeMessage(sender.createMimeMessage().getSession()) {
                    @Override protected void updateMessageID() throws MessagingException { setHeader("Message-ID","<"+job.messageId()+">"); }
                };
                var helper=new MimeMessageHelper(message,"CASH_CLOSED".equals(job.type()),"UTF-8");helper.setFrom(from);helper.setTo(job.recipient());helper.setSubject(rendered.subject());helper.setText(rendered.text(),false);
                if("CASH_CLOSED".equals(job.type())){error="REPORT_GENERATION_FAILED";long id=mapper.readTree(job.payload()).path("closureId").asLong();helper.addAttachment("closure-"+id+".pdf",new org.springframework.core.io.ByteArrayResource(reports.generate(id,"pdf")),"application/pdf");helper.addAttachment("closure-"+id+".xlsx",new org.springframework.core.io.ByteArrayResource(reports.generate(id,"xlsx")),"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");}
                error="SMTP_SEND_FAILED";sender.send(message);accepted=true;error=null;
            } catch(Exception e) { if(error==null)error="SMTP_SEND_FAILED"; }
            outbox.finish(job,accepted,error);
        }
    }
}
