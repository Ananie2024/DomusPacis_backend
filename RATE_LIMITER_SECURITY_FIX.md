# Rate Limiter Security Fix - IP Spoofing Prevention

## Vulnerability

The rate limiter filters (`LoginRateLimitFilter` and `PasswordResetRateLimitFilter`) were vulnerable to IP spoofing attacks via the `X-Forwarded-For` and `X-Real-IP` headers. These headers are client-controlled and can be arbitrarily set by attackers to bypass rate limiting.

### Attack Scenario

An attacker could send multiple requests with different `X-Forwarded-For` header values:
```bash
curl -X POST http://api/login \
  -H "X-Forwarded-For: 1.2.3.4" \
  -d '{"email":"user@example.com","password":"pass"}'

curl -X POST http://api/login \
  -H "X-Forwarded-For: 5.6.7.8" \
  -d '{"email":"user@example.com","password":"pass"}'

# Each request gets a fresh rate-limit bucket
```

This effectively bypassed the IP-based rate limiting, leaving only the per-account DB lockout as protection.

## Solution

### 1. Created `ClientIpResolver` Component

**File:** `src/main/java/com/domuspacis/auth/application/ClientIpResolver.java`

A centralized IP resolution utility that:
- **By default**, uses `request.getRemoteAddr()` which is set by the servlet container and cannot be spoofed
- **Optionally** trusts `X-Forwarded-For` and `X-Real-IP` headers when `app.trust-proxy-headers` is enabled
- Only enable proxy header trust if you control the proxy infrastructure (Vercel, nginx, AWS ALB) that strips and re-sets these headers

### 2. Updated Rate Limiter Filters

**Files:**
- `src/main/java/com/domuspacis/auth/application/LoginRateLimitFilter.java`
- `src/main/java/com/domuspacis/auth/application/PasswordResetRateLimitFilter.java`

Both filters now:
- Inject `ClientIpResolver` instead of implementing their own IP resolution
- Use `clientIpResolver.resolveClientIp(request)` for consistent, secure IP extraction
- Removed the vulnerable `resolveClientIp()` methods that unconditionally trusted client headers

### 3. Added Configuration Property

**File:** `src/main/resources/application.yml`

```yaml
app:
  trust-proxy-headers: false  # Default: secure, uses request.getRemoteAddr()
```

### 4. Added Security Test

**File:** `src/test/java/com/domuspacis/auth/AuthIntegrationTest.java`

Added test `rateLimit_cannotBeBypassed_withSpoofedHeaders()` that:
- Sends multiple requests with different spoofed `X-Forwarded-For` headers
- Verifies that rate limiting still applies (4th request returns 429)
- Confirms the fix prevents IP spoofing attacks

## Deployment Considerations

### Default Configuration (Secure)

With `app.trust-proxy-headers: false` (default):
- Rate limiting uses `request.getRemoteAddr()`
- Immune to `X-Forwarded-For` spoofing
- Works correctly for direct deployments and most cloud platforms

### Behind a Trusted Proxy

If deploying behind a proxy that strips and re-sets `X-Forwarded-For` (e.g., Vercel, nginx, AWS ALB):

1. Set `app.trust-proxy-headers: true` in your deployment environment
2. Ensure your proxy is configured to:
   - Strip incoming `X-Forwarded-For` headers from clients
   - Add its own `X-Forwarded-For` header with the actual client IP

Example nginx configuration:
```nginx
location /api/ {
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_pass http://localhost:8080;
}
```

## Security Impact

### Before Fix
- ❌ Rate limiting could be bypassed via header spoofing
- ❌ Each spoofed IP got a fresh rate-limit bucket
- ⚠️ Only per-account DB lockout provided real protection

### After Fix
- ✅ Rate limiting uses server-determined IP by default
- ✅ Immune to client-side header spoofing
- ✅ Optional proxy support with explicit opt-in
- ✅ Both login and password reset endpoints protected

## Testing

The fix includes a test that verifies:
1. Rate limiting works normally (3 requests allowed, 4th blocked)
2. Spoofed `X-Forwarded-For` headers cannot bypass the limit
3. All requests are tracked by the actual client IP

Run tests (requires Docker for integration tests):
```bash
./mvnw test -Dtest=AuthIntegrationTest#rateLimit_cannotBeBypassed_withSpoofedHeaders
```

## Files Modified

1. **Created:** `src/main/java/com/domuspacis/auth/application/ClientIpResolver.java`
2. **Modified:** `src/main/java/com/domuspacis/auth/application/LoginRateLimitFilter.java`
3. **Modified:** `src/main/java/com/domuspacis/auth/application/PasswordResetRateLimitFilter.java`
4. **Modified:** `src/main/resources/application.yml`
5. **Modified:** `src/test/java/com/domuspacis/auth/AuthIntegrationTest.java`

## References

- [OWASP: IP Spoofing](https://owasp.org/www-community/attacks/IP_Spoofing)
- [Spring Boot: Behind a Proxy Server](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.behind-a-proxy-server)
- [Bucket4j Documentation](https://github.com/vladimir-bukhtoyarov/bucket4j)