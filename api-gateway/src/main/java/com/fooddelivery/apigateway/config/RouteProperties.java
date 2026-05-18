package com.fooddelivery.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "gateway")
public class RouteProperties {

    // key = path prefix (e.g. /auth), value = backend base URL (e.g. http://localhost:8081)
    private Map<String, String> routes = new LinkedHashMap<>();

    public Map<String, String> getRoutes() { return routes; }

    public void setRoutes(Map<String, String> routes) { this.routes = routes; }
}
