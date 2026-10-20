package io.github.gabrielhdias.financeManager.config.security;

import io.github.gabrielhdias.financeManager.api.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/** Per-process fixed windows. Only the socket peer is trusted; bounded memory. */
public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long until, int used) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final ObjectMapper mapper;
    private final Clock clock;
    private final int loginLimit, registrationLimit, capacity;
    private final long windowSeconds;
    private final LongAdder rejected = new LongAdder();
    public AuthRateLimitFilter(ObjectMapper mapper, Clock clock, int loginLimit,
                              int registrationLimit, long windowSeconds, int capacity) {
        if (loginLimit < 1 || registrationLimit < 1 || windowSeconds < 1 || capacity < 1)
            throw new IllegalArgumentException("Rate limit configuration must be positive");
        this.mapper = mapper; this.clock = clock; this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit; this.windowSeconds = windowSeconds; this.capacity = capacity;
    }
    public long rejectedCount() { return rejected.sum(); }
    private synchronized long retryAfter(String key, int limit) {
        long now = clock.instant().getEpochSecond();
        Window window = windows.get(key);
        if (window == null || window.until <= now) {
            if (windows.size() >= capacity) windows.entrySet().removeIf(entry -> entry.getValue().until <= now);
            if (window == null && windows.size() >= capacity) return windowSeconds;
            windows.put(key, new Window(now + windowSeconds, 1)); return 0;
        }
        if (window.used >= limit) return Math.max(1, window.until - now);
        windows.put(key, new Window(window.until, window.used + 1)); return 0;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI();
        if (!"POST".equals(request.getMethod()) || !("/auth/login".equals(path) || "/users".equals(path))) {
            chain.doFilter(request, response); return;
        }
        long retry = retryAfter(path + ":" + request.getRemoteAddr(), "/users".equals(path) ? registrationLimit : loginLimit);
        if (retry == 0) { chain.doFilter(request, response); return; }
        rejected.increment();
        response.setStatus(429); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", Long.toString(retry));
        mapper.writeValue(response.getOutputStream(), new ApiErrorResponse(Instant.now(clock), 429,
            "Too Many Requests", "Muitas tentativas. Aguarde e tente novamente.", request.getRequestURI(), Map.of()));
    }
}
