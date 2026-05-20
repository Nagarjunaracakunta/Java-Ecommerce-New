package com.fooddelivery.apigateway.filter;

import com.fooddelivery.apigateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(-1)  // runs before RoutingFilter (LOWEST_PRECEDENCE - 10)
public class JwtAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    // Routes that do not require a JWT — checked as (method, path-prefix) pairs
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/auth/login"),
            new PublicRoute(HttpMethod.POST, "/auth/register"),
            new PublicRoute(HttpMethod.GET,  "/products"),  // all GET /products/** are public
            new PublicRoute(HttpMethod.GET,  "/actuator")   // Prometheus scrapes /actuator/prometheus
    );

    private record PublicRoute(HttpMethod method, String pathPrefix) {
        boolean matches(HttpMethod m, String path) {
            return this.method.equals(m) && path.startsWith(this.pathPrefix);
        }
    }

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path   = exchange.getRequest().getURI().getPath();

        if (isPublic(method, path)) {
            log.debug("Public route {} {} — skipping JWT check", method, path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing/malformed Authorization header for {} {}", method, path);
            return unauthorized(exchange, "Authorization header missing or not Bearer");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            log.debug("Invalid or expired JWT for {} {}", method, path);
            return unauthorized(exchange, "Token invalid or expired");
        }

        String username = jwtUtil.extractUsername(token);
        String role     = jwtUtil.extractRole(token);

        log.debug("JWT valid for {} {} — user: {}, role: {}", method, path, username, role);

        // Mutate the request to add identity headers — RoutingFilter forwards all headers to the backend
        ServerWebExchange enriched = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.set("X-Username",  username);
                    h.set("X-User-Role", role);
                }))
                .build();

        return chain.filter(enriched);
    }

    private boolean isPublic(HttpMethod method, String path) {
        return PUBLIC_ROUTES.stream().anyMatch(r -> r.matches(method, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}