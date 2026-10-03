package com.tourism.business.business.repository;
import com.tourism.business.business.dto.SettingsDto.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
@Repository
public class SettingsRepository {
    public record Usage(long id, LocalDate date, long people) {}
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public SettingsRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    public Settings read(boolean lock, LocalDate today) {
        var rows = jdbc.query("SELECT * FROM dbo.BusinessSettings " + (lock ? "WITH (UPDLOCK,HOLDLOCK) " : "") + "WHERE singleton=1", (r,n) ->
            new Settings(Base64.getEncoder().encodeToString(r.getBytes("version")),r.getInt("defaultCapacity"),r.getString("phone"),r.getString("email"),r.getString("address"),r.getString("sinpeNumber"),r.getBoolean("enabled"),
                translations(r.getLong("id")), hours(r.getLong("id")),closed(today),overrides(today)));
        return rows.isEmpty() ? null : rows.getFirst();
    }
    private List<Translation> translations(long id) {
        return jdbc.query("SELECT locale,displayName,description,services FROM dbo.BusinessTranslation WHERE settingsId=? ORDER BY locale", (r,n) -> new Translation(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),id);
    }
    private List<Hour> hours(long id) {
        return jdbc.query("SELECT weekday,opensAt,closesAt FROM dbo.BusinessHour WHERE settingsId=? ORDER BY weekday,opensAt", (r,n) -> new Hour(r.getInt(1),r.getString(2).substring(0,5),r.getString(3).substring(0,5)),id);
    }
    private List<Closed> closed(LocalDate today) {
        return jdbc.query("SELECT visitDate,reason FROM dbo.ClosedDate WHERE visitDate>=? ORDER BY visitDate", (r,n) -> new Closed(r.getDate(1).toLocalDate(),r.getString(2)),today);
    }
    private List<CapacityOverride> overrides(LocalDate today) {
        return jdbc.query("SELECT visitDate,capacityOverride FROM dbo.VisitDay WHERE visitDate>=? AND capacityOverride IS NOT NULL ORDER BY visitDate", (r,n) -> new CapacityOverride(r.getDate(1).toLocalDate(),r.getInt(2)),today);
    }
    public List<Usage> lockUsage(LocalDate today) {
        var days = jdbc.query("SELECT id,visitDate FROM dbo.VisitDay WITH (UPDLOCK,HOLDLOCK) WHERE visitDate>=? ORDER BY visitDate", (r,n) -> new Usage(r.getLong(1),r.getDate(2).toLocalDate(),0),today);
        return days.stream().map(day -> new Usage(day.id,day.date,jdbc.queryForObject("SELECT COALESCE((SELECT SUM(CAST(adults AS bigint)+children+seniors) FROM dbo.Reservation WHERE visitDayId=? AND status='CONFIRMED'),0)+COALESCE((SELECT SUM(CAST(adults AS bigint)+children+seniors) FROM dbo.Admission WHERE visitDayId=?),0)",Long.class,day.id,day.id))).toList();
    }
    public Rate rate() {
        var rows=jdbc.query("SELECT TOP(1) id,adultPrice,childPrice,seniorPrice,parkingPrice FROM dbo.RatePlan WHERE effectiveAt<=SYSUTCDATETIME() ORDER BY effectiveAt DESC,id DESC",(r,n)->new Rate(Long.toString(r.getLong(1)),r.getBigDecimal(2).toPlainString(),r.getBigDecimal(3).toPlainString(),r.getBigDecimal(4).toPlainString(),r.getBigDecimal(5).toPlainString()));
        return rows.isEmpty()?null:rows.getFirst();
    }
    public List<Exchange> exchanges() {
        return jdbc.query("SELECT currency,rate,updatedAt FROM dbo.ExchangeRate ORDER BY currency",(r,n)->new Exchange(r.getString(1),r.getBigDecimal(2).toPlainString(),r.getTimestamp(3).toLocalDateTime().toInstant(java.time.ZoneOffset.UTC).atZone(ZoneId.of("America/Costa_Rica")).toLocalDate()));
    }
    public void save(Settings value, LocalDate today) {
        int updated=jdbc.update("UPDATE dbo.BusinessSettings SET defaultCapacity=?,phone=?,email=?,address=?,sinpeNumber=?,enabled=? WHERE singleton=1",value.defaultCapacity(),value.phone().trim(),value.email().trim(),value.address().trim(),value.sinpeNumber().trim(),value.enabled());
        if(updated==0) jdbc.update("INSERT dbo.BusinessSettings(defaultCapacity,phone,email,address,sinpeNumber,enabled) VALUES(?,?,?,?,?,?)",value.defaultCapacity(),value.phone().trim(),value.email().trim(),value.address().trim(),value.sinpeNumber().trim(),value.enabled());
        long id=jdbc.queryForObject("SELECT id FROM dbo.BusinessSettings WHERE singleton=1",Long.class);
        jdbc.update("DELETE dbo.BusinessTranslation WHERE settingsId=?",id);
        for(var t:value.translations()) jdbc.update("INSERT dbo.BusinessTranslation(settingsId,locale,displayName,description,services) VALUES(?,?,?,?,?)",id,t.locale(),t.displayName().trim(),t.description().trim(),t.services().trim());
        jdbc.update("DELETE dbo.BusinessHour WHERE settingsId=?",id);
        for(var h:value.hours()) jdbc.update("INSERT dbo.BusinessHour(settingsId,weekday,opensAt,closesAt) VALUES(?,?,?,?)",id,h.weekday(),h.opensAt(),h.closesAt());
        jdbc.update("DELETE dbo.ClosedDate WHERE visitDate>=?",today);
        for(var c:value.closedDates()) jdbc.update("INSERT dbo.ClosedDate(visitDate,reason) VALUES(?,?)",c.visitDate(),c.reason().trim());
        jdbc.update("UPDATE dbo.VisitDay SET capacityOverride=NULL WHERE visitDate>=? AND capacityOverride IS NOT NULL",today);
        for(var o:value.capacityOverrides()) {
            if(jdbc.update("UPDATE dbo.VisitDay SET capacityOverride=? WHERE visitDate=?",o.capacity(),o.visitDate())==0) jdbc.update("INSERT dbo.VisitDay(visitDate,capacityOverride) VALUES(?,?)",o.visitDate(),o.capacity());
        }
    }
    public void touch() { jdbc.update("UPDATE dbo.BusinessSettings SET defaultCapacity=defaultCapacity WHERE singleton=1"); }
    public void publish(Prices p) { jdbc.update("INSERT dbo.RatePlan(adultPrice,childPrice,seniorPrice,parkingPrice,effectiveAt) VALUES(?,?,?,?,SYSUTCDATETIME())",new BigDecimal(p.adultPrice()),new BigDecimal(p.childPrice()),new BigDecimal(p.seniorPrice()),new BigDecimal(p.parkingPrice())); }
    public void exchange(Exchanges changes) {
        jdbc.update("DELETE dbo.ExchangeRate");
        for(var e:changes.rates()) jdbc.update("INSERT dbo.ExchangeRate(currency,rate,updatedAt) VALUES(?,?,?)",e.currency(),new BigDecimal(e.rate()),java.time.LocalDateTime.ofInstant(e.updatedOn().atStartOfDay(ZoneId.of("America/Costa_Rica")).toInstant(),java.time.ZoneOffset.UTC));
    }
    public void enqueueHours(Settings before, Settings after, LocalDate today) {
        if(before==null) return;
        var rows=jdbc.queryForList("SELECT r.id,r.code,r.email,r.locale,d.visitDate FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE r.status='CONFIRMED' AND d.visitDate>=?",today);
        for(var row:rows) {
            LocalDate date=((java.sql.Date)row.get("visitDate")).toLocalDate();
            int day=date.getDayOfWeek().getValue();
            var oldHours=before.hours().stream().filter(h->h.weekday()==day).sorted(java.util.Comparator.comparing(Hour::opensAt)).toList();
            var newHours=after.hours().stream().filter(h->h.weekday()==day).sorted(java.util.Comparator.comparing(Hour::opensAt)).toList();
            if(oldHours.equals(newHours)) continue;
            long id=((Number)row.get("id")).longValue();
            int version=jdbc.queryForObject("SELECT COALESCE(MAX(eventVersion),0)+1 FROM dbo.EmailOutbox WITH(UPDLOCK,HOLDLOCK) WHERE eventType='HOURS_CHANGED' AND eventId=?",Integer.class,id);
            String payload=mapper.writeValueAsString(Map.of("code",row.get("code"),"visitDate",date.toString(),"hours",newHours,"locale",row.get("locale"),"phone",after.phone(),"email",after.email()));
            jdbc.update("INSERT dbo.EmailOutbox(eventType,eventId,eventVersion,recipient,locale,payload,messageId) VALUES('HOURS_CHANGED',?,?,?,?,?,?)",id,version,row.get("email"),row.get("locale"),payload,"hours-"+id+"-"+version+"@tourism.local");
        }
    }
    public Map<String,Object> dashboard(LocalDate today) {
        long pending=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE d.visitDate=? AND r.status='CONFIRMED'",Long.class,today);
        long people=jdbc.queryForObject("SELECT COALESCE(SUM(CAST(a.adults AS bigint)+a.children+a.seniors),0) FROM dbo.Admission a JOIN dbo.VisitDay d ON d.id=a.visitDayId WHERE d.visitDate=?",Long.class,today);
        var tasks=jdbc.queryForList("SELECT TOP(100) title,status FROM dbo.Task WHERE taskDate=? ORDER BY id",today);
        return Map.of("serverDate",today,"pendingReservations",pending,"admittedPeople",people,"tasks",tasks);
    }
}
