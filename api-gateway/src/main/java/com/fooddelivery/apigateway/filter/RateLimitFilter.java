package com.fooddelivery.apigateway.filter;

import com.fooddelivery.apigateway.config.RateLimitProperties;
import com.fooddelivery.apigateway.ratelimit.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(-2)  // before JwtAuthFilter (-1) — drop abusive traffic before touching JWT
public class RateLimitFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties props;

    // key = "clientIp:routePrefix" — one bucket per (client, route)
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clientIp    = resolveClientIp(exchange);
        String path        = exchange.getRequest().getURI().getPath();
        String routePrefix = matchedPrefix(path);
        int    limit       = limitForPrefix(routePrefix);

        String bucketKey   = clientIp + ":" + routePrefix;
        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(limit));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded — client: {}, route: {}, limit: {}/min", clientIp, routePrefix, limit);
            return tooManyRequests(exchange, limit);
        }

        log.debug("Rate-limit pass — client: {}, path: {}", clientIp, path);
        return chain.filter(exchange);
    }

    // Returns the first matching route prefix, or "default" when none match
    private String matchedPrefix(String path) {
        return props.getRoutes().keySet().stream()
                .filter(path::startsWith)
                .findFirst()
                .orElse("default");
    }

    private int limitForPrefix(String prefix) {
        return props.getRoutes().getOrDefault(prefix, props.getDefaultRequestsPerMinute());
    }

    // Respects X-Forwarded-For so clients behind a load balancer are identified correctly
    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int limit) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.getResponse().getHeaders().set("X-Rate-Limit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().set("Retry-After", "60");
        String body = "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded — try again later\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}