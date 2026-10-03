package com.tourism.business;
import com.tourism.business.admissions.dto.AdmissionDto.*;
import com.tourism.business.admissions.service.AdmissionService;
import com.tourism.business.finance.service.CashSessionService;
import com.tourism.business.reservations.service.ReservationService;
import com.tourism.business.reservations.dto.ReservationDto;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.common.ApiException;
import com.tourism.business.auth.repository.UserRepository.Account;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class AdmissionTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry p)throws Exception{PublicReservationTest.database(p);}
    @Autowired JdbcTemplate jdbc;@Autowired AdmissionService admissions;@Autowired CashSessionService cash;@Autowired ReservationService reservations;@Autowired AvailabilityService dates;@Autowired tools.jackson.databind.ObjectMapper mapper;
    @Autowired org.springframework.web.context.WebApplicationContext context;@Autowired org.springframework.security.web.FilterChainProxy security;
    long actor;LocalDate today;
    @BeforeEach void setup(){
        today=dates.today();jdbc.update("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role) VALUES('admission.test','unused','Test','test@example.invalid','00000000','ADMIN')");actor=jdbc.queryForObject("SELECT id FROM dbo.AppUser WHERE username='admission.test'",Long.class);
        jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(100,'00000000','test@example.invalid','Test','00000000',1)");long settings=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);
        jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Test','Test','Test')",settings);for(int d=1;d<=7;d++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",settings,d);
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");
    }
    Request draft(String method,String received){return new Request(null,null,null,null,null,null,null,2,1,1,1,method,received,null);}
    Request priced(Request r){var q=admissions.quote(r);return new Request(r.reservationId(),r.reservationVersion(),q.rateVersion(),r.name(),r.email(),r.phone(),r.note(),r.adults(),r.children(),r.seniors(),r.vehicles(),r.paymentMethod(),r.receivedAmount(),r.reference());}
    @Test void pa06AtomicReplayPa07WalkInCashPa23Free(){
        assertEquals("CASH_SESSION_REQUIRED",assertThrows(ApiException.class,()->admissions.create(priced(draft("CASH","20000")),UUID.randomUUID().toString(),actor)).code);
        cash.open("20000",actor);assertFalse(((Map<?,?>)cash.current(false).get("session")).containsKey("openingFloat"));
        var request=priced(draft("CASH","20000"));String key=UUID.randomUUID().toString();var result=admissions.create(request,key,actor);assertTrue(result.get("code").toString().startsWith("VIS-"));assertEquals(mapper.writeValueAsString(result),mapper.writeValueAsString(admissions.create(request,key,actor)));
        var payment=(Map<?,?>)result.get("payment");assertEquals("17000.00",payment.get("amount"));assertEquals("3000.00",payment.get("changeAmount"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Payment WHERE admissionId=?",Integer.class,Long.valueOf(result.get("id").toString())));
        assertEquals(1L,admissions.list(today,result.get("code").toString(),0,20).get("total"));assertEquals(1L,admissions.list(today,"",0,20).get("total"));
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Admission",Integer.class);assertEquals("INSUFFICIENT_CASH",assertThrows(ApiException.class,()->admissions.create(priced(draft("CASH","100")),UUID.randomUUID().toString(),actor)).code);assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Admission",Integer.class));
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(0,0,0,0,SYSUTCDATETIME())");var free=admissions.create(priced(draft(null,null)),UUID.randomUUID().toString(),actor);assertNull(free.get("payment"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Payment WHERE admissionId=?",Integer.class,Long.valueOf(result.get("id").toString())));assertEquals(8,jdbc.queryForObject("SELECT SUM(adults+children+seniors) FROM dbo.Admission a JOIN dbo.VisitDay d ON d.id=a.visitDayId WHERE d.visitDate=?",Integer.class,today));
    }
    @Test void pa08ReservationUsesHistoricalRatesAndCountsOnce(){
        cash.open("0",actor);var future=new ReservationDto.Request(today.plusDays(1),"Test Visitor","test@example.invalid","00000000",2,0,0,0,"","es",null);var q=reservations.quote(future);var saved=reservations.create(new ReservationDto.Request(future.visitDate(),future.name(),future.email(),future.phone(),2,0,0,0,"","es",q.rateVersion()),UUID.randomUUID().toString());
        long day=dates.lockDay(today);jdbc.update("UPDATE dbo.Reservation SET visitDayId=? WHERE id=?",day,Long.valueOf(saved.id()));jdbc.update("UPDATE dbo.VisitDay SET capacityOverride=2 WHERE id=?",day);
        var current=reservations.detail(Long.parseLong(saved.id()));jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(9000,9000,9000,9000,SYSUTCDATETIME())");
        var request=new Request(Long.valueOf(saved.id()),(String)current.get("version"),null,null,null,null,null,2,0,0,0,"SINPE",null,"Verified outside app");var admitted=admissions.create(request,UUID.randomUUID().toString(),actor);assertEquals("10000.00",((Map<?,?>)admitted.get("payment")).get("amount"));assertEquals("SINPE",((Map<?,?>)admitted.get("payment")).get("method"));assertEquals("CHECKED_IN",reservations.detail(Long.parseLong(saved.id())).get("status"));assertThrows(ApiException.class,()->admissions.create(request,UUID.randomUUID().toString(),actor));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Admission WHERE reservationId=?",Integer.class,Long.valueOf(saved.id())));
        assertEquals("CAPACITY_CONFLICT",assertThrows(ApiException.class,()->admissions.create(priced(new Request(null,null,null,null,null,null,null,1,0,0,0,"CARD",null,null)),UUID.randomUUID().toString(),actor)).code);
    }
    @Test void pa10CorrectionsUseOriginalRatesAndImmutableLinkedMovements(){
        cash.open("0",actor);var paid=admissions.create(priced(draft("CARD",null)),UUID.randomUUID().toString(),actor);long id=Long.parseLong(paid.get("id").toString());
        var corrected=admissions.correct(id,new Correction(paid.get("version").toString(),3,1,1,1,"Additional adult","CASH","6000","receipt"),UUID.randomUUID().toString(),actor);assertEquals("5000.00",corrected.get("difference"));assertEquals("1000.00",corrected.get("changeAmount"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Adjustment WHERE admissionId=? AND kind='ADDITIONAL_PAYMENT'",Integer.class,id));
        var refund=admissions.correct(id,new Correction(corrected.get("version").toString(),1,1,1,1,"Two adults removed","SINPE",null,null),UUID.randomUUID().toString(),actor);assertEquals("-10000.00",refund.get("difference"));assertEquals("10000.00",jdbc.queryForObject("SELECT amount FROM dbo.Refund WHERE admissionId=?",java.math.BigDecimal.class,id).toPlainString());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AdmissionRevision WHERE admissionId=?",Integer.class,id));
        var changed=admissions.contact(id,new Contact(refund.get("version").toString(),"Corrected name","fixed@example.invalid","00000000","Note"));assertEquals("Corrected name",changed.get("name"));assertThrows(ApiException.class,()->admissions.contact(id,new Contact(refund.get("version").toString(),"Old version",null,null,null)));
        assertEquals("17000.00",jdbc.queryForObject("SELECT amount FROM dbo.Payment WHERE admissionId=?",java.math.BigDecimal.class,id).toPlainString());
    }
    @Test void reservationEditKeepsPricesAndQueuesChange(){
        var request=new ReservationDto.Request(today.plusDays(2),"Original Visitor","test@example.invalid","00000000",1,0,0,0,"","es",null);var q=reservations.quote(request);var saved=reservations.create(new ReservationDto.Request(request.visitDate(),request.name(),request.email(),request.phone(),1,0,0,0,"","es",q.rateVersion()),UUID.randomUUID().toString());long id=Long.parseLong(saved.id());assertEquals(1L,reservations.list(request.visitDate(),"",0,20).get("total"));
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(9000,9000,9000,9000,SYSUTCDATETIME())");
        var change=new ReservationDto.Request(today.plusDays(3),"Updated Visitor","test@example.invalid","00000000",2,1,0,0,"Updated note","pt",null);assertEquals("12500.00",reservations.editQuote(id,saved.version(),change).total());var edited=reservations.edit(id,saved.version(),change);assertEquals("12500.00",((ReservationDto.Quote)edited.get("quote")).total());assertEquals(today.plusDays(3).toString(),edited.get("visitDate"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox WHERE eventId=? AND eventType='RESERVATION_CHANGED' AND locale='pt'",Integer.class,id));assertEquals("VERSION_CONFLICT",assertThrows(ApiException.class,()->reservations.edit(id,saved.version(),change)).code);
    }
    @Test void historicalCorrectionUsesOriginalDayAndFreeCorrectionNeedsNoMovement(){
        cash.open("0",actor);var paid=admissions.create(priced(draft("CARD",null)),UUID.randomUUID().toString(),actor);long id=Long.parseLong(paid.get("id").toString());long oldDay=dates.lockDay(today.minusDays(5));jdbc.update("UPDATE dbo.VisitDay SET capacityOverride=4 WHERE id=?",oldDay);jdbc.update("UPDATE dbo.Admission SET visitDayId=? WHERE id=?",oldDay,id);var historical=admissions.detail(id);
        var increase=new Correction(historical.get("version").toString(),3,1,1,1,"Historical correction","CARD",null,null);assertEquals("CAPACITY_CONFLICT",assertThrows(ApiException.class,()->admissions.correct(id,increase,UUID.randomUUID().toString(),actor)).code);jdbc.update("UPDATE dbo.VisitDay SET capacityOverride=5 WHERE id=?",oldDay);var updated=admissions.correct(id,increase,UUID.randomUUID().toString(),actor);assertEquals(today.minusDays(5).toString(),updated.get("visitDate"));assertEquals(0L,admissions.list(today,"",0,20).get("total"));assertEquals(today,jdbc.queryForObject("SELECT c.businessDate FROM dbo.Adjustment a JOIN dbo.CashSession c ON c.id=a.cashSessionId WHERE a.admissionId=?",java.sql.Date.class,id).toLocalDate());
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(0,0,0,0,SYSUTCDATETIME())");var free=admissions.create(priced(draft(null,null)),UUID.randomUUID().toString(),actor);long freeId=Long.parseLong(free.get("id").toString());jdbc.update("UPDATE dbo.CashSession SET status='CLOSED' WHERE businessDate=?",today);var corrected=admissions.correct(freeId,new Correction(free.get("version").toString(),3,1,1,1,"Free attendance correction",null,null,null),UUID.randomUUID().toString(),actor);assertEquals("0.00",corrected.get("difference"));assertNull(corrected.get("payment"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Adjustment WHERE admissionId=?",Integer.class,freeId));
    }
    @Test void openingAndStaffPermissionsAndCsrf()throws Exception{
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var account=new Account(actor,"admission.test","unused","Test","STAFF",true);jdbc.update("UPDATE dbo.AppUser SET role='STAFF' WHERE id=?",actor);var auth=UsernamePasswordAuthenticationToken.authenticated(account,null,account.getAuthorities());var ctx=SecurityContextHolder.createEmptyContext();ctx.setAuthentication(auth);var session=new org.springframework.mock.web.MockHttpSession();session.setAttribute("SPRING_SECURITY_CONTEXT",ctx);session.setAttribute("authenticatedAt",System.currentTimeMillis());
        var csrf=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/csrf").session(session)).andReturn();String token=mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        for(String path:List.of("/cash-sessions","/admissions/1/corrections","/admissions/1/correction-quotes","/expenses","/refunds","/adjustments","/adjustments/quotes","/cash-sessions/1/close","/closures/1/email"))assertEquals(403,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1"+path).session(session).header("X-CSRF-TOKEN",token).contentType("application/json").content("{}")).andReturn().getResponse().getStatus());
        assertEquals(403,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admissions").session(session).contentType("application/json").content("{}")).andReturn().getResponse().getStatus());
        for(String path:List.of("/finance/summary?date="+today,"/finance/admissions/1/refundable","/closures/1","/closures/1/report?format=pdf"))assertEquals(403,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1"+path).session(session)).andReturn().getResponse().getStatus());
        assertEquals(200,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/cash-sessions/current").session(session)).andReturn().getResponse().getStatus());
        jdbc.update("INSERT dbo.CashSession(businessDate,openingFloat,status,openedBy) VALUES(? ,0,'OPEN',?)",today.minusDays(1),actor);assertEquals("CASH_SESSION_EXISTS",assertThrows(ApiException.class,()->cash.open("0",actor)).code);assertEquals("PREVIOUS_SESSION_OPEN",assertThrows(ApiException.class,()->admissions.create(priced(draft("CASH","20000")),UUID.randomUUID().toString(),actor)).code);
    }
}
