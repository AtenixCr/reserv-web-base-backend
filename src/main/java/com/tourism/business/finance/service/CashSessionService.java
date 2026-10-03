package com.tourism.business.finance.service;
import com.tourism.business.business.repository.SettingsRepository;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.common.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class CashSessionService {
    private final JdbcTemplate jdbc;private final AvailabilityService dates;private final SettingsRepository settings;
    public CashSessionService(JdbcTemplate jdbc,AvailabilityService dates,SettingsRepository settings){this.jdbc=jdbc;this.dates=dates;this.settings=settings;}
    public long lockCurrent() {
        var rows=jdbc.queryForList("SELECT id,businessDate,status FROM dbo.CashSession WITH(UPDLOCK,HOLDLOCK) WHERE status='OPEN' OR businessDate=? ORDER BY businessDate",dates.today());
        for(var row:rows)if("OPEN".equals(row.get("status"))&&((java.sql.Date)row.get("businessDate")).toLocalDate().equals(dates.today()))return ((Number)row.get("id")).longValue();
        throw new ApiException(409,rows.stream().anyMatch(r->"OPEN".equals(r.get("status")))?"PREVIOUS_SESSION_OPEN":"CASH_SESSION_REQUIRED");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> current(boolean admin) {
        var rows=jdbc.queryForList("SELECT TOP(1) * FROM dbo.CashSession WHERE status='OPEN' OR businessDate=? ORDER BY CASE WHEN status='OPEN' THEN 0 ELSE 1 END,businessDate DESC",dates.today());
        var result=new LinkedHashMap<String,Object>();result.put("serverDate",dates.today().toString());result.put("session",null);
        if(!rows.isEmpty()){var r=rows.getFirst();var session=new LinkedHashMap<String,Object>();session.put("id",r.get("id").toString());session.put("businessDate",r.get("businessDate").toString());session.put("status",r.get("status"));session.put("version",Base64.getEncoder().encodeToString((byte[])r.get("version")));session.put("canPay","OPEN".equals(r.get("status"))&&dates.today().toString().equals(r.get("businessDate").toString()));if(admin)session.put("openingFloat",((BigDecimal)r.get("openingFloat")).toPlainString());result.put("session",session);}return result;
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> open(String amount,long actor) {
        settings.read(true,dates.today());
        if(!jdbc.queryForList("SELECT id FROM dbo.CashSession WITH(UPDLOCK,HOLDLOCK) WHERE status='OPEN' OR businessDate=?",dates.today()).isEmpty())throw new ApiException(409,"CASH_SESSION_EXISTS");
        jdbc.update("INSERT dbo.CashSession(businessDate,openingFloat,status,openedBy) VALUES(?,?,'OPEN',?)",dates.today(),new BigDecimal(amount),actor);return current(true);
    }
}
