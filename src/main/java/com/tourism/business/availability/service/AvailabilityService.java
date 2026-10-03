package com.tourism.business.availability.service;
import com.tourism.business.business.dto.SettingsDto.*;
import com.tourism.business.business.repository.SettingsRepository;
import com.tourism.business.business.service.SettingsService;
import com.tourism.business.common.ApiException;
import com.tourism.business.reservations.dto.ReservationDto.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AvailabilityService {
    private final JdbcTemplate jdbc;
    private final SettingsRepository settings;
    private final SettingsService dates;
    public AvailabilityService(JdbcTemplate jdbc,SettingsRepository settings,SettingsService dates) { this.jdbc=jdbc; this.settings=settings; this.dates=dates; }
    public LocalDate today() { return dates.today(); }
    public long lockDay(LocalDate date) {
        var rows=jdbc.queryForList("SELECT id FROM dbo.VisitDay WITH (UPDLOCK,HOLDLOCK) WHERE visitDate=?",Long.class,date);
        if(rows.isEmpty()) { jdbc.update("INSERT dbo.VisitDay(visitDate) VALUES(?)",date); return jdbc.queryForObject("SELECT id FROM dbo.VisitDay WHERE visitDate=?",Long.class,date); }
        return rows.getFirst();
    }
    public Day day(Settings config,LocalDate date) {
        var hours=config==null?List.<Hour>of():config.hours().stream().filter(h->h.weekday()==date.getDayOfWeek().getValue()).toList();
        String reason=null;
        if(date.isBefore(today().plusDays(1)) || date.isAfter(today().plusDays(364))) reason="OUT_OF_RANGE";
        else if(config==null || !config.enabled() || settings.rate()==null) reason="NOT_CONFIGURED";
        else if(config.closedDates().stream().anyMatch(d->d.visitDate().equals(date))) reason="BLOCKED";
        else if(hours.isEmpty()) reason="CLOSED";
        if(reason!=null) return new Day(date,hours,0,reason);
        int capacity=config.capacityOverrides().stream().filter(d->d.visitDate().equals(date)).map(CapacityOverride::capacity).findFirst().orElse(config.defaultCapacity());
        long used=jdbc.queryForObject("SELECT COALESCE((SELECT SUM(CAST(r.adults AS bigint)+r.children+r.seniors) FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE d.visitDate=? AND r.status='CONFIRMED'),0)+COALESCE((SELECT SUM(CAST(a.adults AS bigint)+a.children+a.seniors) FROM dbo.Admission a JOIN dbo.VisitDay d ON d.id=a.visitDayId WHERE d.visitDate=?),0)",Long.class,date,date);
        long remaining=Math.max(0,capacity-used);
        return new Day(date,hours,remaining,remaining==0?"FULL":null);
    }
    public Day require(Settings config,LocalDate date,long people) {
        var day=day(config,date);
        if("OUT_OF_RANGE".equals(day.reason())) throw new ApiException(400,"DATE_OUT_OF_RANGE",Map.of("visitDate","INVALID_FIELD"));
        if(day.reason()!=null) throw new ApiException(409,"DATE_UNAVAILABLE",Map.of("visitDate",day.reason()));
        if(people>day.available()) throw new ApiException(409,"CAPACITY_CONFLICT",Map.of("available",Long.toString(day.available())));
        return day;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Month month(String value) {
        YearMonth month;
        try { if(value==null || !value.matches("[0-9]{4}-[0-9]{2}")) throw new IllegalArgumentException(); month=YearMonth.parse(value); }
        catch(Exception e) { throw new ApiException(400,"INVALID_INPUT",Map.of("month","INVALID_FIELD")); }
        if(month.isBefore(YearMonth.from(today())) || month.isAfter(YearMonth.from(today().plusDays(364)))) throw new ApiException(400,"DATE_OUT_OF_RANGE");
        var config=settings.read(false,today());
        var days=new ArrayList<Day>();
        for(int d=1;d<=month.lengthOfMonth();d++) days.add(day(config,month.atDay(d)));
        return new Month(today(),today().plusDays(1),today().plusDays(364),days);
    }
}
