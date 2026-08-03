package com.domuspacis.config;

import com.domuspacis.auth.application.CorrelationIdFilter;
import com.domuspacis.auth.application.JwtAuthenticationFilter;
import com.domuspacis.auth.application.LoginRateLimitFilter;
import com.domuspacis.auth.application.PasswordResetRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * HTTP security configuration.
 *
 * Authentication infrastructure (UserDetailsService, AuthenticationProvider,
 * PasswordEncoder, AuthenticationManager) lives in {@link AuthConfig} to
 * prevent a circular dependency between this class and JwtAuthenticationFilter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final PasswordResetRateLimitFilter passwordResetRateLimitFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        //allow preflight/OPTIONs
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/password-reset/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Booking public read/create
                        .requestMatchers(HttpMethod.GET, "/api/v1/service-assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/availability").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu-items/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").permitAll()
                        // Role-restricted endpoints
                        .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/tax/**").hasAnyRole("ADMIN", "FINANCE", "MANAGER")
                        .requestMatchers("/api/v1/finance/reports/**").hasAnyRole("ADMIN", "FINANCE", "MANAGER")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/analytics/**").hasAnyRole("ADMIN", "MANAGER", "FINANCE")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(passwordResetRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));

        // Never allow wildcard with credentials — replace with explicit origin
        if (origins.contains("*")) {
            // Use setAllowedOriginPatterns instead of setAllowedOrigins so that
            // wildcard patterns are supported even with allowCredentials=true.
            // This covers production deployments plus common local-dev access
            // methods (localhost, 127.0.0.1, and LAN IPs like 192.168.x.x).
            configuration.setAllowedOriginPatterns(List.of(
                    "https://domuspaciskigali.vercel.app",
                    "https://Ananie2024-domuspacis.hf.space",
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "http://192.168.*.*:3000",
                    "http://10.*.*.*:3000"
            ));
        } else {
            configuration.setAllowedOriginPatterns(origins);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}