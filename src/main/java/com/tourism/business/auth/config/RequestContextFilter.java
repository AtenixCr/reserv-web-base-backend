package com.tourism.business.auth.config;
import com.tourism.business.audit.service.AuditContext;
import com.tourism.business.auth.repository.UserRepository;
import com.tourism.business.auth.repository.UserRepository.Account;
import com.tourism.business.common.ApiErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
public class RequestContextFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final UserRepository users;
    private final ObjectMapper mapper;
    public RequestContextFilter(Clock clock, UserRepository users, ObjectMapper mapper) { this.clock = clock; this.users = users; this.mapper = mapper; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        var trace = UUID.randomUUID();
        request.setAttribute("traceId", trace.toString()); response.setHeader("X-Request-ID", trace.toString()); response.setHeader("Cache-Control", "no-store");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Account account = auth != null && auth.getPrincipal() instanceof Account a ? a : null;
        var session = request.getSession(false);
        if (account != null && (session == null || !(session.getAttribute("authenticatedAt") instanceof Long at) || clock.millis() - at >= 43200000L || !users.isCurrent(account))) {
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext(); ApiErrors.write(mapper, request, response, 401, "SESSION_EXPIRED"); return;
        }
        try (var ignored = AuditContext.open(account == null ? null : account.id, trace)) { chain.doFilter(request, response); }
    }
}
