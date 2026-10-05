package com.tourism.business.auth.config;
import com.tourism.business.auth.repository.UserRepository;
import com.tourism.business.common.ApiErrors;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;
@Configuration
public class SecurityConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean HttpSessionSecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean HttpSessionCsrfTokenRepository csrfRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean AuthenticationManager authenticationManager(UserRepository users) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return new ProviderManager(provider);
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, HttpSessionSecurityContextRepository contexts,
        HttpSessionCsrfTokenRepository csrf, Clock clock, UserRepository users, ObjectMapper mapper,
        CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            .securityContext(context -> context.securityContextRepository(contexts))
            .csrf(config -> config.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/v1/public/reservation-quotes", "/api/v1/public/reservations")).logout(config -> config.disable())
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/v1/admissions/*/corrections", "/api/v1/admissions/*/correction-quotes", "/api/v1/cash-sessions").hasRole("ADMIN")
                .requestMatchers("/api/v1/cash-sessions/current", "/api/v1/admissions", "/api/v1/admissions/**", "/api/v1/reservations", "/api/v1/reservations/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/health", "/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/public/business", "/api/v1/public/availability", "/api/v1/public/reservation-quotes", "/api/v1/public/reservations").permitAll()
                .requestMatchers("/api/v1/reservations/*/cancel", "/api/v1/reservations/*/no-show").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/api/v1/settings", "/api/v1/rate-plans", "/api/v1/exchange-rates", "/api/v1/expenses", "/api/v1/refunds", "/api/v1/adjustments", "/api/v1/adjustments/**", "/api/v1/closures/**", "/api/v1/finance/**", "/api/v1/cash-sessions/**", "/api/v1/activity", "/api/v1/activity/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout", "/api/v1/dashboard", "/api/v1/calendar", "/api/v1/tasks", "/api/v1/tasks/**").hasAnyRole("ADMIN", "STAFF")
                .anyRequest().denyAll())
            .addFilterAfter(new RequestContextFilter(clock, users, mapper), SecurityContextHolderFilter.class)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> ApiErrors.write(mapper, request, response, 401, "AUTHENTICATION_REQUIRED"))
                .accessDeniedHandler((request, response, error) -> ApiErrors.write(mapper, request, response, 403,
                    error instanceof org.springframework.security.web.csrf.CsrfException ? "CSRF_INVALID" : "FORBIDDEN")))
            .build();
    }
}
