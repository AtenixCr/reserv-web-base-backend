package com.tourism.business;
import com.tourism.business.reservations.dto.ReservationDto.*;
import com.tourism.business.reservations.service.ReservationService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class ReservationConcurrencyTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) throws Exception { PublicReservationTest.database(p); }
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationService reservations;
    @Autowired AvailabilityService availability;
    @Autowired PlatformTransactionManager transactions;
    long settingsId;
    final List<Long> dayIds=new ArrayList<>();
    @Test void pa03SimultaneousLastPlaceAndDuplicateKeyUseRealCommits() throws Exception {
        var tx=new TransactionTemplate(transactions);LocalDate day=availability.today().plusDays(120);
        tx.executeWithoutResult(s->{
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.BusinessSettings",Integer.class));
            jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(20,'00000000','test@example.invalid','Test','00000000',1)");
            settingsId=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);
            jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Concurrency test','Test','Test')",settingsId);
            for(int d=1;d<=7;d++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",settingsId,d);
            jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");
            for(int offset=0;offset<2;offset++) { jdbc.update("INSERT dbo.VisitDay(visitDate,capacityOverride) VALUES(?,?)",day.plusDays(offset),offset==0?1:20);dayIds.add(jdbc.queryForObject("SELECT id FROM dbo.VisitDay WHERE visitDate=?",Long.class,day.plusDays(offset))); }
        });
        try {
            var first=ready(day);var result=race(()->reservations.create(first,UUID.randomUUID().toString()),()->reservations.create(first,UUID.randomUUID().toString()));
            assertEquals(1,result.stream().filter(r->r instanceof Confirmation).count());assertEquals(1,result.stream().filter(r->r instanceof ApiException a && a.status==409).count());
            assertEquals(1,jdbc.queryForObject("SELECT SUM(adults+children+seniors) FROM dbo.Reservation WHERE visitDayId=? AND status='CONFIRMED'",Integer.class,dayIds.getFirst()));
            String key=UUID.randomUUID().toString();var second=ready(day.plusDays(1));var replay=race(()->reservations.create(second,key),()->reservations.create(second,key));
            assertInstanceOf(Confirmation.class,replay.get(0));assertEquals(replay.get(0),replay.get(1));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Reservation WHERE visitDayId=?",Integer.class,dayIds.get(1)));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox WHERE eventType='RESERVATION_CONFIRMED' AND eventId IN (SELECT id FROM dbo.Reservation WHERE visitDayId IN (?,?))",Integer.class,dayIds.get(0),dayIds.get(1)));
        } finally { cleanup(); }
    }
    Request ready(LocalDate date) { var request=new Request(date,"Concurrency Visitor","concurrency@example.invalid","00000000",1,0,0,0,"","es",null);var quote=reservations.quote(request);return new Request(date,request.name(),request.email(),request.phone(),1,0,0,0,"","es",quote.rateVersion()); }
    List<Object> race(Callable<Object> first,Callable<Object> second) throws Exception {
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Object>>();
            for(var call:List.of(first,second))futures.add(pool.submit(()->{ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));try{return call.call();}catch(ApiException e){return e;}}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();return List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));
        }
    }
    void cleanup() {
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            for(long day:dayIds) {
                for(long id:jdbc.queryForList("SELECT id FROM dbo.Reservation WHERE visitDayId=?",Long.class,day)) {
                    jdbc.update("DELETE dbo.IdempotencyRecord WHERE operation='CREATE_RESERVATION' AND JSON_VALUE(responseBody,'$.id')=?",Long.toString(id));
                    jdbc.update("DELETE dbo.EmailOutbox WHERE eventType='RESERVATION_CONFIRMED' AND eventId=?",id);
                    jdbc.update("DELETE dbo.Reservation WHERE id=?",id);
                }
                jdbc.update("DELETE dbo.VisitDay WHERE id=?",day);
            }
            jdbc.update("DELETE dbo.BusinessHour WHERE settingsId=?",settingsId);jdbc.update("DELETE dbo.BusinessTranslation WHERE settingsId=?",settingsId);jdbc.update("DELETE dbo.BusinessSettings WHERE id=?",settingsId);
        });
    }
}
