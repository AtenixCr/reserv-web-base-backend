package com.tourism.business.admissions.service;
import com.tourism.business.admissions.dto.AdmissionDto.*;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.business.dto.SettingsDto.*;
import com.tourism.business.business.repository.SettingsRepository;
import com.tourism.business.common.*;
import com.tourism.business.finance.service.CashSessionService;
import com.tourism.business.reservations.service.ReservationService;
import com.tourism.business.reservations.dto.ReservationDto.Quote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AdmissionService {
    private final JdbcTemplate jdbc;private final SettingsRepository settings;private final AvailabilityService dates;private final ReservationService prices;private final CashSessionService cash;private final IdempotencyStore idempotency;private final Validator validator;
    public AdmissionService(JdbcTemplate jdbc,SettingsRepository settings,AvailabilityService dates,ReservationService prices,CashSessionService cash,IdempotencyStore idempotency,Validator validator){this.jdbc=jdbc;this.settings=settings;this.dates=dates;this.prices=prices;this.cash=cash;this.idempotency=idempotency;this.validator=validator;}
    public void validate(Object value){var errors=validator.validate(value);if(!errors.isEmpty()){var fields=new LinkedHashMap<String,String>();errors.forEach(e->fields.put(e.getPropertyPath().toString(),"INVALID_FIELD"));throw new ApiException(400,"INVALID_INPUT",fields);}}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private void contact(String name,String phone){if(clean(name)!=null&&name.trim().length()<2)throw new ApiException(400,"INVALID_INPUT",Map.of("name","INVALID_FIELD"));if(clean(phone)!=null&&(!phone.trim().matches("\\+?[0-9 ()-]+")||phone.replaceAll("\\D","").length()<7||phone.replaceAll("\\D","").length()>15))throw new ApiException(400,"INVALID_INPUT",Map.of("phone","INVALID_FIELD"));}
    private long people(int a,int c,int s){long n=(long)a+c+s;if(n<1)throw new ApiException(400,"INVALID_INPUT",Map.of("adults","PEOPLE_REQUIRED"));return n;}
    private void version(Map<String,Object> row,String version){if(!Base64.getEncoder().encodeToString((byte[])row.get("version")).equals(version))throw new ApiException(409,"VERSION_CONFLICT");}
    private Map<String,Object> row(String table,long id){var rows=jdbc.queryForList("SELECT r.*,d.visitDate FROM dbo."+table+" r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE r.id=?",id);if(rows.isEmpty())throw new ApiException(404,"NOT_FOUND");return rows.getFirst();}
    private int count(Map<String,Object> r,String key){return ((Number)r.get(key)).intValue();}
    private Rate rate(Map<String,Object> r){return new Rate(r.getOrDefault("ratePlanId","historical").toString(),r.get("adultPrice").toString(),r.get("childPrice").toString(),r.get("seniorPrice").toString(),r.get("parkingPrice").toString());}
    private Quote calculate(LocalDate date,int a,int c,int s,int v,Rate rate){return prices.price(new com.tourism.business.reservations.dto.ReservationDto.Request(date,"","","",a,c,s,v,"","es",rate.id()),rate,List.of());}
    private void capacity(Settings config,long dayId,LocalDate date,long delta,boolean requireOpen){
        if(config==null)throw new ApiException(409,"SETTINGS_INCOMPLETE");
        if(requireOpen&&(config.closedDates().stream().anyMatch(d->d.visitDate().equals(date))||config.hours().stream().noneMatch(h->h.weekday()==date.getDayOfWeek().getValue())))throw new ApiException(409,"DATE_UNAVAILABLE");
        long capacity=jdbc.queryForObject("SELECT COALESCE(capacityOverride,?) FROM dbo.VisitDay WHERE id=?",Long.class,config.defaultCapacity(),dayId);
        long used=jdbc.queryForObject("SELECT COALESCE((SELECT SUM(CAST(adults AS bigint)+children+seniors) FROM dbo.Reservation WHERE visitDayId=? AND status='CONFIRMED'),0)+COALESCE((SELECT SUM(CAST(adults AS bigint)+children+seniors) FROM dbo.Admission WHERE visitDayId=?),0)",Long.class,dayId,dayId);
        if(delta>0&&used+delta>capacity)throw new ApiException(409,"CAPACITY_CONFLICT");
    }
    private Quote admissionQuote(Request r,Settings config,boolean lock){
        validate(r);people(r.adults(),r.children(),r.seniors());contact(r.name(),r.phone());
        Rate rate;
        if(r.reservationId()!=null){var reservation=row("Reservation",r.reservationId());version(reservation,r.reservationVersion());if(!"CONFIRMED".equals(reservation.get("status")))throw new ApiException(409,"RESERVATION_STATE_CONFLICT");if(!dates.today().toString().equals(reservation.get("visitDate").toString()))throw new ApiException(409,"ADMISSION_TODAY_ONLY");for(String field:List.of("adults","children","seniors","vehicles")){int desired=switch(field){case "adults"->r.adults();case "children"->r.children();case "seniors"->r.seniors();default->r.vehicles();};if(count(reservation,field)!=desired)throw new ApiException(409,"RESERVATION_QUANTITIES_CHANGED");}rate=rate(reservation);}
        else {rate=settings.rate();if(rate==null)throw new ApiException(409,"SETTINGS_INCOMPLETE");if(lock&&!Objects.equals(rate.id(),r.rateVersion()))throw new ApiException(409,"RATE_CHANGED");}
        return calculate(dates.today(),r.adults(),r.children(),r.seniors(),r.vehicles(),rate);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Quote quote(Request r){return admissionQuote(r,settings.read(false,dates.today()),false);}
    private BigDecimal change(BigDecimal total,String method,String received){
        if(total.signum()==0)return BigDecimal.ZERO.setScale(2);
        if(method==null)throw new ApiException(400,"INVALID_INPUT",Map.of("paymentMethod","INVALID_FIELD"));
        if(!"CASH".equals(method)){if(received!=null&&!received.isBlank())throw new ApiException(400,"INVALID_INPUT",Map.of("receivedAmount","INVALID_FIELD"));return BigDecimal.ZERO.setScale(2);}
        if(received==null||new BigDecimal(received).compareTo(total)<0)throw new ApiException(400,"INSUFFICIENT_CASH",Map.of("receivedAmount","INVALID_FIELD"));return new BigDecimal(received).subtract(total).setScale(2);
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> create(Request request,String key,long actor){
        validate(request);var config=settings.read(true,dates.today());String operation="ADMISSION:"+actor;var replay=idempotency.read(operation,key,request);if(replay!=null)return replay;
        long day=dates.lockDay(dates.today());long session=cash.lockCurrent();var quote=admissionQuote(request,config,true);long persons=people(request.adults(),request.children(),request.seniors());capacity(config,day,dates.today(),request.reservationId()==null?persons:0,true);
        if(request.reservationId()!=null&&!jdbc.queryForList("SELECT id FROM dbo.Admission WHERE reservationId=?",request.reservationId()).isEmpty())throw new ApiException(409,"ALREADY_ADMITTED");
        var total=new BigDecimal(quote.total());var change=change(total,request.paymentMethod(),request.receivedAmount());
        Map<String,Object> original=request.reservationId()==null?null:row("Reservation",request.reservationId());
        String name=original==null?clean(request.name()):(String)original.get("name"),email=original==null?clean(request.email()):(String)original.get("email"),phone=original==null?clean(request.phone()):(String)original.get("phone"),note=original==null?clean(request.note()):(String)original.get("note");
        String code="VIS-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
        jdbc.update("INSERT dbo.Admission(code,visitDayId,reservationId,name,email,phone,note,adults,children,seniors,vehicles,adultPrice,childPrice,seniorPrice,parkingPrice,actorId) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",code,day,request.reservationId(),name,email,phone,note,request.adults(),request.children(),request.seniors(),request.vehicles(),new BigDecimal(quote.lines().get(0).unitPrice()),new BigDecimal(quote.lines().get(1).unitPrice()),new BigDecimal(quote.lines().get(2).unitPrice()),new BigDecimal(quote.lines().get(3).unitPrice()),actor);
        long id=jdbc.queryForObject("SELECT id FROM dbo.Admission WHERE code=?",Long.class,code);
        if(total.signum()>0)jdbc.update("INSERT dbo.Payment(admissionId,cashSessionId,method,amount,receivedAmount,changeAmount,reference,actorId) VALUES(?,?,?,?,?,?,?,?)",id,session,request.paymentMethod(),total,"CASH".equals(request.paymentMethod())?new BigDecimal(request.receivedAmount()):null,"CASH".equals(request.paymentMethod())?change:null,clean(request.reference()),actor);
        if(request.reservationId()!=null)jdbc.update("UPDATE dbo.Reservation SET status='CHECKED_IN' WHERE id=?",request.reservationId());
        jdbc.update("UPDATE dbo.CashSession SET status=status WHERE id=?",session);var result=detail(id);idempotency.save(operation,key,request,result);return result;
    }
    private Map<String,Object> view(Map<String,Object> r){var result=new LinkedHashMap<String,Object>();for(String key:List.of("id","code","name","email","phone","note","adults","children","seniors","vehicles"))result.put(key,key.equals("id")?r.get(key).toString():r.get(key));result.put("visitDate",r.get("visitDate").toString());result.put("version",Base64.getEncoder().encodeToString((byte[])r.get("version")));result.put("reservationId",r.get("reservationId")==null?null:r.get("reservationId").toString());result.put("status","ADMITTED");result.put("quote",calculate(LocalDate.parse(r.get("visitDate").toString()),count(r,"adults"),count(r,"children"),count(r,"seniors"),count(r,"vehicles"),rate(r)));return result;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> detail(long id){
        var result=view(row("Admission",id));
        var payment=jdbc.queryForList("SELECT id,method,amount,receivedAmount,changeAmount,reference FROM dbo.Payment WHERE admissionId=?",id);result.put("payment",payment.isEmpty()?null:jsonRow(payment.getFirst()));
        result.put("revisions",jdbc.queryForList("SELECT id,adults,children,seniors,vehicles,difference,reason,createdAt FROM dbo.AdmissionRevision WHERE admissionId=? ORDER BY id",id).stream().map(this::jsonRow).toList());return result;
    }
    private Map<String,Object> jsonRow(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k,v instanceof BigDecimal d?d.toPlainString():v instanceof java.sql.Timestamp t?t.toLocalDateTime().toInstant(java.time.ZoneOffset.UTC).toString():k.equals("id")?v.toString():v));return result;}
    @Transactional(readOnly=true)
    public Map<String,Object> list(LocalDate date,String search,int page,int size){if(page<0||size<1||size>100||search.length()>120)throw new ApiException(400,"INVALID_INPUT");String filter=" FROM dbo.Admission a JOIN dbo.VisitDay d ON d.id=a.visitDayId WHERE d.visitDate=? AND (?='' OR CHARINDEX(?,COALESCE(a.name,''))>0 OR CHARINDEX(?,a.code)>0)";var rows=jdbc.queryForList("SELECT a.*,d.visitDate"+filter+" ORDER BY a.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",date,search,search,search,(long)page*size,size);return Map.of("items",rows.stream().map(this::view).toList(),"page",page,"size",size,"total",jdbc.queryForObject("SELECT COUNT(*)"+filter,Long.class,date,search,search,search));}
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> contact(long id,Contact request){validate(request);contact(request.name(),request.phone());settings.read(true,dates.today());var r=row("Admission",id);dates.lockDay(LocalDate.parse(r.get("visitDate").toString()));version(r,request.version());jdbc.update("UPDATE dbo.Admission SET name=?,email=?,phone=?,note=? WHERE id=?",clean(request.name()),clean(request.email()),clean(request.phone()),clean(request.note()),id);jdbc.update("UPDATE dbo.CashSession SET status=status WHERE businessDate=? AND status='OPEN'",r.get("visitDate"));return detail(id);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> correctionQuote(long id,Correction request){validate(request);people(request.adults(),request.children(),request.seniors());var r=row("Admission",id);version(r,request.version());var quote=calculate(LocalDate.parse(r.get("visitDate").toString()),request.adults(),request.children(),request.seniors(),request.vehicles(),rate(r));var previous=calculate(quote.visitDate(),count(r,"adults"),count(r,"children"),count(r,"seniors"),count(r,"vehicles"),rate(r));return Map.of("quote",quote,"difference",new BigDecimal(quote.total()).subtract(new BigDecimal(previous.total())).toPlainString());}
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> correct(long id,Correction request,String key,long actor){
        validate(request);var config=settings.read(true,dates.today());String operation="CORRECTION:"+actor+":"+id;var replay=idempotency.read(operation,key,request);if(replay!=null)return replay;
        var r=row("Admission",id);LocalDate date=LocalDate.parse(r.get("visitDate").toString());long day=dates.lockDay(date);version(r,request.version());long delta=people(request.adults(),request.children(),request.seniors())-people(count(r,"adults"),count(r,"children"),count(r,"seniors"));capacity(config,day,date,delta,false);
        var calculation=correctionQuote(id,request);var difference=new BigDecimal((String)calculation.get("difference"));Long session=difference.signum()==0?null:cash.lockCurrent();var change=change(difference.max(BigDecimal.ZERO),request.paymentMethod(),request.receivedAmount());
        if(difference.signum()<0){if(request.paymentMethod()==null)throw new ApiException(400,"INVALID_INPUT",Map.of("paymentMethod","INVALID_FIELD"));BigDecimal net=jdbc.queryForObject("SELECT COALESCE((SELECT SUM(amount) FROM dbo.Payment WHERE admissionId=?),0)+COALESCE((SELECT SUM(amount) FROM dbo.Adjustment WHERE admissionId=?),0)-COALESCE((SELECT SUM(amount) FROM dbo.Refund WHERE admissionId=?),0)",BigDecimal.class,id,id,id);if(difference.negate().compareTo(net)>0)throw new ApiException(409,"REFUND_EXCEEDS_NET");}
        jdbc.update("INSERT dbo.AdmissionRevision(admissionId,previousAdults,previousChildren,previousSeniors,previousVehicles,adults,children,seniors,vehicles,difference,reason,actorId) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,count(r,"adults"),count(r,"children"),count(r,"seniors"),count(r,"vehicles"),request.adults(),request.children(),request.seniors(),request.vehicles(),difference,request.reason().trim(),actor);
        long revision=jdbc.queryForObject("SELECT MAX(id) FROM dbo.AdmissionRevision WHERE admissionId=?",Long.class,id);
        if(difference.signum()>0)jdbc.update("INSERT dbo.Adjustment(cashSessionId,admissionId,revisionId,kind,method,amount,reason,actorId,receivedAmount,changeAmount,reference) VALUES(?,?,?,'ADDITIONAL_PAYMENT',?,?,?,?,?,?,?)",session,id,revision,request.paymentMethod(),difference,request.reason().trim(),actor,"CASH".equals(request.paymentMethod())?new BigDecimal(request.receivedAmount()):null,"CASH".equals(request.paymentMethod())?change:null,clean(request.reference()));
        if(difference.signum()<0)jdbc.update("INSERT dbo.Refund(admissionId,revisionId,cashSessionId,method,amount,reason,actorId) VALUES(?,?,?,?,?,?,?)",id,revision,session,request.paymentMethod(),difference.negate(),request.reason().trim(),actor);
        jdbc.update("UPDATE dbo.Admission SET adults=?,children=?,seniors=?,vehicles=? WHERE id=?",request.adults(),request.children(),request.seniors(),request.vehicles(),id);
        if(session!=null)jdbc.update("UPDATE dbo.CashSession SET status=status WHERE id=?",session);else jdbc.update("UPDATE dbo.CashSession SET status=status WHERE businessDate=? AND status='OPEN'",date);var result=new LinkedHashMap<String,Object>(detail(id));result.put("difference",difference.toPlainString());result.put("changeAmount",change.toPlainString());idempotency.save(operation,key,request,result);return result;
    }
}
