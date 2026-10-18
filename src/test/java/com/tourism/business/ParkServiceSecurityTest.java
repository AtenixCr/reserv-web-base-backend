package com.tourism.business;

import com.tourism.business.auth.config.SecurityConfiguration;
import com.tourism.business.auth.repository.UserRepository;
import com.tourism.business.auth.repository.UserRepository.Account;
import com.tourism.business.business.controller.ParkServiceController;
import com.tourism.business.business.service.ParkServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParkServiceSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc
    static class Dependencies {
        @Bean UserRepository users() {
            var users = mock(UserRepository.class);
            when(users.isCurrent(any())).thenReturn(true);
            return users;
        }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean ParkServiceService services() { return mock(ParkServiceService.class); }
        @Bean ParkServiceController controller(ParkServiceService services) { return new ParkServiceController(services); }
        @Bean CsrfEndpoint csrfEndpoint() { return new CsrfEndpoint(); }
    }

    @org.springframework.web.bind.annotation.RestController
    static class CsrfEndpoint {
        @org.springframework.web.bind.annotation.GetMapping("/api/v1/auth/csrf")
        public java.util.Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
            return java.util.Map.of("token", token.getToken());
        }
    }

    private MockHttpSession session(String role) {
        var account = new Account(1L, "test", "unused", "Test", role, true);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account, null, account.getAuthorities()));
        var session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        session.setAttribute("authenticatedAt", System.currentTimeMillis());
        return session;
    }

    @Test void requiresAdminAndCsrfForCatalogAndUploads() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Dependencies.class, SecurityConfiguration.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
            mvc.perform(get("/api/v1/services")).andExpect(status().isUnauthorized());
            var staff = session("STAFF");
            mvc.perform(get("/api/v1/services").session(staff)).andExpect(status().isForbidden());
            // Upload remains forbidden even with a valid staff CSRF token.
            mvc.perform(get("/api/v1/auth/csrf").session(staff));
            var token = (org.springframework.security.web.csrf.CsrfToken) staff.getAttribute(
                org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.class.getName() + ".CSRF_TOKEN");
            org.junit.jupiter.api.Assertions.assertNotNull(token);
            mvc.perform(post("/api/v1/media").session(staff).header(token.getHeaderName(), token.getToken()))
                .andExpect(status().isForbidden());
            var admin = session("ADMIN");
            mvc.perform(get("/api/v1/services").session(admin)).andExpect(status().isOk());
            mvc.perform(post("/api/v1/services").session(admin).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        }
    }
}
