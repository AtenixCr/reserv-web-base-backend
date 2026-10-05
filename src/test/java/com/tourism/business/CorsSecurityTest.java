package com.tourism.business;

import com.tourism.business.auth.config.CorsConfiguration;
import com.tourism.business.auth.config.SecurityConfiguration;
import com.tourism.business.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.test.context.support.TestPropertySourceUtils;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorsSecurityTest {
    @Configuration
    @EnableWebSecurity
    static class Dependencies {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }

    @Test
    void trustedPreflightsPassBeforeAuthenticationButCsrfAndOriginChecksRemain() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "app.cors.allowed-origins=https://reserva.admin.atenix.net,https://reserva.cliente.atenix.net");
            context.register(Dependencies.class, SecurityConfiguration.class, CorsConfiguration.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
            for (String origin : new String[]{"https://reserva.admin.atenix.net", "https://reserva.cliente.atenix.net"}) {
                mvc.perform(options("/api/v1/auth/login").header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "content-type,x-csrf-token,idempotency-key"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
            }
            mvc.perform(options("/api/v1/auth/login").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
            mvc.perform(post("/api/v1/auth/login").header("Origin", "https://reserva.admin.atenix.net")
                .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
            mvc.perform(get("/api/v1/auth/me").header("Origin", "https://reserva.admin.atenix.net"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://reserva.admin.atenix.net"));
        }
    }
}
