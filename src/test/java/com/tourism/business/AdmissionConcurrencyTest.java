package com.tourism.business;
import com.tourism.business.admissions.dto.AdmissionDto.Request;
import com.tourism.business.admissions.service.AdmissionService;
import com.tourism.business.finance.service.CashSessionService;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.common.ApiException;
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
@Import(AdmissionConcurrencyTest.TestTime.class)
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class AdmissionConcurrencyTest {
    static LocalDate testDate;
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception {
        PublicReservationTest.database(p);
        try(var c=DriverManager.getConnection(System.getenv("TEST_DB_URL"),System.getenv("TEST_DB_USERNAME"),System.getenv("TEST_DB_PASSWORD"));var s=c.createStatement();var r=s.executeQuery("SELECT DATEADD(day,-2,COALESCE(MIN(businessDate),CONVERT(date,'2000-01-01'))) FROM dbo.CashSession")){r.next();testDate=r.getDate(1).toLocalDate();}
    }
    @TestConfiguration static class TestTime {@Bean @Primary Clock testClock(){return Clock.fixed(testDate.atTime(12,0).toInstant(ZoneOffset.UTC),ZoneOffset.UTC);}}
    @Autowired JdbcTemplate jdbc;@Autowired AdmissionService admissions;@Autowired CashSessionService cash;@Autowired AvailabilityService dates;@Autowired PlatformTransactionManager transactions;@Autowired tools.jackson.databind.ObjectMapper mapper;
    long actor,settingsId,day;
    @Test void pa06ConcurrentReplayLastPlaceAndMidPaymentRollback()throws Exception {
        var tx=new TransactionTemplate(transactions);String username="admission.concurrent."+UUID.randomUUID();
        tx.executeWithoutResult(s->{
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.BusinessSettings",Integer.class));
            jdbc.update("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role,active) VALUES(?,'unusable','Concurrency test','test@example.invalid','00000000','ADMIN',0)",username);actor=jdbc.queryForObject("SELECT id FROM dbo.AppUser WHERE username=?",Long.class,username);
            jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(1,'00000000','test@example.invalid','Test','00000000',1)");settingsId=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);
            jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Test','Test','Test')",settingsId);
            for(int d=1;d<=7;d++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",settingsId,d);
            jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");day=dates.lockDay(testDate);cash.open("0",actor);
        });
        try {
            var request=ready();var race=race(()->admissions.create(request,UUID.randomUUID().toString(),actor),()->admissions.create(request,UUID.randomUUID().toString(),actor));
            assertEquals(1,race.stream().filter(Map.class::isInstance).count());assertEquals(1,race.stream().filter(r->r instanceof ApiException e&&e.code.equals("CAPACITY_CONFLICT")).count());
            jdbc.update("UPDATE dbo.BusinessSettings SET defaultCapacity=10 WHERE id=?",settingsId);
            String key=UUID.randomUUID().toString();var replay=race(()->admissions.create(request,key,actor),()->admissions.create(request,key,actor));assertInstanceOf(Map.class,replay.get(0));assertEquals(mapper.writeValueAsString(replay.get(0)),mapper.writeValueAsString(replay.get(1)));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Payment p JOIN dbo.Admission a ON a.id=p.admissionId WHERE a.visitDayId=?",Integer.class,day));
            String trigger="TR_TestPaymentFailure_"+UUID.randomUUID().toString().replace("-","");String failedKey=UUID.randomUUID().toString();
            jdbc.execute("CREATE TRIGGER dbo."+trigger+" ON dbo.Payment AFTER INSERT AS BEGIN THROW 51099,'Simulated payment persistence failure',1; END");
            try {assertThrows(org.springframework.dao.DataAccessException.class,()->admissions.create(request,failedKey,actor));}finally{jdbc.execute("DROP TRIGGER dbo."+trigger);}
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Admission WHERE visitDayId=?",Integer.class,day));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Payment p JOIN dbo.Admission a ON a.id=p.admissionId WHERE a.visitDayId=?",Integer.class,day));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.IdempotencyRecord WHERE operation=?",Integer.class,"ADMISSION:"+actor));
            assertNotNull(admissions.create(request,failedKey,actor).get("id"));
        } finally {
            tx.executeWithoutResult(s->{jdbc.update("UPDATE dbo.CashSession SET status='CLOSED' WHERE businessDate=? AND openedBy=?",testDate,actor);jdbc.update("DELETE dbo.BusinessHour WHERE settingsId=?",settingsId);jdbc.update("DELETE dbo.BusinessTranslation WHERE settingsId=?",settingsId);jdbc.update("DELETE dbo.BusinessSettings WHERE id=?",settingsId);});
        }
    }
    Request ready(){var initial=new Request(null,null,null,null,null,null,null,1,0,0,0,"CARD",null,null);return new Request(null,null,admissions.quote(initial).rateVersion(),null,null,null,null,1,0,0,0,"CARD",null,null);}
    List<Object> race(Callable<Object> first,Callable<Object> second)throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){var futures=new ArrayList<Future<Object>>();for(var call:List.of(first,second))futures.add(pool.submit(()->{ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));try{return call.call();}catch(ApiException e){return e;}}));assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));}
    }
}
