package com.tourism.business;
import com.tourism.business.BusinessApplication;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
@SpringBootTest(classes=BusinessApplication.class)
@org.springframework.transaction.annotation.Transactional
class AccessConfigurationTest {
    static final String URL=System.getenv("TEST_DB_URL");
    static final String USER=System.getenv("TEST_DB_USERNAME");
    static final String PASSWORD=System.getenv("TEST_DB_PASSWORD");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        try(var connection=DriverManager.getConnection(URL,USER,PASSWORD)) { assertEquals("TourismIntegrationTest",connection.getCatalog()); }
        properties.add("spring.datasource.url",()->URL); properties.add("spring.datasource.username",()->USER); properties.add("spring.datasource.password",()->PASSWORD);
        properties.add("spring.flyway.url",()->URL); properties.add("spring.flyway.user",()->USER); properties.add("spring.flyway.password",()->PASSWORD);
        properties.add("server.servlet.session.cookie.secure",()->false);
    }
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired org.springframework.security.web.FilterChainProxy security;
    org.springframework.test.web.servlet.MockMvc mvc;
    @org.junit.jupiter.api.BeforeEach
    void prepare() {
        mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        for(String role:new String[]{"ADMIN","STAFF"}) jdbc.update("INSERT dbo.AppUser(username,passwordHash,name,email,phone,role) VALUES(?,?,?,?,?,?)", role.toLowerCase()+".test",new BCryptPasswordEncoder(12).encode("TestPassword!2026"),"Test "+role,role.toLowerCase()+"@example.invalid","00000000",role);
    }
    static final class Browser {
        org.springframework.mock.web.MockHttpSession session;
        String csrf;
    }
    record Reply(int statusCode,String body,java.net.http.HttpHeaders headers) {}
    Reply request(Browser browser,String method,String path,Object body,boolean token) throws Exception {
        var builder=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1"+path).contentType("application/json");
        if(browser.session!=null) builder.session(browser.session);
        if(token && browser.csrf!=null) builder.header("X-CSRF-TOKEN",browser.csrf);
        if(body!=null) builder.content(mapper.writeValueAsString(body));
        var result=mvc.perform(builder).andReturn();
        browser.session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
        var response=result.getResponse();
        var headers=new java.util.HashMap<String,java.util.List<String>>();
        for(String name:response.getHeaderNames()) headers.put(name,response.getHeaders(name));
        return new Reply(response.getStatus(),response.getContentAsString(),java.net.http.HttpHeaders.of(headers,(k,v)->true));
    }
    JsonNode data(Reply response,int status) { assertEquals(status,response.statusCode(),response.body()); return mapper.readTree(response.body()); }
    void csrf(Browser browser) throws Exception { browser.csrf=data(request(browser,"GET","/auth/csrf",null,false),200).get("token").asText(); }
    void login(Browser browser,String name) throws Exception {
        csrf(browser);
        data(request(browser,"POST","/auth/login",Map.of("username",name,"password","TestPassword!2026"),true),200);
        csrf(browser);
    }
    @Test
    void phaseTwoAcceptance() throws Exception {
        Browser anonymous=new Browser();
        data(request(anonymous,"GET","/settings",null,false),401);
        var csrfResponse=request(anonymous,"GET","/auth/csrf",null,false);
        data(csrfResponse,200);
        assertNotNull(anonymous.session);
        data(request(anonymous,"POST","/auth/login",Map.of("username","admin.test","password","TestPassword!2026"),false),403);
        Browser staff=new Browser(); login(staff,"staff.test");
        data(request(staff,"GET","/settings",null,false),403);
        data(request(staff,"PUT","/settings",Map.of(),true),403);
        data(request(staff,"POST","/cash-sessions/1/close",Map.of(),true),403);
        data(request(staff,"GET","/dashboard",null,false),200);
        data(request(staff,"POST","/auth/logout",Map.of(),true),200);
        data(request(staff,"GET","/auth/me",null,false),401);
        Browser admin=new Browser(); csrf(admin);
        String before=admin.session.getId();
        data(request(admin,"POST","/auth/login",Map.of("username","admin.test","password","TestPassword!2026"),true),200);
        assertNotEquals(before,admin.session.getId()); csrf(admin);
        String settings="""
        {"version":"new","defaultCapacity":100,"phone":"00000000","email":"demo@example.invalid","address":"Demo address","sinpeNumber":"00000000","enabled":false,
        "translations":[{"locale":"es","displayName":"Demo park","description":"Description","services":"Trails"}],
        "hours":[{"weekday":1,"opensAt":"08:00","closesAt":"17:00"}],"closedDates":[],"capacityOverrides":[]}
        """;
        var write=mapper.readTree(settings);
        var snapshot=data(request(admin,"PUT","/settings",write,true),200);
        data(request(admin,"PUT","/settings",write,true),409);
        var current=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        current.put("phone","bad"); data(request(admin,"PUT","/settings",current,true),400);
        var prices=Map.of("version",snapshot.get("settings").get("version").asText(),"adultPrice","5000.00","childPrice","2500.00","seniorPrice","3500.00","parkingPrice","1000.00");
        snapshot=data(request(admin,"POST","/rate-plans",prices,true),200);
        String firstRate=snapshot.get("ratePlan").get("id").asText();
        LocalDate today=LocalDate.now(ZoneId.of("America/Costa_Rica"));
        snapshot=data(request(admin,"PUT","/exchange-rates",Map.of("version",snapshot.get("settings").get("version").asText(),"rates",java.util.List.of(Map.of("currency","USD","rate","0.00200000","updatedOn",today.toString()))),true),200);
        assertEquals("0.00200000",snapshot.get("exchangeRates").get(0).get("rate").asText());
        var publicData=data(request(anonymous,"GET","/public/business",null,false),200);
        assertEquals("5000.00",publicData.get("ratePlan").get("adultPrice").asText());
        assertFalse(publicData.has("passwordHash")); assertFalse(publicData.has("version")); assertFalse(publicData.get("reservationsEnabled").asBoolean());
        LocalDate visit=today.plusDays(1);
        jdbc.update("INSERT dbo.VisitDay(visitDate) VALUES(?)",visit);
        long dayId=jdbc.queryForObject("SELECT id FROM dbo.VisitDay WHERE visitDate=?",Long.class,visit);
        jdbc.update("INSERT dbo.Reservation(code,visitDayId,ratePlanId,name,email,phone,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,total,locale,status) VALUES('RES-TEST',?,?,'Test Visitor','visitor@example.invalid','00000000',2,0,0,0,5000,2500,3500,1000,10000,'es','CONFIRMED')",dayId,Long.parseLong(firstRate));
        var operational=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        var hours=operational.putArray("hours");
        hours.addObject().put("weekday",visit.getDayOfWeek().getValue()).put("opensAt","08:00").put("closesAt","17:00");
        snapshot=data(request(admin,"PUT","/settings",operational,true),200);
        var conflicting=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        conflicting.put("defaultCapacity",1);
        var conflict=data(request(admin,"PUT","/settings",conflicting,true),409);
        assertEquals("CONFIGURATION_CONFLICT",conflict.get("code").asText());
        assertTrue(conflict.get("fieldErrors").get("dates").asText().contains(visit.toString()));
        conflicting=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        conflicting.putArray("closedDates").addObject().put("visitDate",visit.toString()).put("reason","Test closure");
        data(request(admin,"PUT","/settings",conflicting,true),409);
        var rescheduled=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        ((tools.jackson.databind.node.ObjectNode)rescheduled.get("hours").get(0)).put("opensAt","09:00");
        int beforeEmails=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox",Integer.class);
        snapshot=data(request(admin,"PUT","/settings",rescheduled,true),200);
        assertEquals(beforeEmails+1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox",Integer.class));
        data(request(admin,"PUT","/settings",rescheduled,true),409);
        assertEquals(beforeEmails+1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.EmailOutbox",Integer.class));
        var overlap=(tools.jackson.databind.node.ObjectNode)snapshot.get("settings").deepCopy();
        ((tools.jackson.databind.node.ArrayNode)overlap.get("hours")).addObject().put("weekday",visit.getDayOfWeek().getValue()).put("opensAt","10:00").put("closesAt","18:00");
        data(request(admin,"PUT","/settings",overlap,true),400);
        data(request(admin,"PUT","/settings",snapshot.get("settings"),false),403);
        data(request(admin,"POST","/rate-plans",Map.of("version",snapshot.get("settings").get("version").asText(),"adultPrice","6000.00","childPrice","2500.00","seniorPrice","3500.00","parkingPrice","1000.00"),true),200);
        assertEquals("5000.00",jdbc.queryForObject("SELECT adultPrice FROM dbo.RatePlan WHERE id=?",java.math.BigDecimal.class,Long.parseLong(firstRate)).toPlainString());
        assertEquals(1800,admin.session.getMaxInactiveInterval());
        for(int attempt=0;attempt<5;attempt++) data(request(admin,"POST","/auth/login",Map.of("username","missing","password","wrong"),true),401);
        var limited=request(admin,"POST","/auth/login",Map.of("username","missing","password","wrong"),true);
        data(limited,429); assertTrue(Integer.parseInt(limited.headers().firstValue("Retry-After").orElseThrow())>0);
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.ActivityEvent WHERE eventType='LOGIN' AND outcome='FAILURE'",Integer.class));
        admin.session.setAttribute("authenticatedAt",System.currentTimeMillis()-43200001L);
        data(request(admin,"GET","/auth/me",null,false),401);
    }
}
