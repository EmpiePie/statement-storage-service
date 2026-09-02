package za.co.statements.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the app as an OAuth2 resource server that validates Keycloak-issued
 * RS256 JWTs (via the realm JWKS resolved from {@code issuer-uri}).
 *
 * <p>Public routes: the UI pages, auth endpoints, token-based public download,
 * Swagger, and actuator health. Everything under {@code /api/statements/**}
 * requires a valid Bearer token.
 *
 * <p>Disabled under the {@code test} profile — see {@link TestSecurityConfig}.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public UI pages + static assets
                        .requestMatchers("/", "/login", "/register", "/statements",
                                "/favicon.ico", "/css/**", "/js/**").permitAll()
                        // Auth API (register/login) and token-based public download
                        .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                        // API docs
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/actuator/health").permitAll()
                        // Everything else (the statement API) requires a valid JWT
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
