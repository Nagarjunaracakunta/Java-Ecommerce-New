package com.fooddelivery.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-4)  // after logging (-10), before rate limiting (-2)
public class CorsFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorsFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        // Add CORS headers to every response — even 401/429 need them so browsers can read the body
        headers.set("Access-Control-Allow-Origin",   "*");
        headers.set("Access-Control-Allow-Methods",  "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        headers.set("Access-Control-Allow-Headers",  "Authorization, Content-Type, X-Requested-With");
        headers.set("Access-Control-Expose-Headers", "X-Rate-Limit-Limit, Retry-After");
        headers.set("Access-Control-Max-Age",        "3600");

        // Preflight: browser sends OPTIONS before the real request — respond 200 immediately,
        // do NOT forward to the backend (it doesn't know about CORS)
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            log.debug("CORS preflight for {}", exchange.getRequest().getURI().getPath());
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }
}