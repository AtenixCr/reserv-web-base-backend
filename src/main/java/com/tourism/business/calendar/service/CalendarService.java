package com.tourism.business.calendar.service;
import com.tourism.business.calendar.dto.TaskDto.*;
import com.tourism.business.availability.service.AvailabilityService;
import com.tourism.business.common.*;
import java.time.*;
import java.util.*;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class CalendarService {
    private final JdbcTemplate jdbc;private final AvailabilityService dates;private final Validator validator;private final IdempotencyStore keys;
    public CalendarService(JdbcTemplate jdbc,AvailabilityService dates,Validator validator,IdempotencyStore keys){this.jdbc=jdbc;this.dates=dates;this.validator=validator;this.keys=keys;}
    private void date(LocalDate date){if(date==null||date.getYear()<1||date.getYear()>9999)throw new ApiException(400,"INVALID_INPUT");}
    private void page(int page,int size){if(page<0||size<1||size>100)throw new ApiException(400,"INVALID_INPUT");}
    private void validate(Object request){if(!validator.validate(request).isEmpty())throw new ApiException(400,"INVALID_INPUT");}
    private Map<String,Object> json(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k,v instanceof java.sql.Timestamp t?t.toLocalDateTime().toInstant(ZoneOffset.UTC).toString():v instanceof java.sql.Date d?d.toLocalDate().toString():v instanceof byte[] b?Base64.getEncoder().encodeToString(b):v!=null&&(k.equals("id")||k.endsWith("Id")||k.equals("updatedBy"))?v.toString():v));return result;}
    private static final String SELECT="SELECT t.id,t.taskDate,t.title,t.description,t.status,t.authorId,t.updatedBy,t.createdAt,t.updatedAt,t.version,a.name authorName,u.name updatedByName FROM dbo.Task t JOIN dbo.AppUser a ON a.id=t.authorId JOIN dbo.AppUser u ON u.id=t.updatedBy";
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> detail(long id){var rows=jdbc.queryForList(SELECT+" WHERE t.id=?",id);if(rows.isEmpty())throw new ApiException(404,"NOT_FOUND");return json(rows.getFirst());}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> tasks(LocalDate date,int page,int size){date(date);page(page,size);return Map.of("items",jdbc.queryForList(SELECT+" WHERE t.taskDate=? ORDER BY CASE t.status WHEN 'PENDING' THEN 0 ELSE 1 END,t.id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",date,(long)page*size,size).stream().map(this::json).toList(),"page",page,"size",size,"total",jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Task WHERE taskDate=?",Long.class,date));}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> month(String value){YearMonth month;try{month=value==null?YearMonth.from(dates.today()):YearMonth.parse(value);}catch(Exception e){throw new ApiException(400,"INVALID_INPUT");}date(month.atDay(1));var first=month.atDay(1);var last=month.atEndOfMonth();var reservations=jdbc.queryForList("SELECT d.visitDate,COUNT_BIG(*) count FROM dbo.Reservation r JOIN dbo.VisitDay d ON d.id=r.visitDayId WHERE d.visitDate BETWEEN ? AND ? AND r.status='CONFIRMED' GROUP BY d.visitDate",first,last);var tasks=jdbc.queryForList("SELECT taskDate,COUNT_BIG(*) count,SUM(CASE WHEN status='PENDING' THEN CAST(1 AS bigint) ELSE 0 END) pending FROM dbo.Task WHERE taskDate BETWEEN ? AND ? GROUP BY taskDate",first,last);var days=new ArrayList<Map<String,Object>>();for(int n=1;n<=month.lengthOfMonth();n++){String d=month.atDay(n).toString();var r=reservations.stream().filter(x->x.get("visitDate").toString().equals(d)).findFirst();var t=tasks.stream().filter(x->x.get("taskDate").toString().equals(d)).findFirst();days.add(Map.of("date",d,"reservations",r.map(x->x.get("count")).orElse(0L),"tasks",t.map(x->x.get("count")).orElse(0L),"pendingTasks",t.map(x->x.get("pending")).orElse(0L)));}return Map.of("month",month.toString(),"serverDate",dates.today().toString(),"days",days);}
    private void actorLock(long actor){jdbc.queryForObject("SELECT id FROM dbo.AppUser WITH(UPDLOCK,HOLDLOCK) WHERE id=?",Long.class,actor);}
    private byte[] version(String value){try{if(value==null)throw new IllegalArgumentException();var v=Base64.getDecoder().decode(value);if(v.length!=8)throw new IllegalArgumentException();return v;}catch(Exception e){throw new ApiException(400,"INVALID_INPUT");}}
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> save(Long id,Save request,String key,long actor){validate(request);date(request.taskDate());actorLock(actor);String operation=id==null?"TASK_CREATE:"+actor:"TASK_EDIT:"+actor+":"+id;var replay=keys.read(operation,key,request);if(replay!=null)return replay;
        if(id==null){jdbc.update("INSERT dbo.Task(taskDate,title,description,status,authorId,updatedBy) VALUES(?,?,?,?,?,?)",request.taskDate(),request.title().trim(),request.description().trim(),request.status(),actor,actor);id=jdbc.queryForObject("SELECT MAX(id) FROM dbo.Task WHERE authorId=?",Long.class,actor);}else{jdbc.queryForList("SELECT id FROM dbo.Task WITH(UPDLOCK,HOLDLOCK) WHERE id=?",id);detail(id);if(jdbc.update("UPDATE dbo.Task SET taskDate=?,title=?,description=?,status=?,updatedBy=?,updatedAt=SYSUTCDATETIME() WHERE id=? AND version=?",request.taskDate(),request.title().trim(),request.description().trim(),request.status(),actor,id,version(request.version()))!=1)throw new ApiException(409,"VERSION_CONFLICT");}
        var result=detail(id);keys.save(operation,key,request,result,operation.startsWith("TASK_CREATE:")?201:200);return result;
    }
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Map<String,Object> delete(long id,Delete request,String key,long actor){validate(request);actorLock(actor);String operation="TASK_DELETE:"+actor+":"+id;var replay=keys.read(operation,key,request);if(replay!=null)return replay;jdbc.queryForList("SELECT id FROM dbo.Task WITH(UPDLOCK,HOLDLOCK) WHERE id=?",id);detail(id);if(jdbc.update("DELETE dbo.Task WHERE id=? AND version=?",id,version(request.version()))!=1)throw new ApiException(409,"VERSION_CONFLICT");var result=new LinkedHashMap<String,Object>();result.put("id",Long.toString(id));result.put("status","DELETED");keys.save(operation,key,request,result,operation.startsWith("TASK_CREATE:")?201:200);return result;}
}
