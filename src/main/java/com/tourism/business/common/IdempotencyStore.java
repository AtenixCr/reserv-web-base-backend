package com.tourism.business.common;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
@Component
public class IdempotencyStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public IdempotencyStore(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    private String hash(String value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public String key(String value) {if(value==null||!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"))throw new ApiException(400,"IDEMPOTENCY_REQUIRED");return hash(value.toLowerCase(Locale.ROOT));}
    public Map<String,Object> read(String operation,String key,Object request) {
        var rows=jdbc.queryForList("SELECT requestHash,responseBody FROM dbo.IdempotencyRecord WITH(UPDLOCK,HOLDLOCK) WHERE operation=? AND keyHash=?",operation,key(key));
        if(rows.isEmpty())return null;
        if(!hash(mapper.writeValueAsString(request)).equals(rows.getFirst().get("requestHash")))throw new ApiException(409,"IDEMPOTENCY_CONFLICT");
        return mapper.readValue((String)rows.getFirst().get("responseBody"),new tools.jackson.core.type.TypeReference<Map<String,Object>>(){});
    }
    public void save(String operation,String key,Object request,Map<String,Object> result) {save(operation,key,request,result,201);}
    public void save(String operation,String key,Object request,Map<String,Object> result,int status) {jdbc.update("INSERT dbo.IdempotencyRecord(operation,keyHash,requestHash,responseStatus,responseBody,expiresAt) VALUES(?,?,?,?,?,DATEADD(hour,49,SYSUTCDATETIME()))",operation,key(key),hash(mapper.writeValueAsString(request)),status,mapper.writeValueAsString(result));}
}
