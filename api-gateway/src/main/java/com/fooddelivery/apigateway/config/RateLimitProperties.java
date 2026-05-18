package com.fooddelivery.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    // key = path prefix, value = max requests per minute per IP
    private Map<String, Integer> routes = new LinkedHashMap<>();

    // applied when no route prefix matches
    private int defaultRequestsPerMinute = 60;

    public Map<String, Integer> getRoutes() { return routes; }
    public void setRoutes(Map<String, Integer> routes) { this.routes = routes; }

    public int getDefaultRequestsPerMinute() { return defaultRequestsPerMinute; }
    public void setDefaultRequestsPerMinute(int defaultRequestsPerMinute) {
        this.defaultRequestsPerMinute = defaultRequestsPerMinute;
    }
}