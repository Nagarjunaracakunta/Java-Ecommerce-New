package com.fooddelivery.apigateway.filter;

import com.fooddelivery.apigateway.config.RouteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)  // runs after auth/rate-limiting filters (added in later steps)
public class RoutingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RoutingFilter.class);

    private final WebClient webClient;
    private final RouteProperties routeProperties;

    public RoutingFilter(WebClient webClient, RouteProperties routeProperties) {
        this.webClient = webClient;
        this.routeProperties = routeProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Find the first route whose prefix matches the request path
        String targetBase = routeProperties.getRoutes().entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (targetBase == null) {
            log.debug("No route matched for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        // Preserve the full path and query string on the target URL
        String rawQuery = exchange.getRequest().getURI().getRawQuery();
        String targetUrl = targetBase + path + (rawQuery != null ? "?" + rawQuery : "");
        URI targetUri = URI.create(targetUrl);

        log.debug("Routing {} {} → {}", exchange.getRequest().getMethod(), path, targetUri);

        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(targetUri)
                .headers(headers -> {
                    headers.addAll(exchange.getRequest().getHeaders());
                    headers.remove(HttpHeaders.HOST);  // remove client's Host header — backend has its own
                })
                .body(exchange.getRequest().getBody(), DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(clientResponse.statusCode());
                    response.getHeaders().addAll(clientResponse.headers().asHttpHeaders());
                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                });
    }
}