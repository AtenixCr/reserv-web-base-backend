package com.tourism.business.auth.controller;
import com.tourism.business.auth.repository.UserRepository.Account;
import com.tourism.business.auth.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record Login(@NotBlank @Size(max=80) String username, @NotBlank @Size(max=200) String password) {}
    public record Identity(String id, String username, String name, String role) {}
    private final LoginService login;
    private final HttpSessionSecurityContextRepository contexts;
    private final HttpSessionCsrfTokenRepository csrf;
    private final Clock clock;
    public AuthController(LoginService login, HttpSessionSecurityContextRepository contexts, HttpSessionCsrfTokenRepository csrf, Clock clock) {
        this.login = login; this.contexts = contexts; this.csrf = csrf; this.clock = clock;
    }
    private Identity identity(Authentication auth) {
        var account = (Account) auth.getPrincipal();
        return new Identity(Long.toString(account.id), account.getUsername(), account.displayName, account.role);
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }
    @GetMapping("/me") Identity me(Authentication auth) { return identity(auth); }
    @PostMapping("/login") Identity login(@Valid @RequestBody Login body, HttpServletRequest request, HttpServletResponse response) {
        var auth = login.login(body.username(), body.password(), request.getRemoteAddr());
        if (request.getSession(false) != null) request.changeSessionId();
        request.getSession().setAttribute("authenticatedAt", clock.millis()); request.getSession().setMaxInactiveInterval(1800);
        csrf.saveToken(null, request, response);
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
        return identity(auth);
    }
    @PostMapping("/logout") Map<String,String> logout(Authentication auth, HttpServletRequest request) {
        login.event("LOGOUT", ((Account)auth.getPrincipal()).id, "SUCCESS");
        var session = request.getSession(false); if (session != null) session.invalidate();
        SecurityContextHolder.clearContext(); return Map.of("status", "SIGNED_OUT");
    }
}
