package com.tourism.business;
import com.tourism.business.reservations.dto.ReservationDto.*;
import com.tourism.business.reservations.service.ReservationService;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.notifications.repository.OutboxRepository;
import com.tourism.business.notifications.service.*;
import com.tourism.business.common.ApiException;
import java.time.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class PublicReservationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) throws Exception {
        try(var c=DriverManager.getConnection(System.getenv("TEST_DB_URL"),System.getenv("TEST_DB_USERNAME"),System.getenv("TEST_DB_PASSWORD"))) { assertEquals("TourismIntegrationTest",c.getCatalog()); }
        for(String prefix:List.of("spring.datasource.","spring.flyway.")) { p.add(prefix+"url",()->System.getenv("TEST_DB_URL"));p.add(prefix+(prefix.contains("flyway")?"user":"username"),()->System.getenv("TEST_DB_USERNAME"));p.add(prefix+"password",()->System.getenv("TEST_DB_PASSWORD")); }
        p.add("notifications.enabled",()->false);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationService reservations;
    @Autowired AvailabilityService availability;
    @Autowired OutboxRepository outbox;
    @Autowired MailTemplates templates;
    @Autowired com.tourism.business.reports.service.ClosureReports reports;
    @Autowired Clock clock;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired org.springframework.security.web.FilterChainProxy security;
    LocalDate today;
    @BeforeEach void setup() {
        today=availability.today();
        jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(20,'00000000','park@example.invalid','Test','00000000',1)");
        long id=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);
        jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,'es','Test park','Test','Test')",id);
        for(int d=1;d<=7;d++)jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,'08:00','17:00')",id,d);
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(5000,2500,3500,1000,SYSUTCDATETIME())");
    }
    Request request(LocalDate date,int adults,int children,int seniors,int vehicles,String rate) { return new Request(date," Test Visitor ","visitor@example.invalid","+506 0000-0000",adults,children,seniors,vehicles,"<plain text>","pt",rate); }
    Request ready(LocalDate date) { var q=reservations.quote(request(date,2,1,1,1,null));return request(date,2,1,1,1,q.rateVersion()); }
    @Test void pa01DatesAndPa02ExactPricesAndValidation() {
        for(int offset:new int[]{0,365})assertEquals("DATE_OUT_OF_RANGE",assertThrows(ApiException.class,()->reservations.quote(request(today.plusDays(offset),1,0,0,0,null))).code);
        for(int offset:new int[]{1,364})assertEquals("17000.00",reservations.quote(request(today.plusDays(offset),2,1,1,1,null)).total());
        assertThrows(ApiException.class,()->reservations.quote(request(today.plusDays(1),0,0,0,1,null)));
        assertThrows(ApiException.class,()->reservations.quote(request(today.plusDays(1),-1,0,0,0,null)));
        assertThrows(ApiException.class,()->reservations.quote(new Request(today.plusDays(1),"A","bad","12",1,0,0,0,"","es",null)));
        var month=availability.month(java.time.YearMonth.from(today).toString());assertEquals(today,month.serverDate());
        assertEquals("OUT_OF_RANGE",month.days().stream().filter(d->d.visitDate().equals(today)).findFirst().orElseThrow().reason());
    }
    @Test void pa04TransitionPa05TariffAndIdempotency() {
        var request=ready(today.plusDays(2));String key=UUID.randomUUID().toString();var saved=reservations.create(request,key);
        assertEquals(saved,reservations.create(request,key));
        assertEquals("Test Visitor",saved.name());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox WHERE eventType='RESERVATION_CONFIRMED' AND eventId=?",Integer.class,Long.valueOf(saved.id())));
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(ApiException.class,()->reservations.create(request(today.plusDays(2),1,0,0,0,request.rateVersion()),key)).code);
        jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(6000,2500,3500,1000,SYSUTCDATETIME())");
        assertEquals(saved,reservations.create(request,key));
        assertEquals("RATE_CHANGED",assertThrows(ApiException.class,()->reservations.create(request,UUID.randomUUID().toString())).code);
        assertEquals("17000.00",jdbc.queryForObject("SELECT total FROM dbo.Reservation WHERE id=?",java.math.BigDecimal.class,Long.valueOf(saved.id())).toPlainString());
        assertEquals("NO_SHOW_TOO_EARLY",assertThrows(ApiException.class,()->reservations.transition(Long.parseLong(saved.id()),new Transition(saved.version(),"Test"),true)).code);
        var state=reservations.transition(Long.parseLong(saved.id()),new Transition(saved.version(),"Test cancellation"),false);assertEquals("CANCELLED",state.get("status"));
        assertThrows(ApiException.class,()->reservations.transition(Long.parseLong(saved.id()),new Transition(saved.version(),"Again"),false));
        assertEquals(20,availability.month(YearMonth.from(request.visitDate()).toString()).days().stream().filter(d->d.visitDate().equals(request.visitDate())).findFirst().orElseThrow().available());
    }
    @Test void pa15SmtpFailureRetriesRecoveryAndAcceptedMessage() throws Exception {
        var saved=reservations.create(ready(today.plusDays(3)),UUID.randomUUID().toString());
        var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(1);sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","1000");
        var worker=new OutboxWorker(outbox,templates,sender,reports,mapper,"test@example.invalid");worker.process();
        long id=jdbc.queryForObject("SELECT id FROM dbo.EmailOutbox WHERE eventId=? AND eventType='RESERVATION_CONFIRMED'",Long.class,Long.valueOf(saved.id()));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Reservation WHERE id=?",Integer.class,Long.valueOf(saved.id())));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailAttempt WHERE outboxId=?",Integer.class,id));
        assertTrue(jdbc.queryForObject("SELECT DATEDIFF(second,SYSUTCDATETIME(),availableAt) FROM dbo.EmailOutbox WHERE id=?",Integer.class,id)>50);
        jdbc.update("UPDATE dbo.EmailOutbox SET availableAt=SYSUTCDATETIME() WHERE id=?",id);
        var lease=outbox.claim();assertNotNull(lease);assertNull(outbox.claim());
        jdbc.update("UPDATE dbo.EmailOutbox SET lockedAt=DATEADD(minute,-11,SYSUTCDATETIME()) WHERE id=?",id);
        var recovered=outbox.claim();assertEquals(3,recovered.attempt());outbox.finish(lease,true,null);
        assertEquals("PROCESSING",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,id));
        outbox.finish(recovered,false,"SMTP_SEND_FAILED");
        jdbc.update("UPDATE dbo.EmailOutbox SET availableAt=SYSUTCDATETIME() WHERE id=?",id);
        try(var smtp=new LocalSmtp()) {
            sender.setPort(smtp.port());worker.process();String wire=smtp.message.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(wire.contains("Message-ID: <"+recovered.messageId()+">"));assertTrue(wire.contains("visitor@example.invalid"));
            assertEquals("SENT",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,id));
            assertNull(outbox.claim());
        }
        var rendered=templates.render("RESERVATION_CONFIRMED","pt",mapper.writeValueAsString(saved));assertTrue(rendered.text().contains("Obrigado"));assertTrue(rendered.text().contains("17000.00 CRC"));
        assertTrue(templates.render("RESERVATION_CONFIRMED","en",mapper.writeValueAsString(saved)).text().contains("Thank you"));
        assertTrue(templates.render("RESERVATION_CONFIRMED","es",mapper.writeValueAsString(saved)).text().contains("Gracias"));
        var next=reservations.create(ready(today.plusDays(5)),UUID.randomUUID().toString());
        long failedId=jdbc.queryForObject("SELECT id FROM dbo.EmailOutbox WHERE eventId=? AND eventType='RESERVATION_CONFIRMED'",Long.class,Long.valueOf(next.id()));
        sender.setPort(1);
        for(int attempt=1;attempt<=5;attempt++) {
            worker.process();assertEquals(attempt,jdbc.queryForObject("SELECT attempts FROM dbo.EmailOutbox WHERE id=?",Integer.class,failedId));
            if(attempt<5) { int minutes=new int[]{1,5,15,60}[attempt-1];int seconds=jdbc.queryForObject("SELECT DATEDIFF(second,SYSUTCDATETIME(),availableAt) FROM dbo.EmailOutbox WHERE id=?",Integer.class,failedId);assertTrue(seconds>minutes*60-10 && seconds<=minutes*60);jdbc.update("UPDATE dbo.EmailOutbox SET availableAt=SYSUTCDATETIME() WHERE id=?",failedId); }
        }
        assertEquals("FAILED",jdbc.queryForObject("SELECT status FROM dbo.EmailOutbox WHERE id=?",String.class,failedId));
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailAttempt WHERE outboxId=?",Integer.class,failedId));
        assertNull(outbox.claim());
    }
    @Test void pa24PublicRateLimitAndSafeApiErrors() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var request=ready(today.plusDays(4));String key=UUID.randomUUID().toString();
        String fractional=mapper.writeValueAsString(request).replace("\"adults\":2","\"adults\":2.5");
        assertEquals(400,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/public/reservation-quotes").contentType("application/json").content(fractional)).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/public/availability")).andReturn().getResponse().getStatus());
        for(int i=0;i<10;i++) {
            var result=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/public/reservations").with(r->{r.setRemoteAddr("127.0.0.77");return r;}).header("Idempotency-Key",key).contentType("application/json").content(mapper.writeValueAsString(request))).andReturn();assertEquals(201,result.getResponse().getStatus(),result.getResponse().getContentAsString());
        }
        var limited=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/public/reservations").with(r->{r.setRemoteAddr("127.0.0.77");return r;}).header("Idempotency-Key",key).contentType("application/json").content(mapper.writeValueAsString(request))).andReturn();
        assertEquals(429,limited.getResponse().getStatus());assertNotNull(limited.getResponse().getHeader("Retry-After"));assertFalse(limited.getResponse().getContentAsString().contains("visitor@example.invalid"));
        assertEquals(401,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/reservations")).andReturn().getResponse().getStatus());
    }
    static class LocalSmtp implements AutoCloseable {
        final ServerSocket server; final java.util.concurrent.CompletableFuture<String> message=new java.util.concurrent.CompletableFuture<>();final Thread thread;
        LocalSmtp() throws IOException {
            server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
            thread=Thread.ofPlatform().start(()->{try(var socket=server.accept();var input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));var output=new PrintWriter(socket.getOutputStream(),true,StandardCharsets.UTF_8)) {
                output.print("220 localhost test\r\n");output.flush();String line;boolean data=false;var content=new StringBuilder();
                while((line=input.readLine())!=null) { if(data) { if(line.equals(".")) { data=false;message.complete(content.toString());output.print("250 accepted\r\n"); }else content.append(line).append('\n'); }
                    else if(line.startsWith("DATA")) { data=true;output.print("354 send\r\n"); }
                    else if(line.startsWith("QUIT")) { output.print("221 bye\r\n");output.flush();break; }
                    else output.print("250 OK\r\n");output.flush(); }
            }catch(Exception e){message.completeExceptionally(e);}});
        }
        int port(){return server.getLocalPort();}
        public void close() throws Exception { server.close();thread.join(2000); }
    }
}
