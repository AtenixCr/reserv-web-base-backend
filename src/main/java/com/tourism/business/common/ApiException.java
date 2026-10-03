package com.tourism.business.common;
import java.util.Map;
public class ApiException extends RuntimeException {
    public final int status;
    public final String code;
    public final Map<String, String> fields;
    public ApiException(int status, String code) { this(status, code, Map.of()); }
    public ApiException(int status, String code, Map<String, String> fields) {
        super(code); this.status = status; this.code = code; this.fields = fields;
    }
}
