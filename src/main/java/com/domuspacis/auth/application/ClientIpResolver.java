package com.domuspacis.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Utility for resolving client IP addresses from HTTP requests.
 *
 * Security considerations:
 * - X-Forwarded-For and X-Real-IP headers can be spoofed by clients
 * - Only trust these headers if the application is behind a trusted proxy
 * - By default, uses request.getRemoteAddr() which is set by the server
 * - Can be configured to trust proxy headers when deployed behind a known proxy
 */
@Component
public class ClientIpResolver {

    /**
     * Whether to trust X-Forwarded-For and X-Real-IP headers.
     * Should only be enabled if the application is behind a trusted proxy
     * that strips and re-sets these headers (e.g., Vercel, nginx, AWS ALB).
     */
    @Value("${app.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    /**
     * Resolves the client IP address from the request.
     *
     * If trustProxyHeaders is enabled, uses X-Forwarded-For or X-Real-IP headers.
     * Otherwise, uses request.getRemoteAddr() which is set by the servlet container
     * and cannot be spoofed by the client.
     *
     * @param request the HTTP servlet request
     * @return the client IP address
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (!trustProxyHeaders) {
            // Secure default: use the IP set by the server, not client-controlled headers
            return request.getRemoteAddr();
        }

        // Trust proxy headers - only enable if you control the proxy infrastructure
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2, ...
            // The first IP is the original client
            String[] ips = xf.split(",");
            return ips[0].trim();
        }

        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }

        // Fallback to remote address if headers are not present
        return request.getRemoteAddr();
    }
}