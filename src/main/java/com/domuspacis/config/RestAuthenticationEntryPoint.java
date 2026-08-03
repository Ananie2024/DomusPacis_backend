package com.domuspacis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * REST-friendly {@link AuthenticationEntryPoint}.
 *
 * <p>By default Spring Security responds to an unauthenticated request by
 * redirecting to an HTML login page.  For a JSON API that is wrong — the
 * client expects a structured error body, not a 302 redirect to a web page.
 * This entry point writes a JSON error with HTTP 401 instead.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                Map.of(
                        "status", "error",
                        "message", "Authentication required",
                        "path", request.getRequestURI()
                ));
        // Flush so downstream filters (e.g. LoginRateLimitFilter) cannot
        // reset a buffered 401 to a 500 if they throw after the chain.
        response.getOutputStream().flush();
    }
}