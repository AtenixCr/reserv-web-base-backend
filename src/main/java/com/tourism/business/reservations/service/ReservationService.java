package com.tourism.business.reservations.service;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.business.dto.SettingsDto.*;
import com.tourism.business.business.repository.SettingsRepository;
import com.tourism.business.common.ApiException;
import com.tourism.business.reservations.dto.ReservationDto.*;
import jakarta.validation.Validator;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
@Service
public class ReservationService {
    private final JdbcTemplate jdbc;
    private final SettingsRepository settings;
    private final AvailabilityService availability;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final Clock clock;
    public ReservationService(JdbcTemplate jdbc,SettingsRepository settings,AvailabilityService availability,ObjectMapper mapper,Validator validator,Clock clock) { this.jdbc=jdbc;this.settings=settings;this.availability=availability;this.mapper=mapper;this.validator=validator;this.clock=clock; }
    private void validate(Request r) {
        var fields=new LinkedHashMap<String,String>();
        validator.validate(r).forEach(v->fields.put(v.getPropertyPath().toString(),"INVALID_FIELD"));
        if(!fields.isEmpty()) throw new ApiException(400,"INVALID_INPUT",fields);
        if(r.people()<1) throw new ApiException(400,"INVALID_INPUT",Map.of("adults","PEOPLE_REQUIRED"));
        if(!r.phone().matches("\\+?[0-9 ()-]+") || r.phone().replaceAll("\\D", "").length()<7 || r.phone().replaceAll("\\D", "").length()>15) throw new ApiException(400,"INVALID_INPUT",Map.of("phone","INVALID_FIELD"));
        if(r.email().contains("\r") || r.email().contains("\n")) throw new ApiException(400,"INVALID_INPUT",Map.of("email","INVALID_FIELD"));
    }
    public Quote price(Request r,Rate rate,List<Hour> hours) {
        if(rate==null) throw new ApiException(409,"DATE_UNAVAILABLE");
        var categories=List.of("adults","children","seniors","vehicles");
        var prices=List.of(rate.adultPrice(),rate.childPrice(),rate.seniorPrice(),rate.parkingPrice());
        var counts=List.of(r.adults(),r.children(),r.seniors(),r.vehicles());
        var lines=new ArrayList<Line>(); var total=BigDecimal.ZERO;
        for(int i=0;i<4;i++) { var subtotal=new BigDecimal(prices.get(i)).multiply(BigDecimal.valueOf(counts.get(i))).setScale(2,RoundingMode.HALF_UP); total=total.add(subtotal);lines.add(new Line(categories.get(i),counts.get(i),prices.get(i),subtotal.toPlainString())); }
        if(total.compareTo(new BigDecimal("99999999999999999.99"))>0) throw new ApiException(400,"INVALID_INPUT",Map.of("vehicles","AMOUNT_TOO_LARGE"));
        return new Quote(r.visitDate(),rate.id(),"CRC",lines,total.setScale(2).toPlainString(),hours);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Quote quote(Request r) { validate(r);var config=settings.read(false,availability.today());var day=availability.require(config,r.visitDate(),r.people());return price(r,settings.rate(),day.hours()); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Confirmation create(Request r,String key) {
        if(key==null || !key.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")) throw new ApiException(400,"IDEMPOTENCY_REQUIRED");
        validate(r);
        var config=settings.read(true,availability.today());
        String keyHash=hash(key.toLowerCase(Locale.ROOT)),requestHash=hash(mapper.writeValueAsString(r));
        var existing=jdbc.queryForList("SELECT requestHash,responseBody FROM dbo.IdempotencyRecord WITH(UPDLOCK,HOLDLOCK) WHERE operation='CREATE_RESERVATION' AND keyHash=?",keyHash);
        if(!existing.isEmpty()) {
            if(!existing.getFirst().get("requestHash").equals(requestHash)) throw new ApiException(409,"IDEMPOTENCY_CONFLICT");
            return mapper.readValue((String)existing.getFirst().get("responseBody"),Confirmation.class);
        }
        long dayId=availability.lockDay(r.visitDate());
        var day=availability.require(config,r.visitDate(),r.people()); var rate=settings.rate();
        if(rate==null || !Objects.equals(rate.id(),r.rateVersion())) throw new ApiException(409,"RATE_CHANGED");
        var quote=price(r,rate,day.hours());
        String code="RES-"+UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        jdbc.update("INSERT dbo.Reservation(code,visitDayId,ratePlanId,name,email,phone,note,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,total,locale,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'CONFIRMED')",code,dayId,Long.valueOf(rate.id()),r.name(),r.email(),r.phone(),r.note(),r.adults(),r.children(),r.seniors(),r.vehicles(),new BigDecimal(rate.adultPrice()),new BigDecimal(rate.childPrice()),new BigDecimal(rate.seniorPrice()),new BigDecimal(rate.parkingPrice()),new BigDecimal(quote.total()),r.locale());
        var row=jdbc.queryForMap("SELECT id,version FROM dbo.Reservation WHERE code=?",code);long id=((Number)row.get("id")).longValue();
        var response=new Confirmation(Long.toString(id),code,"CONFIRMED",Base64.getEncoder().encodeToString((byte[])row.get("version")),r.name(),r.locale(),quote,config.sinpeNumber(),businessName(config,r.locale()),config.phone(),config.email(),"PENDING");
        enqueue(id,"RESERVATION_CONFIRMED",r.email(),r.locale(),response);
        jdbc.update("INSERT dbo.IdempotencyRecord(operation,keyHash,requestHash,responseStatus,responseBody,expiresAt) VALUES('CREATE_RESERVATION',?,?,201,?,DATEADD(hour,49,SYSUTCDATETIME()))",keyHash,requestHash,mapper.writeValueAsString(response));
        return response;
    }
    private String businessName(Settings config,String locale) { return config.translations().stream().filter(t->t.locale().equals(locale)).map(Translation::displayName).filter(s->!s.isBlank()).findFirst().orElseGet(()->config.translations().stream().filter(t->t.locale().equals("es")).map(Translation::displayName).findFirst().orElse("")); }
    private void enqueue(long id,String type,String recipient,String locale,Object payload) {
        int version=jdbc.queryForObject("SELECT COALESCE(MAX(eventVersion),0)+1 FROM dbo.EmailOutbox WITH(UPDLOCK,HOLDLOCK) WHERE eventType=? AND eventId=?",Integer.class,type,id);
        jdbc.update("INSERT dbo.EmailOutbox(eventType,eventId,eventVersion,recipient,locale,payload,messageId) VALUES(?,?,?,?,?,?,?)",type,id,version,recipient,locale,mapper.writeValueAsString(payload),UUID.randomUUID()+"@tourism.local");
    }
    private Map<String,Object> internalView(Map<String,Object> r) {
        var view=new LinkedHashMap<String,Object>();for(String k:List.of("code","name","email","phone","note","adults","children","seniors","vehicles","locale","status"))view.put(k,r.get(k));view.put("id",r.get("id").toString());view.put("visitDate",r.get("visitDate").toString());view.put("version",Base64.getEncoder().encodeToString((byte[])r.get("version")));
        var rate=new Rate(r.get("ratePlanId").toString(),r.get("adultPrice").toString(),r.get("childPrice").toString(),r.get("seniorPrice").toString(),r.get("parkingPrice").toString());
        var req=new Request(LocalDate.parse(r.get("visitDate").toString()),(String)r.get("name"),(String)r.get("email"),(String)r.get("phone"),(Integer)r.get("adults"),(Integer)r.get("children"),(Integer)r.get("seniors"),(Integer)r.get("vehicles"),(String)r.get("note"),(String)r.get("locale"),rate.id());view.put("quote",price(req,rate,List.of()));return view;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> detail(long id) {var rows=jdbc.queryForList("SELECT r.*,d.visitDate FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE r.id=?",id);if(rows.isEmpty())throw new ApiException(404,"NOT_FOUND");return internalView(rows.getFirst());}
    @Transactional(readOnly=true)
    public Map<String,Object> list(LocalDate date,String search,int page,int size) {
        if(page<0||size<1||size>100||search.length()>120)throw new ApiException(400,"INVALID_INPUT");String filter=" FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE d.visitDate=? AND (?='' OR CHARINDEX(?,r.name)>0 OR CHARINDEX(?,r.code)>0)";
        return Map.of("items",jdbc.queryForList("SELECT r.*,d.visitDate"+filter+" ORDER BY r.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",date,search,search,search,(long)page*size,size).stream().map(this::internalView).toList(),"page",page,"size",size,"total",jdbc.queryForObject("SELECT COUNT(*)"+filter,Long.class,date,search,search,search));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Quote editQuote(long id,String version,Request request) {
        validate(request);var previous=detail(id);if(!Objects.equals(previous.get("version"),version))throw new ApiException(409,"VERSION_CONFLICT");if(!"CONFIRMED".equals(previous.get("status")))throw new ApiException(409,"RESERVATION_STATE_CONFLICT");
        var config=settings.read(false,availability.today());var day=availability.day(config,request.visitDate());if(day.reason()!=null&&!"FULL".equals(day.reason()))throw new ApiException("OUT_OF_RANGE".equals(day.reason())?400:409,"OUT_OF_RANGE".equals(day.reason())?"DATE_OUT_OF_RANGE":"DATE_UNAVAILABLE");
        long allowance=day.available()+(previous.get("visitDate").equals(request.visitDate().toString())?(long)(Integer)previous.get("adults")+(Integer)previous.get("children")+(Integer)previous.get("seniors"):0);if(request.people()>allowance)throw new ApiException(409,"CAPACITY_CONFLICT");
        var original=(Quote)previous.get("quote");return price(request,new Rate(original.rateVersion(),original.lines().get(0).unitPrice(),original.lines().get(1).unitPrice(),original.lines().get(2).unitPrice(),original.lines().get(3).unitPrice()),day.hours());
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> edit(long id,String version,Request request) {
        validate(request);var config=settings.read(true,availability.today());var previous=detail(id);if(!Objects.equals(previous.get("version"),version))throw new ApiException(409,"VERSION_CONFLICT");if(!"CONFIRMED".equals(previous.get("status")))throw new ApiException(409,"RESERVATION_STATE_CONFLICT");
        LocalDate oldDate=LocalDate.parse((String)previous.get("visitDate"));var ordered=new TreeSet<LocalDate>();ordered.add(oldDate);ordered.add(request.visitDate());for(var date:ordered)availability.lockDay(date);
        var day=availability.day(config,request.visitDate());if(day.reason()!=null&&!"FULL".equals(day.reason()))throw new ApiException("OUT_OF_RANGE".equals(day.reason())?400:409,"OUT_OF_RANGE".equals(day.reason())?"DATE_OUT_OF_RANGE":"DATE_UNAVAILABLE");
        long allowance=day.available()+(oldDate.equals(request.visitDate())?(long)(Integer)previous.get("adults")+(Integer)previous.get("children")+(Integer)previous.get("seniors"):0);if(request.people()>allowance)throw new ApiException(409,"CAPACITY_CONFLICT");
        var original=(Quote)previous.get("quote");var rate=new Rate(original.rateVersion(),original.lines().get(0).unitPrice(),original.lines().get(1).unitPrice(),original.lines().get(2).unitPrice(),original.lines().get(3).unitPrice());var quote=price(request,rate,day.hours());long dayId=availability.lockDay(request.visitDate());
        jdbc.update("UPDATE dbo.Reservation SET visitDayId=?,name=?,email=?,phone=?,note=?,adults=?,children=?,seniors=?,vehicles=?,total=?,locale=? WHERE id=?",dayId,request.name(),request.email(),request.phone(),request.note(),request.adults(),request.children(),request.seniors(),request.vehicles(),new BigDecimal(quote.total()),request.locale(),id);
        var result=detail(id);var confirmation=new Confirmation(Long.toString(id),(String)result.get("code"),"CONFIRMED",(String)result.get("version"),request.name(),request.locale(),quote,config.sinpeNumber(),businessName(config,request.locale()),config.phone(),config.email(),"PENDING");enqueue(id,"RESERVATION_CHANGED",request.email(),request.locale(),confirmation);return result;
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> transition(long id,Transition change,boolean noShow) {
        var config=settings.read(true,availability.today());
        var rows=jdbc.queryForList("SELECT r.*,d.visitDate FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE r.id=?",id);
        if(rows.isEmpty()) throw new ApiException(404,"NOT_FOUND");var row=rows.getFirst();LocalDate date=((java.sql.Date)row.get("visitDate")).toLocalDate();
        availability.lockDay(date);
        if(!Base64.getEncoder().encodeToString((byte[])row.get("version")).equals(change.version())) throw new ApiException(409,"VERSION_CONFLICT");
        if(!"CONFIRMED".equals(row.get("status"))) throw new ApiException(409,"RESERVATION_STATE_CONFLICT");
        if(noShow) {
            var now=ZonedDateTime.now(clock.withZone(ZoneId.of("America/Costa_Rica")));
            String end=config==null?"23:59":config.hours().stream().filter(h->h.weekday()==date.getDayOfWeek().getValue()).map(Hour::closesAt).max(String::compareTo).orElse("23:59");
            if(date.isAfter(now.toLocalDate()) || (date.equals(now.toLocalDate()) && !now.toLocalTime().isAfter(LocalTime.parse(end)))) throw new ApiException(409,"NO_SHOW_TOO_EARLY");
        }
        String state=noShow?"NO_SHOW":"CANCELLED";
        jdbc.update("UPDATE dbo.Reservation SET status=?,statusReason=? WHERE id=?",state,change.reason().trim(),id);
        if(!noShow) enqueue(id,"RESERVATION_CANCELLED",(String)row.get("email"),(String)row.get("locale"),Map.of("code",row.get("code"),"visitDate",date.toString(),"phone",config==null?"":config.phone(),"email",config==null?"":config.email()));
        return Map.of("id",Long.toString(id),"status",state,"version",jdbc.queryForObject("SELECT version FROM dbo.Reservation WHERE id=?",(r,n)->Base64.getEncoder().encodeToString(r.getBytes(1)),id));
    }
}
