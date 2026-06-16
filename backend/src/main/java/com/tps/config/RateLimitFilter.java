package com.tps.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tps.dto.ApiResponse;
import com.tps.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.auth.max-requests:20}")
    private int authMaxRequests;

    @Value("${app.rate-limit.auth.window-seconds:300}")
    private long authWindowSeconds;

    @Value("${app.rate-limit.upload.max-requests:10}")
    private int uploadMaxRequests;

    @Value("${app.rate-limit.upload.window-seconds:600}")
    private long uploadWindowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            applyRateLimitIfNeeded(request);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.fail(e.getCode(), e.getMessage()));
        }
    }

    private void applyRateLimitIfNeeded(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientKey = request.getRemoteAddr();

        if ("POST".equals(method) && path.startsWith("/api/auth/")) {
            enforceLimit("auth:" + path + ":" + clientKey, authMaxRequests, authWindowSeconds,
                    "认证请求过于频繁，请稍后再试");
            return;
        }

        if ("POST".equals(method) && "/api/files/upload".equals(path)) {
            enforceLimit("upload:" + clientKey, uploadMaxRequests, uploadWindowSeconds,
                    "上传过于频繁，请稍后再试");
        }
    }

    private void enforceLimit(String key, int maxRequests, long windowSeconds, String message) {
        Instant now = Instant.now();
        Duration window = Duration.ofSeconds(windowSeconds);
        Counter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new Counter(new AtomicInteger(1), now.plus(window));
            }
            existing.requests.incrementAndGet();
            return existing;
        });
        if (counter.requests.get() > maxRequests) {
            throw BusinessException.tooManyRequests(message);
        }
    }

    private static final class Counter {
        private final AtomicInteger requests;
        private final Instant expiresAt;

        private Counter(AtomicInteger requests, Instant expiresAt) {
            this.requests = requests;
            this.expiresAt = expiresAt;
        }
    }
}
