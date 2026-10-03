package com.tourism.business;
import com.tourism.business.finance.dto.FinanceDto.*;
import com.tourism.business.finance.service.*;
import com.tourism.business.admissions.dto.AdmissionDto;
import com.tourism.business.admissions.service.AdmissionService;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.reports.service.ClosureReports;
import com.tourism.business.notifications.service.*;
import com.tourism.business.notifications.repository.OutboxRepository;
import com.tourism.business.common.ApiException;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class FinanceTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception{PublicReservationTest.database(p);}
    @Autowired JdbcTemplate jdbc;@Autowired FinanceService finance;@Autowired CashSessionService cash;@Autowired AdmissionService admissions;@Autowired AvailabilityService dates;@Autowired ClosureReports reports;@Autowired tools.jackson.databind.ObjectMapper mapper;@Autowired OutboxRepository outbox;@Autowired MailTemplates templates;
    long actor,session;LocalDate today;
    String key(){return UUID.randomUUID().toString();}
    @BeforeEach void setup(){today=dates.today();jdbc.update("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role) VALUES('finance.test','unused','María Test','finance@example.invalid','00000000','ADMIN')");actor=jdbc.queryForObject("SELECT id FROM dbo.AppUser WHERE username='finance.test'",Long.class);jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(1000,'00000000','test@example.invalid','Test','00000000',1)");long settings=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Parque de prueba','Test','Test')",settings);for(int d=1;d<=7;d++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",settings,d);jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");cash.open("20000",actor);session=jdbc.queryForObject("SELECT id FROM dbo.CashSession WHERE businessDate=?",Long.class,today);}
    Map<String,Object> pay(int people,String method){var r=new AdmissionDto.Request(null,null,null,null,null,null,null,people,0,0,0,method,method.equals("CASH")?Integer.toString(people*5000):null,null);var q=admissions.quote(r);return admissions.create(new AdmissionDto.Request(null,null,q.rateVersion(),null,null,null,null,people,0,0,0,method,r.receivedAmount(),null),key(),actor);}
    Close closeRequest(String counted,String note,String locale){var s=(Map<?,?>)finance.summary(today).get("session");return new Close(s.get("version").toString(),counted,note,locale);}
    String pdfText(byte[] bytes)throws Exception{try(var doc=Loader.loadPDF(bytes)){return new PDFTextStripper().getText(doc);}}
    List<List<String>> cells(byte[] bytes)throws Exception{try(var book=new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))){var result=new ArrayList<List<String>>();for(var sheet:book){result.add(List.of(sheet.getSheetName()));for(var row:sheet){var values=new ArrayList<String>();for(var cell:row){assertEquals(org.apache.poi.ss.usermodel.CellType.STRING,cell.getCellType());values.add(cell.getStringCellValue());}result.add(values);}}return result;}}
    @Test void pa12ReconciliationPa14ReportsAndPa13FrozenHistory()throws Exception{
        var first=pay(10,"CASH");pay(6,"SINPE");pay(2,"CARD");long admission=Long.parseLong(first.get("id").toString());finance.expense(new Expense("5000","CASH","Supplies","=Untrusted text remains text"),key(),actor);finance.refund(new Refund(admission,"2000","CASH","Commercial refund"),key(),actor);assertEquals(10,admissions.detail(admission).get("adults"));var summary=(Map<?,?>)finance.summary(today).get("snapshot");var totals=(Map<?,?>)summary.get("totals");assertEquals("63000.00",totals.get("expectedCash"));assertEquals("83000.00",totals.get("operatingResult"));assertEquals("CLOSURE_NOTE_REQUIRED",assertThrows(ApiException.class,()->finance.close(session,closeRequest("62500","","es"),key(),actor)).code);
        var request=closeRequest("62500","Difference counted in local test","es");String key=key();var closed=finance.close(session,request,key,actor);long id=Long.parseLong(closed.get("id").toString());assertEquals("-500.00",closed.get("difference"));assertEquals(mapper.writeValueAsString(closed),mapper.writeValueAsString(finance.close(session,request,key,actor)));assertEquals(closed.get("id"),finance.close(session,request,key(),actor).get("id"));
        byte[] pdf=reports.generate(id,"pdf"),xlsx=reports.generate(id,"xlsx");String text=pdfText(pdf);assertTrue(text.contains("63000.00"));assertTrue(text.contains("83000.00"));assertTrue(text.contains("María Test"));var values=cells(xlsx);assertTrue(values.contains(List.of("Summary")));assertTrue(values.contains(List.of("Attendance")));assertTrue(values.stream().anyMatch(row->row.contains("63000.00")));assertTrue(values.stream().anyMatch(row->row.contains("=Untrusted text remains text")));
        Path dir=Path.of("target/phase-five-evidence");Files.createDirectories(dir);Files.write(dir.resolve("closure.pdf"),pdf);Files.write(dir.resolve("closure.xlsx"),xlsx);try(var doc=Loader.loadPDF(pdf)){assertTrue(doc.getNumberOfPages()>1);javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0,110),"png",dir.resolve("closure-page-1.png").toFile());}
        jdbc.update("UPDATE dbo.BusinessTranslation SET displayName='Changed name'");jdbc.update("UPDATE dbo.AppUser SET name='Changed administrator' WHERE id=?",actor);var current=admissions.detail(admission);admissions.contact(admission,new AdmissionDto.Contact(current.get("version").toString(),"Changed contact",null,null,null));assertEquals(text,pdfText(reports.generate(id,"pdf")));assertEquals(values,cells(reports.generate(id,"xlsx")));assertEquals("CLOSED",((Map<?,?>)finance.summary(today).get("session")).get("status"));assertEquals("CASH_SESSION_EXISTS",assertThrows(ApiException.class,()->cash.open("0",actor)).code);
    }
    @Test void commercialRefundCompensationsAndNetLimit(){var paid=pay(2,"CARD");long id=Long.parseLong(paid.get("id").toString());var expense=finance.expense(new Expense("1000","CASH","Food","Local test"),key(),actor);var compensation=finance.compensate(new Compensation("EXPENSE",Long.parseLong(expense.get("id").toString()),"Wrong expense"),key(),actor);assertEquals("1000.00",compensation.get("amount"));assertEquals("ALREADY_COMPENSATED",assertThrows(ApiException.class,()->finance.compensate(new Compensation("EXPENSE",Long.parseLong(expense.get("id").toString()),"Duplicate"),key(),actor)).code);finance.compensate(new Compensation("ADJUSTMENT",Long.parseLong(compensation.get("id").toString()),"Restore expense"),key(),actor);var again=finance.compensate(new Compensation("EXPENSE",Long.parseLong(expense.get("id").toString()),"Correct again"),key(),actor);assertEquals("1000.00",again.get("amount"));
        var refund=finance.refund(new Refund(id,"7000","SINPE","Commercial concession"),key(),actor);assertEquals(2,admissions.detail(id).get("adults"));assertEquals("3000.00",finance.refundable(id).get("available"));assertEquals("REFUND_EXCEEDS_NET",assertThrows(ApiException.class,()->finance.refund(new Refund(id,"3001","CASH","Too much"),key(),actor)).code);assertEquals("AMOUNT_POSITIVE",assertThrows(ApiException.class,()->finance.refund(new Refund(id,"0","CASH","Zero"),key(),actor)).code);finance.compensate(new Compensation("REFUND",Long.parseLong(refund.get("id").toString()),"Refund entered in error"),key(),actor);assertEquals("10000.00",finance.refundable(id).get("available"));assertEquals("10000.00",jdbc.queryForObject("SELECT amount FROM dbo.Payment WHERE admissionId=?",java.math.BigDecimal.class,id).toPlainString());}
    @Test void failedGenerationAndSmtpKeepClosureAndSingleOutbox(){pay(1,"CARD");var c=finance.close(session,closeRequest("20000",null,"pt"),key(),actor);long id=Long.parseLong(c.get("id").toString());long outboxId=jdbc.queryForObject("SELECT id FROM dbo.EmailOutbox WHERE eventType='CASH_CLOSED' AND eventId=?",Long.class,id);jdbc.update("UPDATE dbo.EmailOutbox SET availableAt='19000101' WHERE id=?",outboxId);var sender=new org.springframework.mail.javamail.JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(1);sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","500");var broken=org.mockito.Mockito.mock(ClosureReports.class);org.mockito.Mockito.when(broken.generate(id,"pdf")).thenThrow(new ApiException(503,"REPORT_GENERATION_FAILED"));new OutboxWorker(outbox,templates,sender,broken,mapper,"test@example.invalid").process();assertEquals("REPORT_GENERATION_FAILED",jdbc.queryForObject("SELECT lastErrorCode FROM dbo.EmailOutbox WHERE id=?",String.class,outboxId));jdbc.update("UPDATE dbo.EmailOutbox SET availableAt='19000101' WHERE id=?",outboxId);new OutboxWorker(outbox,templates,sender,reports,mapper,"test@example.invalid").process();assertEquals("SMTP_SEND_FAILED",jdbc.queryForObject("SELECT lastErrorCode FROM dbo.EmailOutbox WHERE id=?",String.class,outboxId));jdbc.update("UPDATE dbo.EmailOutbox SET status='FAILED' WHERE id=?",outboxId);finance.retryEmail(id);finance.retryEmail(id);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox WHERE eventType='CASH_CLOSED' AND eventId=?",Integer.class,id));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.CashClosure WHERE cashSessionId=?",Integer.class,session));assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,outboxId));}

    @Test void closureMailAttachesFrozenReportsAndStoredRecipient() throws Exception {
        pay(1,"CARD");var closure=finance.close(session,closeRequest("20000",null,"pt"),key(),actor);long id=Long.parseLong(closure.get("id").toString());
        long job=jdbc.queryForObject("SELECT id FROM dbo.EmailOutbox WHERE eventType='CASH_CLOSED' AND eventId=?",Long.class,id);
        jdbc.update("UPDATE dbo.AppUser SET name='Changed',email='changed@example.invalid' WHERE id=?",actor);
        jdbc.update("UPDATE dbo.EmailOutbox SET availableAt='19000101' WHERE id=?",job);
        var sender=new org.springframework.mail.javamail.JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","1000");
        try(var smtp=new PublicReservationTest.LocalSmtp()) {
            sender.setPort(smtp.port());new OutboxWorker(outbox,templates,sender,reports,mapper,"test@example.invalid").process();
            String wire=smtp.message.get(10,java.util.concurrent.TimeUnit.SECONDS);
            var mail=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new java.io.ByteArrayInputStream(wire.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            assertEquals("finance@example.invalid",mail.getAllRecipients()[0].toString());assertTrue(mail.getSubject().startsWith("Fechamento de caixa"));
            assertEquals("<"+jdbc.queryForObject("SELECT messageId FROM dbo.EmailOutbox WHERE id=?",String.class,job)+">",mail.getMessageID());
            var parts=(jakarta.mail.Multipart)mail.getContent();var attachments=new HashMap<String,byte[]>();
            for(int i=0;i<parts.getCount();i++){var part=parts.getBodyPart(i);if(part.getFileName()!=null)attachments.put(part.getFileName(),part.getInputStream().readAllBytes());}
            assertEquals(2,attachments.size());assertEquals(pdfText(reports.generate(id,"pdf")),pdfText(attachments.get("closure-"+id+".pdf")));assertEquals(cells(reports.generate(id,"xlsx")),cells(attachments.get("closure-"+id+".xlsx")));
        }
        assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,job));finance.retryEmail(id);assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,job));
    }
    @Test void changedAttendanceInvalidatesClosurePreviewWithoutMoneyMovement() {
        var paid=pay(1,"CARD");long id=Long.parseLong(paid.get("id").toString());var beforeContact=closeRequest("20000",null,"es");
        var changed=admissions.contact(id,new AdmissionDto.Contact(paid.get("version").toString(),"New name",null,null,null));
        assertEquals("VERSION_CONFLICT",assertThrows(ApiException.class,()->finance.close(session,beforeContact,key(),actor)).code);
        var beforeCorrection=closeRequest("20000",null,"es");admissions.correct(id,new AdmissionDto.Correction(changed.get("version").toString(),0,2,0,0,"Equivalent price",null,null,null),key(),actor);
        assertEquals("VERSION_CONFLICT",assertThrows(ApiException.class,()->finance.close(session,beforeCorrection,key(),actor)).code);
        var closed=finance.close(session,closeRequest("20000",null,"es"),key(),actor);var totals=(Map<?,?>)((Map<?,?>)closed.get("snapshot")).get("totals");assertEquals("5000.00",totals.get("payments"));assertEquals(2,((Number)totals.get("children")).intValue());
    }

    @Test void sqlUtcInstantsRemainUtcRegardlessOfJvmTimezone() {
        var paid=pay(1,"CARD");long admission=Long.parseLong(paid.get("id").toString());
        String sqlPayment=jdbc.queryForObject("SELECT CONVERT(varchar(33),createdAt,126)+'Z' FROM dbo.Payment WHERE admissionId=?",String.class,admission);
        var snapshot=(Map<?,?>)finance.summary(today).get("snapshot");var payment=(Map<?,?>)((List<?>)snapshot.get("payments")).getFirst();assertEquals(Instant.parse(sqlPayment),Instant.parse(payment.get("createdAt").toString()));
        var closed=finance.close(session,closeRequest("20000",null,"es"),key(),actor);long id=Long.parseLong(closed.get("id").toString());String sqlClosure=jdbc.queryForObject("SELECT CONVERT(varchar(33),createdAt,126)+'Z' FROM dbo.CashClosure WHERE id=?",String.class,id);
        assertEquals(Instant.parse(sqlClosure),Instant.parse(((Map<?,?>)closed.get("snapshot")).get("closedAt").toString()));assertEquals(Instant.parse(sqlClosure),Instant.parse(closed.get("createdAt").toString()));
    }
}
