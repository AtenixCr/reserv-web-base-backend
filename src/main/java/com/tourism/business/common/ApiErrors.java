package com.tourism.business.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class ApiErrors {
    
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> uploadTooLarge(HttpServletRequest request) {
        return ResponseEntity.status(413).body(
                body("FILE_TOO_LARGE", Map.of(), request));
    }

    public static Map<String, Object> body(String code, Map<String, String> fields, HttpServletRequest request) {
        Object trace = request.getAttribute("traceId");
        return Map.of("code", code, "message", code, "fieldErrors", fields, "traceId",
                trace == null ? UUID.randomUUID().toString() : trace);
    }

    public static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response, int status,
            String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), body(code, Map.of(), request));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> business(ApiException error, HttpServletRequest request) {
        var builder = ResponseEntity.status(error.status);
        if (error.status == 429)
            builder.header("Retry-After", error.fields.getOrDefault("retryAfter", "900"));
        return builder.body(body(error.code, error.fields, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.put(field.getField(), "INVALID_FIELD"));
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", fields, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> malformed(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", Map.of(), request));
    }

    @ExceptionHandler({ org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class })
    ResponseEntity<?> parameter(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", Map.of(), request));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> method(HttpServletRequest request) {
        return ResponseEntity.status(405).body(body("METHOD_NOT_ALLOWED", Map.of(), request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(body("UNEXPECTED_ERROR", Map.of(), request));
    }
}
