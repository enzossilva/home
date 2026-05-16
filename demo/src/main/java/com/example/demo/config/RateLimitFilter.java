package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Máximo de tentativas por IP em 15 minutos
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    private static final java.util.Set<String> RATE_LIMITED_PATHS = java.util.Set.of(
            "/users/login",
            "/users/register",
            "/users/reset-request"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("POST".equals(request.getMethod()) && RATE_LIMITED_PATHS.contains(path)) {
            String ip = getClientIp(request);
            if (isBlocked(ip)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"erro\":\"Muitas tentativas. Aguarde 15 minutos.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isBlocked(String ip) {
        long now = System.currentTimeMillis();
        attempts.compute(ip, (key, val) -> {
            if (val == null) return new long[]{now, 1};
            if (now - val[0] > WINDOW_MS) return new long[]{now, 1};
            val[1]++;
            return val;
        });
        long[] data = attempts.get(ip);
        return data != null && data[1] > MAX_ATTEMPTS;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
