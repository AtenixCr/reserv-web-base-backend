package com.tourism.business;
import com.tourism.business.finance.dto.FinanceDto.*;
import com.tourism.business.finance.service.*;
import com.tourism.business.admissions.dto.AdmissionDto;
import com.tourism.business.admissions.service.AdmissionService;
import com.tourism.business.common.ApiException;
import com.tourism.business.reports.service.ClosureReports;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Import(FinanceConcurrencyTest.TestTime.class)
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class FinanceConcurrencyTest {
    static LocalDate testDate;
    static class MutableClock extends Clock {volatile Instant time;MutableClock(Instant time){this.time=time;}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(time,zone);}public Instant instant(){return time;}}
    @TestConfiguration static class TestTime {@Bean @Primary MutableClock financeClock(){return new MutableClock(testDate.atTime(12,0).toInstant(ZoneOffset.UTC));}}
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception{PublicReservationTest.database(p);try(var c=DriverManager.getConnection(System.getenv("TEST_DB_URL"),System.getenv("TEST_DB_USERNAME"),System.getenv("TEST_DB_PASSWORD"));var s=c.createStatement();var r=s.executeQuery("SELECT DATEADD(day,-3,COALESCE(MIN(businessDate),CONVERT(date,'2000-01-01'))) FROM dbo.CashSession")){r.next();testDate=r.getDate(1).toLocalDate();}}
    @Autowired JdbcTemplate jdbc;@Autowired FinanceService finance;@Autowired CashSessionService cash;@Autowired AdmissionService admissions;@Autowired ClosureReports reports;@Autowired PlatformTransactionManager transactions;@Autowired MutableClock clock;
    long actor,settings;String key(){return UUID.randomUUID().toString();}
    @Test void pa11RefundRacePa22PaymentClosureRaceAndPa13HistoricalCorrection()throws Exception{
        var tx=new TransactionTemplate(transactions);String username="admission.concurrent.finance."+UUID.randomUUID();tx.executeWithoutResult(s->{assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.BusinessSettings",Integer.class));jdbc.update("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role,active) VALUES(?,'unusable','Finance race','test@example.invalid','00000000','ADMIN',0)",username);actor=jdbc.queryForObject("SELECT id FROM dbo.AppUser WHERE username=?",Long.class,username);jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(100,'00000000','test@example.invalid','Test','00000000',1)");settings=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Test','Test','Test')",settings);for(int day=1;day<=7;day++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",settings,day);jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");});
        try{
            cash.open("0",actor);var paid=admissions.create(ready(2),key(),actor);long admission=Long.parseLong(paid.get("id").toString());var refund=new Refund(admission,"7000","CARD","Concurrent commercial refund");var refunds=race(()->finance.refund(refund,key(),actor),()->finance.refund(refund,key(),actor));assertEquals(1,refunds.stream().filter(Map.class::isInstance).count());assertEquals(1,refunds.stream().filter(r->r instanceof ApiException e&&e.code.equals("REFUND_EXCEEDS_NET")).count());assertEquals("3000.00",finance.refundable(admission).get("available"));assertEquals(2,admissions.detail(admission).get("adults"));
            var summary=finance.summary(testDate);var session=(Map<?,?>)summary.get("session");long cashId=Long.parseLong(session.get("id").toString());var close=new Close(session.get("version").toString(),"0",null,"en");var payment=ready(1);var closing=race(()->admissions.create(payment,key(),actor),()->finance.close(cashId,close,key(),actor));
            Map<String,Object> closure;boolean paymentWon=closing.getFirst() instanceof Map;if(paymentWon){assertInstanceOf(ApiException.class,closing.get(1));assertEquals("VERSION_CONFLICT",((ApiException)closing.get(1)).code);var fresh=(Map<?,?>)finance.summary(testDate).get("session");closure=finance.close(cashId,new Close(fresh.get("version").toString(),"0",null,"en"),key(),actor);}else{assertInstanceOf(ApiException.class,closing.getFirst());assertEquals("CASH_SESSION_REQUIRED",((ApiException)closing.getFirst()).code);closure=(Map<String,Object>)closing.get(1);}
            long closureId=Long.parseLong(closure.get("id").toString());var snapshot=(Map<?,?>)closure.get("snapshot");assertEquals(paymentWon?2:1,((List<?>)snapshot.get("payments")).size());assertEquals(paymentWon?"8000.00":"3000.00",((Map<?,?>)snapshot.get("totals")).get("operatingResult"));String text=pdfText(reports.generate(closureId,"pdf"));var excel=cells(reports.generate(closureId,"xlsx"));
            clock.time=testDate.plusDays(1).atTime(12,0).toInstant(ZoneOffset.UTC);cash.open("0",actor);var original=admissions.detail(admission);admissions.correct(admission,new AdmissionDto.Correction(original.get("version").toString(),3,0,0,0,"Historical increase","CARD",null,null),key(),actor);var now=(Map<?,?>)finance.summary(testDate.plusDays(1)).get("snapshot");assertEquals(0L,((Map<?,?>)now.get("totals")).get("adults"));assertEquals("5000.00",((Map<?,?>)now.get("totals")).get("additionalPayments"));assertEquals(text,pdfText(reports.generate(closureId,"pdf")));assertEquals(excel,cells(reports.generate(closureId,"xlsx")));var next=(Map<?,?>)finance.summary(testDate.plusDays(1)).get("session");finance.close(Long.parseLong(next.get("id").toString()),new Close(next.get("version").toString(),"0",null,"pt"),key(),actor);
        }finally{tx.executeWithoutResult(s->{jdbc.update("UPDATE dbo.CashSession SET status='CLOSED' WHERE openedBy=? AND status='OPEN'",actor);jdbc.update("UPDATE dbo.EmailOutbox SET availableAt='20990101' WHERE eventType='CASH_CLOSED' AND eventId IN(SELECT id FROM dbo.CashClosure WHERE actorId=?)",actor);jdbc.update("DELETE dbo.BusinessHour WHERE settingsId=?",settings);jdbc.update("DELETE dbo.BusinessTranslation WHERE settingsId=?",settings);jdbc.update("DELETE dbo.BusinessSettings WHERE id=?",settings);});}
    }
    AdmissionDto.Request ready(int count){var r=new AdmissionDto.Request(null,null,null,null,null,null,null,count,0,0,0,"CARD",null,null);return new AdmissionDto.Request(null,null,admissions.quote(r).rateVersion(),null,null,null,null,count,0,0,0,"CARD",null,null);}
    List<Object> race(Callable<Object> first,Callable<Object> second)throws Exception{var ready=new CountDownLatch(2);var go=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var futures=new ArrayList<Future<Object>>();for(var task:List.of(first,second))futures.add(pool.submit(()->{ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));try{return task.call();}catch(ApiException e){return e;}}));assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));}}
    String pdfText(byte[] bytes)throws Exception{try(var doc=org.apache.pdfbox.Loader.loadPDF(bytes)){return new org.apache.pdfbox.text.PDFTextStripper().getText(doc);}}
    List<String> cells(byte[] bytes)throws Exception{try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))){var values=new ArrayList<String>();for(var sheet:book)for(var row:sheet)for(var cell:row)values.add(cell.getStringCellValue());return values;}}
}
