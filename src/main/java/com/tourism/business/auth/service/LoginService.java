package com.tourism.business.auth.service;
import com.tourism.business.auth.repository.UserRepository.Account;
import com.tourism.business.common.ApiException;
import com.tourism.business.audit.service.AuditContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
@Service
public class LoginService {
    private record Attempts(int count, Instant expires) {}
    private final Map<String, Attempts> attempts = new HashMap<>();
    private final AuthenticationManager manager;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int limit;
    private final int window;
    public LoginService(AuthenticationManager manager, JdbcTemplate jdbc, Clock clock,
        @Value("${security.login.max-failures:5}") int limit, @Value("${security.login.window-seconds:900}") int window) {
        this.manager = manager; this.jdbc = jdbc; this.clock = clock; this.limit = limit; this.window = window;
    }
    public synchronized Authentication login(String username, String password, String ip) {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> !entry.getValue().expires.isAfter(now));
        String userKey = "user:" + username.trim().toLowerCase(Locale.ROOT);
        String ipKey = "ip:" + ip;
        for (String key : new String[]{userKey, ipKey}) {
            Attempts value = attempts.get(key);
            if (value != null && value.count >= limit) throw new ApiException(429, "RATE_LIMITED", Map.of("retryAfter", Long.toString(Math.max(1, Duration.between(now, value.expires).toSeconds() + 1))));
        }
        if (attempts.size() > 20000) throw new ApiException(429, "RATE_LIMITED", Map.of("retryAfter", "60"));
        try {
            var result = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username.trim(), password));
            attempts.remove(userKey);
            event("LOGIN", ((Account) result.getPrincipal()).id, "SUCCESS");
            return result;
        } catch (AuthenticationException error) {
            for (String key : new String[]{userKey, ipKey}) attempts.compute(key, (k, v) -> v == null ? new Attempts(1, now.plusSeconds(window)) : new Attempts(v.count + 1, v.expires));
            event("LOGIN", null, "FAILURE");
            throw new ApiException(401, "INVALID_CREDENTIALS");
        }
    }
    public void event(String type, Long actor, String outcome) {
        var context = AuditContext.current();
        jdbc.update("INSERT dbo.ActivityEvent(eventType,actorId,requestId,outcome) VALUES (?,?,?,?)", type, actor, context == null ? null : context.requestId().toString(), outcome);
    }
}
