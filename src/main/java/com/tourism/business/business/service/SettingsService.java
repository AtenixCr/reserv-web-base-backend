package com.tourism.business.business.service;
import com.tourism.business.business.dto.SettingsDto.*;
import com.tourism.business.business.repository.SettingsRepository;
import com.tourism.business.common.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
@Service
public class SettingsService {
    private final SettingsRepository repository;
    private final Clock clock;
    public SettingsService(SettingsRepository repository,Clock clock) { this.repository=repository; this.clock=clock; }
    public LocalDate today() { return LocalDate.now(clock.withZone(ZoneId.of("America/Costa_Rica"))); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Snapshot read() { return new Snapshot(repository.read(false,today()),repository.rate(),repository.exchanges(),today()); }
    private void version(Settings current,String version) {
        if(!Objects.equals(current==null?"new":current.version(),version)) throw new ApiException(409,"VERSION_CONFLICT");
    }
    private void invalid(String field) { throw new ApiException(400,"INVALID_INPUT",Map.of(field,"INVALID_FIELD")); }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Snapshot save(Settings value) {
        Settings current=repository.read(true,today()); version(current,value.version());
        if(!value.phone().matches("\\+?[0-9 ()-]+") || !value.sinpeNumber().matches("\\+?[0-9 ()-]+")) invalid("phone");
        for(String phone:List.of(value.phone(),value.sinpeNumber())) if(phone.replaceAll("\\D", "").length()<7 || phone.replaceAll("\\D", "").length()>15) invalid("phone");
        if(value.translations().stream().map(Translation::locale).distinct().count()!=value.translations().size() || value.translations().stream().noneMatch(t->t.locale().equals("es"))) invalid("translations");
        for(int day=1;day<=7;day++) {
            int weekday=day; var hours=value.hours().stream().filter(h->h.weekday()==weekday).sorted(Comparator.comparing(Hour::opensAt)).toList();
            String end="00:00";
            for(var h:hours) { if(h.opensAt().compareTo(h.closesAt())>=0 || h.opensAt().compareTo(end)<0) invalid("hours"); end=h.closesAt(); }
        }
        if(value.closedDates().stream().map(Closed::visitDate).distinct().count()!=value.closedDates().size() || value.closedDates().stream().anyMatch(c->c.visitDate().isBefore(today()))) invalid("closedDates");
        if(value.capacityOverrides().stream().map(CapacityOverride::visitDate).distinct().count()!=value.capacityOverrides().size() || value.capacityOverrides().stream().anyMatch(o->o.visitDate().isBefore(today()))) invalid("capacityOverrides");
        if(value.enabled() && (value.hours().isEmpty() || repository.rate()==null)) throw new ApiException(409,"SETTINGS_INCOMPLETE");
        var conflicts=new ArrayList<String>();
        for(var usage:repository.lockUsage(today())) {
            int capacity=value.capacityOverrides().stream().filter(o->o.visitDate().equals(usage.date())).map(CapacityOverride::capacity).findFirst().orElse(value.defaultCapacity());
            boolean closed=value.closedDates().stream().anyMatch(c->c.visitDate().equals(usage.date())) || value.hours().stream().noneMatch(h->h.weekday()==usage.date().getDayOfWeek().getValue());
            if(usage.people()>capacity || (closed && usage.people()>0)) conflicts.add(usage.date().toString());
        }
        if(!conflicts.isEmpty()) throw new ApiException(409,"CONFIGURATION_CONFLICT",Map.of("dates",String.join(", ",conflicts)));
        repository.save(value,today()); repository.enqueueHours(current,value,today()); return read();
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Snapshot publish(Prices prices) {
        var current=repository.read(true,today()); version(current,prices.version());
        if(current==null) throw new ApiException(409,"SETTINGS_INCOMPLETE");
        repository.publish(prices); repository.touch(); return read();
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Snapshot exchanges(Exchanges exchanges) {
        var current=repository.read(true,today()); version(current,exchanges.version());
        if(current==null) throw new ApiException(409,"SETTINGS_INCOMPLETE");
        if(exchanges.rates().stream().map(Exchange::currency).distinct().count()!=exchanges.rates().size()) invalid("rates");
        for(var rate:exchanges.rates()) if(new BigDecimal(rate.rate()).signum()<=0 || rate.updatedOn().isAfter(today())) invalid("rates");
        repository.exchange(exchanges); repository.touch(); return read();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> publicBusiness() {
        var data=read(); var result=new LinkedHashMap<String,Object>();
        result.put("serverDate",today()); result.put("timeZone","America/Costa_Rica"); result.put("currency","CRC");
        result.put("configured",data.settings()!=null); result.put("reservationsEnabled",data.settings()!=null && data.settings().enabled() && data.ratePlan()!=null && !data.settings().hours().isEmpty());
        if(data.settings()!=null) {
            var s=data.settings(); result.put("translations",s.translations()); result.put("phone",s.phone()); result.put("email",s.email());
            result.put("address",s.address()); result.put("hours",s.hours()); result.put("sinpeNumber",s.sinpeNumber());
        }
        result.put("ratePlan",data.ratePlan()); result.put("exchangeRates",data.exchangeRates()); return result;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> dashboard() {
        var result=new LinkedHashMap<String,Object>(repository.dashboard(today()));
        var settings=repository.read(false,today());
        boolean closed=settings==null || settings.closedDates().stream().anyMatch(d->d.visitDate().equals(today()));
        result.put("hours",closed?List.of():settings.hours().stream().filter(h->h.weekday()==today().getDayOfWeek().getValue()).toList());
        return result;
    }
}
