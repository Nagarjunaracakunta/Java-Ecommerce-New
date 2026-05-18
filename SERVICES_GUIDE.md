# Java Ecommerce — Services Guide
# api-gateway · auth-service · product-service · JWT · Spring Security · Validations · Design Patterns

---

## Table of Contents

**Multi-Module**
1. [Multi-Module Project Structure](#1-multi-module-project-structure)

**api-gateway**
2. [api-gateway — Overview and Design](#2-api-gateway--overview-and-design)
3. [api-gateway — Why WebFlux, Not WebMVC](#3-api-gateway--why-webflux-not-webmvc)
4. [api-gateway — Route Configuration](#4-api-gateway--route-configuration)
5. [api-gateway — RoutingFilter: The Proxy](#5-api-gateway--routingfilter-the-proxy)
6. [api-gateway — Step Roadmap](#6-api-gateway--step-roadmap)

**product-service**
7. [product-service — Overview and Structure](#7-product-service--overview-and-structure)
8. [product-service — Dependencies](#8-product-service--dependencies)
9. [product-service — Entity and Category Design](#9-product-service--entity-and-category-design)
10. [product-service — Validations (Complete Guide)](#10-product-service--validations-complete-guide)
11. [product-service — Role-Based Security](#11-product-service--role-based-security)
12. [product-service — JWT Flow: How Auth and Product Services Communicate](#12-product-service--jwt-flow-how-auth-and-product-services-communicate)
13. [product-service — Immutable Objects](#13-product-service--immutable-objects)
14. [product-service — Deep Copy vs Shallow Copy](#14-product-service--deep-copy-vs-shallow-copy)
15. [product-service — Fail-Fast vs Fail-Safe](#15-product-service--fail-fast-vs-fail-safe)
16. [product-service — Service Layer](#16-product-service--service-layer)
17. [product-service — Controller and Exception Handler](#17-product-service--controller-and-exception-handler)
18. [product-service — Testing Strategy](#18-product-service--testing-strategy)
19. [product-service — API Usage with curl](#19-product-service--api-usage-with-curl)

**auth-service**
20. [auth-service — Overview](#20-auth-service--overview)
21. [auth-service — Dependencies](#21-auth-service--dependencies)
22. [auth-service — Database Setup](#22-auth-service--database-setup)
23. [auth-service — Entity and Repository](#23-auth-service--entity-and-repository)
24. [auth-service — DTOs as Java Records](#24-auth-service--dtos-as-java-records)
25. [auth-service — JWT Implementation](#25-auth-service--jwt-implementation)
26. [auth-service — Security Filter](#26-auth-service--security-filter)
27. [auth-service — Security Config](#27-auth-service--security-config)
28. [auth-service — Service Layer](#28-auth-service--service-layer)
29. [auth-service — Controller and Exception Handler](#29-auth-service--controller-and-exception-handler)
30. [auth-service — Testing Strategy](#30-auth-service--testing-strategy)
31. [auth-service — Test Setup](#31-auth-service--test-setup)
32. [Creating the First Admin User](#32-creating-the-first-admin-user)
33. [auth-service — API Usage with curl](#33-auth-service--api-usage-with-curl)

**Errors**
34. [Common Errors and Fixes](#34-common-errors-and-fixes)

---

## 1. Multi-Module Project Structure

The root project is a Maven aggregator that owns all child modules.

```
Java-Ecommerce-New/           ← root aggregator (pom packaging)
├── pom.xml                   ← parent pom — shared plugins, properties
├── api-gateway/              ← child module (port 8080) — entry point
│   └── pom.xml
├── auth-service/             ← child module (port 8081)
│   └── pom.xml
└── product-service/          ← child module (port 8082)
    └── pom.xml
```

### Full architecture (startup order)
```
auth-service        (port 8081) — user login, JWT issue
product-service     (port 8082) — product CRUD
api-gateway         (port 8080) — routing, JWT validation, rate limiting  ← BUILT
cart-service        (port 8083)
order-service       (port 8084)
payment-service     (port 8085)
inventory-service   (port 8086)
notification-service(port 8087)
```

### Root pom.xml — module declaration
```xml
<packaging>pom</packaging>
<modules>
    <module>api-gateway</module>
    <module>auth-service</module>
    <module>product-service</module>
</modules>
```

### How child modules inherit plugins
- Root pom puts shared plugin config inside `<pluginManagement>` — defines config but does NOT run anything.
- Each child module activates it by declaring the plugin under `<build><plugins>` without repeating configuration.
- Version numbers and settings live in one place only.

### Build commands
```bash
# Build all modules from root
mvn clean package

# Build only one module
cd product-service && mvn clean package
```

---

## 2. api-gateway — Overview and Design

Runs on port 8080. Every client request hits the gateway first — it handles routing, JWT validation, and cross-cutting concerns so downstream services don't need to duplicate that logic.

```
api-gateway/
├── config/
│   ├── GatewayConfig.java         — WebClient bean (16 MB buffer for proxied responses)
│   ├── RateLimitProperties.java   — @ConfigurationProperties: rate-limit.routes map
│   └── RouteProperties.java       — @ConfigurationProperties: gateway.routes map
├── filter/
│   ├── RequestLoggingFilter.java  — @Order(-10) structured req/res logging + timing
│   ├── CorsFilter.java            — @Order(-4)  CORS headers + OPTIONS preflight
│   ├── RateLimitFilter.java       — @Order(-2)  per-IP token-bucket throttling
│   ├── JwtAuthFilter.java         — @Order(-1)  JWT validation + identity headers
│   └── RoutingFilter.java         — @Order(MAX) path-prefix proxy + error handling
├── ratelimit/
│   └── TokenBucket.java           — thread-safe token bucket (continuous refill)
├── util/
│   └── JwtUtil.java               — verify + extract only (no token generation)
└── src/main/resources/
    └── application.yml            — port 8080, routes, jwt.secret, rate limits, logging
```

### Complete filter chain

```
Incoming request :8080
  │
  ▼  @Order(-10)  RequestLoggingFilter  → logs "→ METHOD path client=IP"
  │  @Order(-4)   CorsFilter            → adds Access-Control-* headers
  │                                        OPTIONS preflight → 200 immediately
  │  @Order(-2)   RateLimitFilter       → token bucket per (IP, route prefix)
  │                                        bucket empty → 429 JSON
  │  @Order(-1)   JwtAuthFilter         → public route → pass through
  │                                        missing/bad token → 401 JSON
  │                                        valid token → mutate request, add headers
  │  @Order(MAX)  RoutingFilter         → prefix match → WebClient forward
  │                                        no match → 404
  │                                        backend down → 503 JSON
  │                                        gateway error → 502 JSON
  ▼  @Order(-10)  RequestLoggingFilter  → logs "← STATUS METHOD path Xms"
```

**Step-by-step build status:**

| Step | What it adds | Files | Status |
|---|---|---|---|
| 1 | Basic routing | RouteProperties, GatewayConfig, RoutingFilter | Done |
| 2 | JWT validation | JwtUtil, JwtAuthFilter | Done |
| 3 | Identity header forwarding | JwtAuthFilter (mutate) | Done |
| 4 | Rate limiting | RateLimitProperties, TokenBucket, RateLimitFilter | Done |
| 5 | CORS, logging, error format | CorsFilter, RequestLoggingFilter, RoutingFilter (onErrorResume) | Done |

---

## 3. api-gateway — Why WebFlux, Not WebMVC

The gateway uses `spring-boot-starter-webflux` (Netty, reactive). **Never add `spring-boot-starter-webmvc`** — the two stacks conflict on the same classpath.

| | WebMVC (Servlet) | WebFlux (Reactive) |
|---|---|---|
| Thread model | 1 thread per request | Event loop — 1 thread handles thousands |
| Blocking I/O | OK | Blocks the event loop — avoid |
| Filter type | `javax.servlet.Filter` / `OncePerRequestFilter` | `WebFilter` (returns `Mono<Void>`) |
| HTTP client | `RestTemplate` / `RestClient` | `WebClient` (non-blocking) |
| Best for | CRUD services with JPA | Gateways, proxies, streaming |

A gateway does almost nothing but forward requests — it spends all its time waiting for I/O. Reactive / non-blocking is the right model here.

**Why no Spring Cloud Gateway?**
Spring Cloud 2025.x compiled against different Spring Boot 4.x internal classes that were moved between patch versions, causing `ClassNotFoundException` on startup. The fix: drop Spring Cloud entirely. A `WebFilter` proxy with `WebClient` is only ~70 lines and has zero extra dependencies.

---

## 4. api-gateway — Route Configuration

Routes live in `application.yml` and are bound to `RouteProperties` via `@ConfigurationProperties`:

```yaml
gateway:
  routes:
    "[/auth]": http://localhost:8081
    "[/products]": http://localhost:8082
```

**Why bracket notation `"[/auth]"`?**
Spring Boot's relaxed binding normalizes map keys — a key like `/auth` has its `/` treated as a path separator and the key arrives empty or wrong. Bracket notation bypasses normalization and preserves the key exactly as written.

```java
@Component
@ConfigurationProperties(prefix = "gateway")
public class RouteProperties {
    private Map<String, String> routes = new LinkedHashMap<>();
    public Map<String, String> getRoutes() { return routes; }
    public void setRoutes(Map<String, String> routes) { this.routes = routes; }
}
```

To add a new service, add one line to `application.yml` — no code change needed:
```yaml
"[/orders]": http://localhost:8084
```

---

## 5. api-gateway — RoutingFilter: The Proxy

`RoutingFilter` implements `WebFilter` and runs at `LOWEST_PRECEDENCE - 10` — after auth/rate-limiting filters are added in later steps.

```java
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RoutingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Match the longest prefix in the route table
        String targetBase = routeProperties.getRoutes().entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);

        if (targetBase == null) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();   // 404 — no route
        }

        // 2. Build target URL preserving path + query string
        String rawQuery = exchange.getRequest().getURI().getRawQuery();
        String targetUrl = targetBase + path + (rawQuery != null ? "?" + rawQuery : "");

        // 3. Forward: method, headers (minus HOST), body → stream response back
        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(URI.create(targetUrl))
                .headers(h -> {
                    h.addAll(exchange.getRequest().getHeaders());
                    h.remove(HttpHeaders.HOST);   // backend has its own Host
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
```

**Why remove the HOST header?**
The client sends `Host: localhost:8080` (the gateway). If forwarded as-is, some backends reject it because it doesn't match their own host. Removing it lets the backend use its own default.

**Why `exchangeToMono` instead of `retrieve()`?**
`retrieve()` throws on 4xx/5xx. `exchangeToMono` passes the response through unchanged — the gateway is a transparent proxy and should never swallow a 404 or 401 that came from the backend.

---

## 6. api-gateway — All 5 Steps: Build Guide, Challenges, and Testing

---

### Step 1 — Basic Routing

**What was built:**
`RoutingFilter` reads a `gateway.routes` map from `application.yml`, matches the request path against prefixes, and proxies the full request (method + headers + body) to the backend using `WebClient`.

**Key files:**
- `RouteProperties.java` — `@ConfigurationProperties(prefix="gateway")` binds the routes map
- `GatewayConfig.java` — `WebClient` bean with 16 MB buffer (for large JSON responses)
- `RoutingFilter.java` — implements `WebFilter`, runs at `LOWEST_PRECEDENCE - 10`

**application.yml:**
```yaml
gateway:
  routes:
    "[/auth]": http://localhost:8081
    "[/products]": http://localhost:8082
```

**Design decisions:**
- `@Order(LOWEST_PRECEDENCE - 10)` — routing runs LAST so every other filter (auth, rate limit) can short-circuit before the backend is ever called
- `exchangeToMono` instead of `retrieve()` — `retrieve()` throws on 4xx/5xx responses; the gateway must be a transparent proxy and pass backend error codes through unchanged
- Remove `HOST` header — client sends `Host: localhost:8080`; if forwarded, backends that do Host validation reject it

---

**Challenge 1A — Spring Cloud incompatible with Spring Boot 4.0.6**

Initial plan was to use `spring-cloud-starter-gateway`. After adding the dependency, startup failed with:

```
ClassNotFoundException: org.springframework.boot.autoconfigure.web.ServerProperties
```

Then after adding exclusions:
```
ClassNotFoundException: org.springframework.boot.web.context.WebServerInitializedEvent
```

**Root cause:** Spring Cloud 2025.0.0 was compiled against different Spring Boot 4.x internal classes that moved between patch versions. `spring.cloud.discovery.enabled=false` did not help because the failure happens during `@ConditionalOnMissingBean` evaluation before any property binding.

**Fix:** Dropped Spring Cloud entirely. Replaced with `spring-boot-starter-webflux` + a custom `WebFilter` proxy — only ~70 lines, zero extra dependencies, full control.

---

**Challenge 1B — Routes map keys not matching**

After writing the route config as:
```yaml
gateway:
  routes:
    /auth: http://localhost:8081
```
The log showed `No route matched for path: /auth` even though the path clearly started with `/auth`.

**Root cause:** Spring Boot's relaxed binding normalizes map keys. A key containing `/` has the slash treated as a path separator, so the key arrives in the map as an empty string or gets mangled.

**Fix:** Bracket notation forces Spring to bind the key exactly as written:
```yaml
gateway:
  routes:
    "[/auth]": http://localhost:8081
```

---

**Testing Step 1:**
```bash
# Start auth-service (:8081) and product-service (:8082), then start gateway (:8080)

# Should forward to product-service and return product list
curl http://localhost:8080/products

# Should return 404 — no route configured for /unknown
curl http://localhost:8080/unknown

# Expected gateway DEBUG log:
# Routing GET /products → http://localhost:8082/products
# No route matched for path: /unknown
```

---

### Step 2 — JWT Validation Filter

**What was built:**
`JwtAuthFilter` runs at `@Order(-1)` — before `RoutingFilter`. It checks a public-route whitelist and, for protected routes, validates the `Authorization: Bearer <token>` header. Invalid or missing → 401 JSON before the backend is called.

**Key files:**
- `JwtUtil.java` — verify + extract only, same JJWT 0.12.6 API as auth-service and product-service
- `JwtAuthFilter.java` — `@Order(-1)`, public whitelist as a list of `(HttpMethod, pathPrefix)` records

**Public whitelist:**
```java
private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
    new PublicRoute(HttpMethod.POST, "/auth/login"),
    new PublicRoute(HttpMethod.POST, "/auth/register"),
    new PublicRoute(HttpMethod.GET,  "/products")   // all GET /products/** are public
);
```

**Design decisions:**
- `@Order(-1)` — runs before routing (`LOWEST_PRECEDENCE - 10`) so 401 stops the request before `WebClient` makes any network call
- `isTokenValid()` is fail-safe — catches `JwtException` and returns `false` instead of throwing; the filter converts that to a clean 401
- Public routes skip JWT entirely — no header required even if one happens to be present

**Why the JWT secret is also in api-gateway:**
The gateway needs to verify the signature. It uses the same `jwt.secret` value as auth-service and product-service — all three share the secret but the gateway never generates tokens, only verifies them.

---

**Challenge 2 — Correct filter ordering**

`WebFilter` order in Spring WebFlux: **lower number = higher priority = runs first**.

| Filter | Order | Runs |
|---|---|---|
| `RequestLoggingFilter` | -10 | First (outermost) |
| `CorsFilter` | -4 | Second |
| `RateLimitFilter` | -2 | Third |
| `JwtAuthFilter` | -1 | Fourth |
| `RoutingFilter` | `MAX - 10` | Last |

If JwtAuthFilter were at a higher number (lower priority) than RoutingFilter, the request would be forwarded to the backend before the token is checked — defeating the purpose.

---

**Testing Step 2:**
```bash
# 1. Public route — no token needed
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/products
# → 200

# 2. Protected route — no token
curl -s -X DELETE http://localhost:8080/products/1
# → {"error":"Unauthorized","message":"Authorization header missing or not Bearer"}

# 3. Protected route — bad token
curl -s -X DELETE http://localhost:8080/products/1 \
  -H "Authorization: Bearer bad.token.here"
# → {"error":"Unauthorized","message":"Token invalid or expired"}

# 4. Protected route — real token (get one from auth-service first)
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -X DELETE http://localhost:8080/products/1 \
  -H "Authorization: Bearer $TOKEN"
# → forwarded to product-service (403 if not admin, 204 if admin)
```

---

### Step 3 — Forward Identity Headers

**What was built:**
After validating the JWT, `JwtAuthFilter` extracts `username` and `role` from the token and injects them as `X-Username` and `X-User-Role` headers. `RoutingFilter` already forwards all headers — no changes needed there.

**Key change in JwtAuthFilter:**
```java
String username = jwtUtil.extractUsername(token);
String role     = jwtUtil.extractRole(token);

ServerWebExchange enriched = exchange.mutate()
        .request(r -> r.headers(h -> {
            h.set("X-Username",  username);
            h.set("X-User-Role", role);
        }))
        .build();

return chain.filter(enriched);   // pass the mutated exchange, not the original
```

**Why downstream services benefit:**
- product-service currently validates the JWT itself (`JwtAuthFilter` + `JwtUtil`)
- Once network-isolated (Docker/K8s), it can be replaced by a simple header-reader — no JJWT dependency needed
- cart-service, order-service built from the start to read `X-Username` from headers — no JWT at all

---

**Challenge 3 — WebFlux requests are immutable**

In WebMVC, you can modify `HttpServletRequest` attributes freely. In WebFlux, `ServerHttpRequest` is **immutable** — calling a setter on headers throws an `UnsupportedOperationException`.

**Fix:** `exchange.mutate()` creates a new `ServerWebExchange` with the modified request. The original exchange is unchanged. The mutated exchange is what you pass to `chain.filter()`.

```java
// WRONG — immutable, throws UnsupportedOperationException
exchange.getRequest().getHeaders().set("X-Username", username);

// CORRECT — create a new exchange with modified headers
ServerWebExchange enriched = exchange.mutate()
    .request(r -> r.headers(h -> h.set("X-Username", username)))
    .build();
return chain.filter(enriched);
```

---

**Testing Step 3:**
```bash
# Get a token, then make a protected request
# The backend service will receive X-Username and X-User-Role headers
# Verify by checking product-service logs for the username it received

TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Gateway debug log will show:
# JwtAuthFilter: JWT valid for POST /products — user: alice, role: ROLE_USER
# RoutingFilter: Routing POST /products → http://localhost:8082/products
# product-service will receive: X-Username: alice, X-User-Role: ROLE_USER
curl -s -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","description":"test","price":9.99,"stock":5,"category":"MAIN_COURSE"}'
```

---

### Step 4 — Rate Limiting

**What was built:**
`RateLimitFilter` runs at `@Order(-2)`. It maintains one `TokenBucket` per `(clientIP, route prefix)` in a `ConcurrentHashMap`. Each request consumes one token. When the bucket is empty → 429 with `Retry-After: 60` and `X-Rate-Limit-Limit` headers.

**Configured limits:**
```yaml
rate-limit:
  routes:
    "[/auth/login]": 5      # brute-force protection
    "[/auth/register]": 10  # account farming prevention
    "[/products]": 100      # normal API traffic
  default-requests-per-minute: 60
```

**Token bucket algorithm:**
```java
public synchronized boolean tryConsume() {
    refill();           // add tokens based on elapsed time
    if (tokens >= 1.0) {
        tokens -= 1.0;
        return true;    // request allowed
    }
    return false;       // bucket empty → 429
}

private void refill() {
    long now   = System.currentTimeMillis();
    double add = (now - lastRefillTime) * refillRatePerMs;
    tokens         = Math.min(capacity, tokens + add);  // never exceed capacity
    lastRefillTime = now;
}
```

**Why rate limiting runs BEFORE JWT validation (`@Order -2` not `-3`):**
Dropping abusive traffic at the rate limiter means JWT parsing never happens for flood requests. This is intentional — JWT parsing is relatively expensive. The most aggressive limit (`/auth/login: 5/min`) protects against brute-force attacks where no valid token exists anyway.

**Why `X-Forwarded-For` is respected:**
When the gateway runs behind a load balancer, `getRemoteAddress()` returns the load balancer's IP, not the real client. `X-Forwarded-For` carries the original client IP.

---

**Challenge 4 — Thread safety of the token bucket**

`ConcurrentHashMap` is thread-safe for `put`/`get`, but `TokenBucket.tryConsume()` involves a read-modify-write sequence (read tokens → refill → subtract → write). In the reactive event loop, multiple concurrent requests for the same client can race on the same bucket.

**Fix:** `synchronized` on `tryConsume()`. The lock is per-bucket (one object per client+route), so it only serializes requests from the same client to the same route — not the whole map.

```java
public synchronized boolean tryConsume() { ... }
```

---

**Testing Step 4:**
```bash
# Fire 7 rapid requests to /auth/login (limit = 5)
# First 5 should pass (401 from auth-service — wrong credentials, but reached backend)
# Requests 6 and 7 should get 429 from the gateway

for i in $(seq 1 7); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"x","password":"y"}')
  echo "Request $i → HTTP $CODE"
done

# Expected:
# Request 1 → HTTP 401  (reached auth-service, wrong credentials)
# Request 2 → HTTP 401
# Request 3 → HTTP 401
# Request 4 → HTTP 401
# Request 5 → HTTP 401
# Request 6 → HTTP 429  (rate limit hit — gateway stops here)
# Request 7 → HTTP 429

# Gateway WARN log for requests 6+:
# Rate limit exceeded — client: 127.0.0.1, route: /auth/login, limit: 5/min

# Check the 429 response body and headers
curl -v -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{}' 2>&1 | grep -E "429|Retry-After|X-Rate-Limit|Too Many"
```

---

### Step 5 — CORS, Request Logging, Uniform Error Format

**What was built — three independent additions:**

#### 5a. CorsFilter (`@Order(-4)`)

Adds `Access-Control-*` headers to **every** response — including 401 and 429. Without this, a browser receiving a 401 cannot read the JSON error body because the CORS headers are missing.

Handles `OPTIONS` preflight in the gateway itself (returns 200 immediately) — the backend never needs to know about CORS:
```java
if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
    response.setStatusCode(HttpStatus.OK);
    return response.setComplete();  // 1ms, backend not called
}
```

#### 5b. RequestLoggingFilter (`@Order(-10)`)

Outermost filter — wraps the entire pipeline to measure true end-to-end time:
```
→ GET /products client=127.0.0.1          (on arrival)
← 200 GET /products 631ms                 (on completion, including backend time)
← 429 POST /auth/login 1ms                (rate-limited — never reached backend)
```

Uses `doFinally(signal -> ...)` — fires after the reactive chain completes regardless of success or error.

#### 5c. Backend error handling in RoutingFilter

`WebClient` throws `WebClientRequestException` when the backend is unreachable. Without handling, Netty emits a raw stack trace to the client. With `.onErrorResume()`:

```java
.onErrorResume(ex -> backendError(exchange, ex))

// backendError():
// WebClientRequestException (connection refused) → 503 Service Unavailable
// anything else                                 → 502 Bad Gateway
// response: {"error":"Service Unavailable","message":"Backend service is not available","path":"/products"}
```

---

**Challenge 5A — CORS must wrap all other filters**

Initial placement of `CorsFilter` at `@Order(-1)` meant 401 and 429 responses sent before it ran had no CORS headers — browsers could not read the error body.

**Fix:** Move `CorsFilter` to `@Order(-4)` — runs after logging but before rate-limiting and JWT. Every response that goes back up the chain passes through it.

---

**Challenge 5B — OPTIONS preflight must never reach the backend**

If a browser sends `OPTIONS /products/1` and the gateway forwards it to product-service, the product-service (running Spring Security) has no CORS config and returns 403. The browser sees a failed preflight and blocks the real request.

**Fix:** `CorsFilter` intercepts `OPTIONS` and returns 200 immediately — `RoutingFilter` never runs for preflight requests.

---

**Testing Step 5:**
```bash
# 1. Structured logging — watch the gateway console while making requests
curl http://localhost:8080/products
# Gateway INFO logs:
# → GET /products client=0:0:0:0:0:0:0:1
# ← 200 GET /products 631ms

# 2. CORS preflight
curl -s -o /dev/null -w "%{http_code}" -X OPTIONS http://localhost:8080/products \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST"
# → 200 (1ms — backend never called)

# 3. CORS headers on all responses (including 401)
curl -s -I -X DELETE http://localhost:8080/products/1 | grep -i "access-control"
# access-control-allow-origin: *
# access-control-allow-methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
# access-control-allow-headers: Authorization, Content-Type, X-Requested-With

# 4. Backend-down error format — stop product-service first
# kill $(lsof -ti :8082)
curl http://localhost:8080/products
# → {"error":"Service Unavailable","message":"Backend service is not available","path":"/products"}
# Gateway ERROR log:
# Backend error [/products → 503 SERVICE_UNAVAILABLE]: Connection refused: localhost/[::1]:8082
```

---

### Full end-to-end testing sequence

Start services in order, then run:

```bash
# Step 1 — Start all services
# Terminal 1: cd auth-service    && mvn spring-boot:run
# Terminal 2: cd product-service && mvn spring-boot:run
# Terminal 3: cd api-gateway     && mvn spring-boot:run

# Step 2 — Get a token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Step 3 — Public read (no token)
curl http://localhost:8080/products                        # 200 list

# Step 4 — Protected write (admin token)
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Pizza","description":"Margherita","price":11.99,"stock":30,"category":"MAIN_COURSE"}'

# Step 5 — Protected write (no token) → 401
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{}'

# Step 6 — Rate limit brute-force (run 6 times fast → 6th gets 429)
for i in $(seq 1 6); do
  curl -s -o /dev/null -w "req $i → %{http_code}\n" \
    -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"x","password":"y"}'
done

# Step 7 — CORS preflight
curl -s -o /dev/null -w "%{http_code}\n" -X OPTIONS http://localhost:8080/products \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST"   # → 200
```

---

## 7. product-service — Overview and Structure

Runs on port 8082. Manages the product catalogue for the food delivery app.

```
product-service/
├── controller/
│   └── ProductController.java        — REST endpoints (GET public, POST/PUT/DELETE admin)
├── service/
│   └── ProductService.java           — business logic
├── repository/
│   └── ProductRepository.java        — Spring Data JPA custom queries
├── entity/
│   ├── Product.java                  — JPA entity mapped to `products` table
│   └── Category.java                 — enum: MAIN_COURSE, APPETIZER, DESSERT, BEVERAGES, SIDES
├── dto/
│   ├── ProductRequest.java           — record: validated input for create/update
│   └── ProductResponse.java          — record: immutable output snapshot
├── config/
│   ├── SecurityConfig.java           — Spring Security rules (GET public, writes need auth)
│   └── JwtAuthFilter.java            — JWT validation filter (same pattern as auth-service)
├── util/
│   └── JwtUtil.java                  — JWT verify / extract (no token generation here)
└── exception/
    ├── ProductNotFoundException.java  — thrown when product not found by ID
    └── GlobalExceptionHandler.java    — maps exceptions to HTTP 400/404/403
```

### Port allocation
| Service | Port | Reason |
|---|---|---|
| auth-service | 8081 | Handles identity — runs first |
| product-service | 8082 | Downstream service |
| api-gateway | 8080 | Entry point — faces the internet |

---

## 8. product-service — Dependencies

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- JJWT 0.12.6 — three jars required -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 9. product-service — Entity and Category Design

### Product entity
```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // no-arg constructor (JPA requirement), all-args constructor, getters, setters
}
```

`@Enumerated(EnumType.STRING)` stores `"MAIN_COURSE"` in the DB, not the index `0`. Using ordinal (`EnumType.ORDINAL`) is dangerous — if enum order changes, all existing data becomes wrong.

### Category — Enum vs @ManyToOne (design decision)

**Current implementation — enum:**
```java
public enum Category {
    MAIN_COURSE, APPETIZER, DESSERT, BEVERAGES, SIDES
}
```

**When to use enum:**
- Categories are fixed and developer-controlled
- Adding a category means a code change — which is acceptable
- No additional metadata needed per category (no image, description)
- Simple and fast — no JOIN query needed

**When to switch to `@ManyToOne` (a separate Category table):**
- Admins need to manage categories via API (add/edit/delete without deployment)
- Each category needs its own data: display name, image URL, display order, active flag
- The number of categories grows frequently

**What @ManyToOne looks like when needed:**
```java
// Category becomes its own entity
@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private int displayOrder;
}

// Product references it
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id", nullable = false)
private Category category;
```

`FetchType.LAZY` means the category is only loaded from DB when you actually call `product.getCategory()`. Without it, every product query does a JOIN even when you don't need the category — which wastes performance.

| Factor | Use Enum | Use @ManyToOne |
|---|---|---|
| Categories are stable (gender, status) | Yes | |
| Admins manage categories via UI | | Yes |
| Need category image/description | | Yes |
| Simplicity is priority now | Yes | |
| Zero-downtime category additions needed | | Yes |

**Rule:** Start with enum. Switch to `@ManyToOne` when admins need a category management screen.

---

## 10. product-service — Validations (Complete Guide)

### Two layers of validation

**Layer 1 — Input validation (DTO level)**
Catches bad data before it reaches the service. Throws `MethodArgumentNotValidException` → 400.

**Layer 2 — Business validation (Service level)**
Catches invalid states that input validation cannot detect (e.g., duplicate name, stock going negative).

### ProductRequest — all constraints explained

```java
public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal price,
        @Min(0) @Max(100000) int stock,
        @NotNull Category category
) {}
```

| Constraint | Field | Why |
|---|---|---|
| `@NotBlank` | name | Rejects null, empty string, and whitespace-only |
| `@Size(max = 255)` | name | MySQL column default length is 255 — catch before DB truncation error |
| `@Size(max = 1000)` | description | Matches `@Column(length = 1000)` — same reason |
| `@NotNull` | price | Explicit — records allow null unless you guard it |
| `@DecimalMin("0.01")` | price | Price zero or negative makes no sense |
| `@DecimalMax("99999.99")` | price | Matches `DECIMAL(10,2)` capacity; prevents absurd values |
| `@Min(0)` | stock | Stock can't be negative |
| `@Max(100000)` | stock | Prevents overflow inserts |
| `@NotNull` | category | Category is always required |

### Path variable validation

Without this, `/products/-1` or `/products/0` hits the database with a meaningless query.

```java
@Validated          // activates constraint checking on method parameters
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable @Positive Long id) {   // rejects 0 and negatives
        return ResponseEntity.ok(productService.getProductById(id));
    }
}
```

`@Positive` on a path variable throws `ConstraintViolationException` — different from body validation. Both need separate handlers in `GlobalExceptionHandler`.

### Two exception types for validation failures

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid on @RequestBody — field-level violations
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    // Handles @Positive / @Min / @Max on @PathVariable and @RequestParam
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst().orElse("Invalid request parameter");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
```

### What triggers which exception

| Annotation location | Exception thrown | Handler needed |
|---|---|---|
| `@Valid @RequestBody` | `MethodArgumentNotValidException` | `handleValidation` |
| `@Positive @PathVariable` | `ConstraintViolationException` | `handleConstraintViolation` |
| `@RequestParam` with constraints | `ConstraintViolationException` | `handleConstraintViolation` |

### Validation layers — what catches what

```
Client sends:  POST /products/-1 → @Positive catches it → 400 (never hits DB)
Client sends:  POST /products {"name": ""}  → @NotBlank catches it → 400 (never hits service)
Client sends:  POST /products {"name": "Burger", "price": -5} → @DecimalMin catches it → 400
Client sends:  valid request, name already exists → service layer throws → 409
```

---

## 11. product-service — Role-Based Security

### Endpoint access rules

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
            .anyRequest().authenticated()
        );
}
```

```java
// Controller — method-level role enforcement
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ProductResponse> createProduct(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/{id}")
public ResponseEntity<ProductResponse> updateProduct(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteProduct(...) { ... }
```

### Why GET is public but writes require ROLE_ADMIN

Food delivery customers need to browse the menu without logging in. Only admins (restaurant managers) should be able to add or change products. This is a common e-commerce pattern.

| Endpoint | Auth required | Role needed | Reason |
|---|---|---|---|
| `GET /products` | No | None | Public menu browsing |
| `GET /products/{id}` | No | None | Public product details |
| `POST /products` | Yes | ROLE_ADMIN | Create new product |
| `PUT /products/{id}` | Yes | ROLE_ADMIN | Update existing product |
| `DELETE /products/{id}` | Yes | ROLE_ADMIN | Remove product |

### Why `@EnableMethodSecurity` is required

Without it, `@PreAuthorize("hasRole('ADMIN')")` is silently ignored — the annotation exists on the method but nothing enforces it. All authenticated users could call admin endpoints.

```java
@Configuration
@EnableMethodSecurity   // ← this activates @PreAuthorize
public class SecurityConfig { ... }
```

### How the role gets into the SecurityContext

```
1. Client sends: Authorization: Bearer eyJhbGci...
2. JwtAuthFilter reads the header
3. JwtUtil.extractRole(token) → "ROLE_ADMIN"
4. SecurityContextHolder.setAuthentication(
       new UsernamePasswordAuthenticationToken(username, null,
           List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
   )
5. @PreAuthorize("hasRole('ADMIN')") checks SecurityContext → passes
```

### Why role must come from JWT, not request header

If you read the role from a request header like `X-Role: ADMIN`, any client can add that header themselves. The JWT is **signed with the server's secret** — tampering with any part (including the role claim) breaks the signature and `isTokenValid()` returns false. Only the server that issued the token can create a valid one.

---

## 12. product-service — JWT Flow: How Auth and Product Services Communicate

### The key concept — they never call each other

JWT is stateless. auth-service **signs** a token with a secret. product-service **verifies** the same token using the **same secret**. No HTTP call between services is needed.

```
┌────────────────────────────────────────────────────────────────┐
│  STEP 1 — Client logs in via auth-service (port 8081)          │
│                                                                │
│  Client ──POST /auth/login──▶ auth-service                     │
│                               ├── finds user in MySQL          │
│                               ├── BCrypt.matches(pass, hash)   │
│                               ├── generateToken("alice",       │
│                               │       "ROLE_ADMIN", SECRET)    │
│                               └──▶ returns JWT to client       │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│  STEP 2 — Client uses JWT to call product-service (port 8082)  │
│                                                                │
│  Client ──POST /products──▶ product-service                    │
│          Authorization:      ├── JwtAuthFilter reads header    │
│          Bearer eyJhbGci...  ├── JwtUtil.isTokenValid(token)   │
│                              │     → verifies with SAME SECRET │
│                              ├── extracts username + role      │
│                              ├── sets SecurityContext          │
│                              ├── @PreAuthorize checks role     │
│                              └──▶ 201 Created                  │
│                                                                │
│          ✗ auth-service is never called in step 2              │
└────────────────────────────────────────────────────────────────┘
```

### Why it works — shared secret

Both services have the same `jwt.secret` in their `application.properties`:

```properties
# auth-service application.properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437

# product-service application.properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
```

auth-service uses this key to sign. product-service uses the same key to verify the signature. Like a wax seal — the sender stamps it, any recipient who knows the stamp shape can verify it wasn't tampered with.

### What the JWT payload contains

```json
{
  "sub": "alice",
  "role": "ROLE_ADMIN",
  "iat": 1716000000,
  "exp": 1716086400
}
```

product-service extracts `sub` (username) and `role` directly from the payload — no DB lookup, no auth-service call needed.

### Token scenarios in product-service

```
No token sent          → JwtAuthFilter passes through
                       → GET /products → 200 OK (public)
                       → POST /products → 401 (Spring Security blocks unauthenticated)

Expired token          → isTokenValid() returns false → auth not set
                       → POST /products → 401

Tampered token         → signature check fails → isTokenValid() false
                       → POST /products → 401

Valid token ROLE_USER  → authenticated but @PreAuthorize("hasRole('ADMIN')") fails
                       → POST /products → 403 Forbidden

Valid token ROLE_ADMIN → all checks pass
                       → POST /products → 201 Created
```

### Actual step-by-step with curl

```bash
# Step 1 — get a token from auth-service
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'

# Response — copy the token value
# { "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 86400 }

# Step 2 — use that token to call product-service
curl -X POST http://localhost:8082/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Burger","description":"Classic","price":9.99,"stock":50,"category":"MAIN_COURSE"}'
```

### Current architecture — API Gateway validates JWT once

The api-gateway validates the JWT on every request and forwards identity as trusted headers. Downstream services don't need to parse the token again:

```
Client → api-gateway :8080  (validates JWT, extracts user info)
              │
              ├── X-Username: alice
              ├── X-User-Role: ROLE_ADMIN
              │
              ├──▶ product-service :8082  (reads headers, no JWT needed)
              ├──▶ cart-service    :8083  (reads headers, no JWT needed)
              └──▶ order-service   :8084  (reads headers, no JWT needed)
```

`JwtUtil` and `JwtAuthFilter` remain in each service for standalone use (e.g., running without the gateway in dev). Once Step 3 of the gateway is complete, downstream services can drop JWT validation and read the trusted gateway headers instead via a lightweight `GatewayHeaderFilter`.

---

## 13. product-service — Immutable Objects

### Records are immutable by design

All DTOs are Java records. Components are `final` — no setters, no mutation.

```java
public record ProductResponse(Long id, String name, BigDecimal price, ...) {}

ProductResponse r = new ProductResponse(1L, "Burger", new BigDecimal("9.99"), ...);
// r.name() works — r.setName() does not exist
```

Once a `ProductResponse` is created, nothing can change it. This is safe to share across threads, pass to different layers, and return from any method.

### Records with compact constructor — defensive validation

Records support a compact constructor to add validation or normalize values:

```java
public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Min(0) @Max(100000) int stock,
        @NotNull Category category
) {
    // compact constructor — runs before the record is created
    public ProductRequest {
        if (name != null) name = name.trim();  // normalize whitespace
    }
}
```

### Entity (`Product`) must be mutable — JPA requirement

JPA needs to update entity fields via setters (for `updateProduct`). But protect what shouldn't change:

```java
public class Product {
    // id never gets a setter — it is auto-generated and should never change
    public Long getId() { return id; }

    // all mutable fields have setters — service calls these on update
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    // ...
}
```

### `ProductResponse.from()` — snapshot, not a reference

```java
public static ProductResponse from(Product product) {
    return new ProductResponse(
            product.getId(),
            product.getName(),    // String is immutable — safe
            product.getDescription(),
            product.getPrice(),   // BigDecimal is immutable — safe
            product.getStock(),   // int is a primitive — copied by value
            product.getCategory() // enum is immutable — safe
    );
}
```

The caller gets a frozen snapshot of the product at that moment. If the product entity later changes (in the same transaction or another request), the already-returned `ProductResponse` is unaffected.

### When you need explicit defensive copy

If `Product` ever holds a mutable collection (e.g., tags, images):

```java
// WRONG — caller can mutate the internal list
public List<String> getTags() { return tags; }

// CORRECT — return an unmodifiable copy
public List<String> getTags() { return List.copyOf(tags); }

// In ProductResponse compact constructor — defensive copy on the way in
public record ProductResponse(List<String> tags, ...) {
    public ProductResponse {
        tags = List.copyOf(tags);  // any mutable list input becomes unmodifiable
    }
}
```

**Rule:** If a field type is immutable (`String`, `BigDecimal`, `Long`, enum), no defensive copy is needed. If it's a mutable collection or object, copy it in the constructor and return copies from getters.

---

## 14. product-service — Deep Copy vs Shallow Copy

### Shallow copy — copies references, not objects

```java
List<Product> original = productRepository.findAll();
List<Product> shallow = original;   // same list, same Product references

shallow.get(0).setName("Changed");
// original.get(0).getName() is also "Changed" — they point to the same object
```

Both variables point to the same objects. Changing one changes the other.

### Deep copy — creates new independent objects

```java
List<ProductResponse> deep = products.stream()
        .map(ProductResponse::from)   // creates a NEW ProductResponse for each product
        .toList();

// Even if the original Product entity is modified after this,
// the ProductResponse objects are unaffected — they are independent copies
```

`ProductResponse::from` is effectively a deep copy because it creates new record instances with immutable field values.

### Where this matters in the codebase

```java
// Service returns a deep copy of product data
public List<ProductResponse> getAllProducts(Category category) {
    List<Product> products = productRepository.findAll();
    return products.stream()
            .map(ProductResponse::from)  // each call creates a new independent record
            .toList();
}
```

The JPA-managed `Product` entities returned by the repository are in the persistence context — changes to them inside a transaction will be persisted. By converting them to `ProductResponse` immediately, callers work with immutable snapshots that are completely outside JPA control.

### Summary

| | Shallow Copy | Deep Copy |
|---|---|---|
| What is copied | References (pointers) | New objects with same values |
| Mutation effect | Affects original | No effect on original |
| In this codebase | `List<Product>` assignment | `ProductResponse.from(product)` |
| When needed | Rarely — usually accidental | Returning data to callers safely |

---

## 15. product-service — Fail-Fast vs Fail-Safe

### Fail-Fast — detect and throw immediately

Fail early so bugs surface at the right place rather than causing confusing errors downstream.

```java
// FAIL-FAST — throws as soon as product is not found
public ProductResponse getProductById(Long id) {
    return ProductResponse.from(
            productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id))
    );
    // Caller gets 404 immediately — never reaches a NullPointerException later
}

// FAIL-FAST — rejects the request before hitting the DB
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProductById(
        @PathVariable @Positive Long id) {   // @Positive fails on /products/-1 → 400
    ...
}

// FAIL-FAST — throws before saving a non-existent product
public void deleteProduct(Long id) {
    if (!productRepository.existsById(id)) {
        throw new ProductNotFoundException("Product not found: " + id);
    }
    productRepository.deleteById(id);
}
```

### Fail-Safe — continue gracefully

Fail-safe is correct when "nothing found" is a valid, expected result — not an error.

```java
// FAIL-SAFE — empty list is a valid response (no products in this category yet)
public List<ProductResponse> getAllProducts(Category category) {
    List<Product> products = category != null
            ? productRepository.findByCategory(category)
            : productRepository.findAll();
    return products.stream().map(ProductResponse::from).toList();
    // Returns [] — never throws when list is empty
}

// FAIL-SAFE — invalid JWT returns false, never crashes the filter chain
public boolean isTokenValid(String token) {
    try {
        parseClaims(token);
        return true;
    } catch (Exception e) {
        return false;  // graceful — lets the filter pass through
    }
}
```

### Decision table — which pattern to use

| Scenario | Pattern | Why |
|---|---|---|
| `GET /products/{id}` (specific resource) | Fail-Fast | Not finding a specific ID is an error |
| `GET /products` (list) | Fail-Safe | Empty list is valid — no products yet |
| `DELETE /products/{id}` | Fail-Fast | Deleting something that doesn't exist is an error |
| `POST /products` with invalid body | Fail-Fast | Client sent bad data — reject immediately |
| JWT validation in filter | Fail-Safe | Invalid token = no auth, not a crash |
| Path variable `/products/-1` | Fail-Fast | Invalid ID rejected before DB call |
| Category filter returns no results | Fail-Safe | No items in category is not an error |

### Fail-Fast at the boundary vs inside the service

```
Controller boundary:
  @Positive @PathVariable Long id → @Validated → ConstraintViolationException → 400
  @Valid @RequestBody → MethodArgumentNotValidException → 400

Service boundary:
  productRepository.findById(id).orElseThrow() → ProductNotFoundException → 404
  productRepository.existsById(id) check before delete → ProductNotFoundException → 404

Never:
  NullPointerException escaping to the caller  ← this means fail-fast wasn't applied early enough
```

---

## 16. product-service — Service Layer

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts(Category category) {
        List<Product> products = category != null
                ? productRepository.findByCategory(category)
                : productRepository.findAll();
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(
                productRepository.findById(id)
                        .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id))
        );
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(
                request.name(), request.description(), request.price(),
                request.stock(), request.category()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        return ProductResponse.from(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}
```

---

## 17. product-service — Controller and Exception Handler

### ProductController
```
GET    /products              — public, optional ?category= filter
GET    /products/{id}         — public
POST   /products              — ROLE_ADMIN
PUT    /products/{id}         — ROLE_ADMIN
DELETE /products/{id}         — ROLE_ADMIN, returns 204 No Content
```

```java
@Validated
@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(productService.getAllProducts(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable @Positive Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
```

### HTTP status choices explained

| Method | Status | Why |
|---|---|---|
| GET found | 200 OK | Standard |
| POST created | 201 Created | Signals a new resource was created |
| PUT updated | 200 OK | Returns updated resource |
| DELETE | 204 No Content | Success but nothing to return |
| Not found | 404 Not Found | Specific resource missing |
| Validation fail | 400 Bad Request | Client sent invalid data |
| No auth | 401 Unauthorized | No valid token |
| Wrong role | 403 Forbidden | Token valid but insufficient role |

### GlobalExceptionHandler — full version

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ← Product not found → 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    // ← @Positive on @PathVariable → 400
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst().orElse("Invalid request parameter");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    // ← @Valid on @RequestBody → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
```

---

## 18. product-service — Testing Strategy

### What to test

| Class | Test type | Reason |
|---|---|---|
| `ProductService` | Unit (Mockito) | Business logic, all CRUD paths |
| `ProductRepository` | `@DataJpaTest` | Custom queries: findByCategory, findByName |
| `ProductController` | `@WebMvcTest` | HTTP status, validation, role enforcement |
| `SecurityConfig` | No | Covered by `@WebMvcTest` via `@Import` |
| `JwtAuthFilter` | No | Covered by `@WebMvcTest` via `@Import` |
| `JwtUtil` | No (in product-service) | Only validates — tested in auth-service |
| `Product` entity / `Category` enum | No | No custom logic |

### Test count
```
ProductServiceApplicationTests — 1  (context loads with H2)
ProductControllerTest          — 11 (GET/POST/PUT/DELETE status codes + role checks)
ProductServiceTest             — 9  (all CRUD methods + not-found paths)
ProductRepositoryTest          — 5  (findByCategory, findByName, findAll)
```

### ProductControllerTest — key patterns

```java
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    // Test public endpoint — no auth needed
    @Test
    void getAllProducts_shouldReturn200WithList() throws Exception {
        when(productService.getAllProducts(null)).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    // Test that not-found propagates to 404
    @Test
    void getProductById_shouldReturn404WhenNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException("Product not found: 99"));
        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound());
    }

    // Test role enforcement
    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn201WhenAdmin() throws Exception { ... }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_shouldReturn403WhenNotAdmin() throws Exception { ... }
}
```

### ProductServiceTest — ArgumentCaptor pattern

```java
@Test
void createProduct_shouldSaveWithCorrectFields() {
    Product saved = sampleProduct();
    when(productRepository.save(any(Product.class))).thenReturn(saved);

    productService.createProduct(sampleRequest());

    // Capture what was actually passed to save()
    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertThat(captor.getValue().getCategory()).isEqualTo(Category.MAIN_COURSE);
}
```

### ProductRepositoryTest — @DataJpaTest

```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;  // Spring Boot 4.x

@DataJpaTest
class ProductRepositoryTest {
    @Autowired ProductRepository productRepository;

    @Test
    void findByCategory_shouldReturnOnlyMatchingCategory() {
        productRepository.save(new Product("Burger", "...", new BigDecimal("9.99"), 50, Category.MAIN_COURSE));
        productRepository.save(new Product("Cola", "...", new BigDecimal("2.49"), 100, Category.BEVERAGES));

        List<Product> result = productRepository.findByCategory(Category.BEVERAGES);
        assertThat(result).hasSize(1).extracting(Product::getName).containsExactly("Cola");
    }
}
```

---

## 19. product-service — API Usage with curl

### Browse products (no token needed)
```bash
# Get all products
curl http://localhost:8082/products

# Filter by category
curl "http://localhost:8082/products?category=MAIN_COURSE"

# Get specific product
curl http://localhost:8082/products/1
```

### Admin operations (requires ROLE_ADMIN JWT from auth-service)
```bash
# Get admin token first
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}' | jq -r '.token')

# Create product
curl -X POST http://localhost:8082/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Burger","description":"Classic beef","price":9.99,"stock":50,"category":"MAIN_COURSE"}'

# Update product
curl -X PUT http://localhost:8082/products/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Cheeseburger","description":"With cheese","price":11.99,"stock":40,"category":"MAIN_COURSE"}'

# Delete product
curl -X DELETE http://localhost:8082/products/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Validation error responses
```bash
# Blank name → 400
curl -X POST http://localhost:8082/products -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"","price":9.99,"stock":10,"category":"MAIN_COURSE"}'
# Response: {"error": "name: must not be blank"}

# Negative ID → 400
curl http://localhost:8082/products/-1
# Response: {"error": "getProductById.id: must be greater than 0"}

# Product not found → 404
curl http://localhost:8082/products/999
# Response: {"error": "Product not found: 999"}

# No token on admin endpoint → 401
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Burger","price":9.99,"stock":10,"category":"MAIN_COURSE"}'
# Response: 401 Unauthorized

# USER role on admin endpoint → 403
curl -X POST http://localhost:8082/products \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Burger","price":9.99,"stock":10,"category":"MAIN_COURSE"}'
# Response: 403 Forbidden
```

---

## 20. auth-service — Overview

Runs on port 8081. Handles user registration, login with JWT token generation, and admin role promotion.

```
auth-service/
├── controller/
│   └── AuthController.java         — REST endpoints
├── authservice/
│   └── AuthService.java            — business logic
├── config/
│   ├── SecurityConfig.java         — Spring Security rules
│   └── JwtAuthFilter.java          — JWT validation filter
├── dto/
│   ├── LoginRequest.java           — record: username + password
│   ├── LoginResponse.java          — record: token + tokenType + expiresIn
│   └── RegisterRequest.java        — record: username + password
├── entity/
│   ├── UserEntity.java             — JPA entity mapped to `users` table
│   └── Role.java                   — enum: ROLE_USER, ROLE_ADMIN
├── exception/
│   └── GlobalExceptionHandler.java — maps exceptions to HTTP responses
├── repository/
│   └── UserRepository.java         — Spring Data JPA repository
└── util/
    └── JwtUtil.java                — JWT generate / extract / validate
```

---

## 21. auth-service — Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- JJWT 0.12.6 — three jars required -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

---

## 22. auth-service — Database Setup

### Why two application.properties files

| File | Active when | Database |
|---|---|---|
| `src/main/resources/application.properties` | Running the app | MySQL on 127.0.0.1:3306 |
| `src/test/resources/application.properties` | Running tests | H2 in-memory |

Spring Boot automatically picks up `src/test/resources/application.properties` during test runs — MySQL is never touched.

### Main properties
```properties
spring.application.name=auth-service
server.port=8081
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/ecommerce?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration-ms=86400000
```

### Test properties
```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration-ms=86400000
```

### `ddl-auto` values
| Value | Behaviour | Use case |
|---|---|---|
| `update` | Adds new columns, never drops | Development / production |
| `create-drop` | Creates on start, drops on stop | Tests only |
| `validate` | Checks schema matches entity | Production safety |

---

## 23. auth-service — Entity and Repository

### Role enum
```java
public enum Role { ROLE_USER, ROLE_ADMIN }
```
The `ROLE_` prefix is required by Spring Security. `hasRole('ADMIN')` internally checks for `ROLE_ADMIN`.

### UserEntity
```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // constructors, getters, setRole()
}
```

### UserRepository
```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
```
Spring Data JPA generates the SQL from the method name — no `@Query` needed.

### Why role is never accepted from the frontend
`register()` always assigns `ROLE_USER` server-side. Accepting role from the request would let anyone register as admin.

---

## 24. auth-service — DTOs as Java Records

```java
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password
) {}

public record LoginResponse(String token, String tokenType, long expiresIn) {
    public static LoginResponse of(String token, long expiresInMs) {
        return new LoginResponse(token, "Bearer", expiresInMs / 1000);
    }
}
```

`expiresIn` is in seconds — JWT standard. `expiresInMs / 1000` converts from milliseconds.

### Why POST not GET for login
GET requests are cached, logged with the URL, and may have body dropped by proxies. Credentials in a URL appear in server logs and browser history. Always POST for credential transmission.

---

## 25. auth-service — JWT Implementation

### JwtUtil — sign and verify
```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration-ms}") private long expirationMs;

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) { return parseClaims(token).getSubject(); }
    public String extractRole(String token) { return parseClaims(token).get("role", String.class); }

    public boolean isTokenValid(String token) {
        try { parseClaims(token); return true; }
        catch (Exception e) { return false; }
    }
}
```

### JJWT 0.12.6 API changes from older versions
| Old API | New API (0.12.6) |
|---|---|
| `Jwts.parserBuilder()` | `Jwts.parser()` |
| `.setSigningKey()` | `.verifyWith()` |
| `parseClaimsJws()` | `parseSignedClaims()` |

---

## 26. auth-service — Security Filter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); return;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response); return;
        }
        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority(role))));
        filterChain.doFilter(request, response);
    }
}
```

`OncePerRequestFilter` guarantees the filter runs exactly once per HTTP request.

---

## 27. auth-service — Security Config

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

CSRF is disabled because stateless JWTs don't use cookies. `@EnableMethodSecurity` activates `@PreAuthorize`.

---

## 28. auth-service — Service Layer

```java
@Service
public class AuthService {

    @Value("${jwt.expiration-ms}") private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return LoginResponse.of(jwtUtil.generateToken(user.getUsername(), user.getRole().name()), expirationMs);
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("Username already exists");
        userRepository.save(new UserEntity(request.username(),
                passwordEncoder.encode(request.password()), Role.ROLE_USER));
    }

    public void promoteToAdmin(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
    }
}
```

### BCrypt validation — why you can't query by password
BCrypt adds a random salt before hashing. The same password hashed twice gives different strings. You must find by username first, then call `passwordEncoder.matches(raw, hash)`.

### Bug fixed — promoteToAdmin
Original code did `new UserEntity(username, password, ROLE_ADMIN)` — no ID, so JPA would INSERT a new row and fail with a unique constraint on username. Fix: call `user.setRole(Role.ROLE_ADMIN)` on the fetched entity and save it.

---

## 29. auth-service — Controller and Exception Handler

```
POST /auth/login                    — open to everyone
POST /auth/register                 — open to everyone
POST /auth/admin/promote/{username} — requires ROLE_ADMIN
```

| Exception | HTTP | Scenario |
|---|---|---|
| `BadCredentialsException` | 401 | Wrong password or username |
| `IllegalArgumentException` | 409 | Username exists / user not found |
| `MethodArgumentNotValidException` | 400 | Blank fields, short password |

---

## 30. auth-service — Testing Strategy

| Class | Test type | Reason |
|---|---|---|
| `AuthService` | Unit (Mockito) | Login, register, promote logic |
| `UserRepository` | `@DataJpaTest` | Custom queries + unique constraint |
| `JwtUtil` | Unit | Token encode/decode/expiry logic |
| `AuthController` | `@WebMvcTest` | HTTP status, validation, role checks |
| Config / Filter | No | Covered by `@WebMvcTest` via `@Import` |
| DTOs / Entity / Role | No | No custom logic |

```
AuthServiceApplicationTests — 1
AuthControllerTest          — 9
AuthServiceTest             — 8
UserRepositoryTest          — 5
JwtUtilTest                 — 6
Total: 29
```

---

## 31. auth-service — Test Setup

### JwtUtilTest — inject @Value without Spring context
```java
@BeforeEach
void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", "5367566B...");
    ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
}

@Test
void isTokenValid_shouldReturnFalseForExpiredToken() {
    ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);  // force expiry
    String token = jwtUtil.generateToken("alice", "ROLE_USER");
    assertThat(jwtUtil.isTokenValid(token)).isFalse();
}
```

### AuthControllerTest — @WebMvcTest
```java
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean AuthService authService;
}
```

`@Import` of real `JwtAuthFilter` and `JwtUtil` is required — mocking the filter breaks the filter chain and all tests pass regardless of auth.

### UserRepositoryTest — @DataJpaTest
```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;  // Spring Boot 4.x

@DataJpaTest  // uses H2 automatically, ignores MySQL config
class UserRepositoryTest { ... }
```

---

## 32. Creating the First Admin User

The promote endpoint requires an existing admin — so the first admin must be inserted directly into the DB.

### Generate BCrypt hash
```bash
htpasswd -nbBC 10 admin Admin@123 | cut -d: -f2
# Output: $2y$10$...
```

`$2y$` and `$2a$` are both accepted by Spring Security's `BCryptPasswordEncoder`.

### SQL INSERT
```sql
INSERT INTO users (username, password, role)
VALUES ('admin', '$2y$10$aM9F6mKqlaftgv2LtK1zzefCIr/mUNtDpeLBUwiG3ejQuLj5ZIlMG', 'ROLE_ADMIN');
```

```bash
mysql -u root -p ecommerce   # then paste the INSERT
```

### After first admin exists
Login → get JWT → call `POST /auth/admin/promote/{username}` to promote future admins. No more direct DB access needed.

---

## 33. auth-service — API Usage with curl

```bash
# Register
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
# 201 Created

# Login
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
# { "token": "eyJhbGci...", "tokenType": "Bearer", "expiresIn": 86400 }

# Promote to admin (requires admin JWT)
curl -X POST http://localhost:8081/auth/admin/promote/alice \
  -H "Authorization: Bearer eyJhbGci..."
# 200 OK
```

---

## 34. Common Errors and Fixes

### api-gateway: ClassNotFoundException on startup with Spring Cloud
```
ClassNotFoundException: org.springframework.boot.autoconfigure.web.ServerProperties
ClassNotFoundException: org.springframework.boot.web.context.WebServerInitializedEvent
```
**Cause:** Spring Cloud 2025.0.0 compiled against different Spring Boot 4.x internal classes that moved between patch versions. `spring.cloud.discovery.enabled=false` does not help because the failure happens in `@ConditionalOnMissingBean` evaluation before properties are bound.
**Fix:** Drop Spring Cloud entirely. Use `spring-boot-starter-webflux` with a custom `WebFilter` proxy — zero extra dependencies.

---

### api-gateway: No route matched — routes map empty despite correct config
```
DEBUG RoutingFilter: No route matched for path: /products
```
**Cause:** YAML map keys containing `/` are mangled by Spring Boot's relaxed binding. `/products` arrives in the map as an empty string.
**Fix:** Bracket notation preserves the key exactly:
```yaml
gateway:
  routes:
    "[/products]": http://localhost:8082   # correct
    /products: http://localhost:8082       # wrong — key arrives mangled
```

---

### api-gateway: UnsupportedOperationException when adding headers
```
java.lang.UnsupportedOperationException
  at org.springframework.http.ReadOnlyHttpHeaders.set(...)
```
**Cause:** `ServerHttpRequest` headers are immutable in WebFlux. You cannot call `exchange.getRequest().getHeaders().set(...)`.
**Fix:** Use `exchange.mutate()` to create a new exchange with the modified headers:
```java
ServerWebExchange enriched = exchange.mutate()
    .request(r -> r.headers(h -> h.set("X-Username", username)))
    .build();
return chain.filter(enriched);
```

---

### api-gateway: Browser cannot read 401/429 error body (CORS blocked)
**Cause:** `CorsFilter` was ordered after `JwtAuthFilter` / `RateLimitFilter`. Responses that short-circuit before `CorsFilter` runs carry no `Access-Control-Allow-Origin` header — browsers block the response entirely.
**Fix:** `CorsFilter` must be at a lower order number (higher priority) than all other filters:
```java
@Order(-4)  // correct — runs before RateLimitFilter (-2) and JwtAuthFilter (-1)
public class CorsFilter implements WebFilter { ... }
```

---

### api-gateway: OPTIONS preflight returns 403 (product-service rejects it)
**Cause:** `CorsFilter` was not short-circuiting preflight — `OPTIONS` requests were forwarded to product-service, which has no CORS config and rejects them with 403.
**Fix:** Return 200 immediately for `OPTIONS` inside `CorsFilter`, never call `chain.filter()`:
```java
if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
    response.setStatusCode(HttpStatus.OK);
    return response.setComplete();   // gateway handles it — backend never called
}
```

---

### api-gateway: Backend-down gives raw Netty error to client
```
reactor.netty.http.client.PrematureCloseException: Connection has been closed BEFORE response...
```
**Cause:** No error handling on the `WebClient` call in `RoutingFilter`. Exception bubbles to the client as a 500 with a stack trace.
**Fix:** Add `.onErrorResume()` to `RoutingFilter`:
```java
.onErrorResume(ex -> backendError(exchange, ex))
// WebClientRequestException → 503 {"error":"Service Unavailable",...}
// other exceptions          → 502 {"error":"Bad Gateway",...}
```

---

### @SpringBootTest fails — no DataSource configured
```
Failed to configure a DataSource: 'url' attribute is not specified
```
**Fix:** Add H2 as test dependency and create `src/test/resources/application.properties` with H2 config.

---

### All controller tests return 403
**Cause:** `@WebMvcTest` loads default Spring Security (all routes locked). Custom `SecurityConfig` not loaded.
**Fix:**
```java
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
```

---

### Controller tests return 200 for everything after mocking JwtAuthFilter
**Cause:** `@MockitoBean JwtAuthFilter` creates an empty mock that never calls `filterChain.doFilter()` — all requests pass.
**Fix:** Import the real class — never mock `JwtAuthFilter`.

---

### ObjectMapper cannot be autowired in @WebMvcTest
**Cause:** Spring Boot 4.x does not auto-configure `ObjectMapper` in the `@WebMvcTest` slice.
**Fix:** `private final ObjectMapper objectMapper = new ObjectMapper();`

---

### @DataJpaTest — package not found
```
package org.springframework.boot.test.autoconfigure.orm.jpa does not exist
```
**Fix:** Spring Boot 4.x moved it:
```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
```

---

### promoteToAdmin fails with duplicate entry
**Cause:** `new UserEntity(username, password, ROLE_ADMIN)` has no ID — JPA does INSERT, violates unique constraint.
**Fix:** `user.setRole(Role.ROLE_ADMIN); userRepository.save(user);` — update the existing entity.

---

### @Positive on path variable not returning 400
**Cause:** `@Validated` is missing on the controller class.
**Fix:** Add `@Validated` at class level:
```java
@Validated
@RestController
public class ProductController { ... }
```
Without `@Validated`, constraint annotations on method parameters (`@Positive`, `@Min`, `@Max`) are silently ignored.

---

### ConstraintViolationException not handled — returns 500
**Cause:** Only `MethodArgumentNotValidException` handler exists. Path variable violations throw `ConstraintViolationException` — a different type.
**Fix:** Add a separate `@ExceptionHandler(ConstraintViolationException.class)` in `GlobalExceptionHandler`.

---

### jwt.secret not found
```
Could not resolve placeholder 'jwt.secret'
```
**Cause:** Property exists in test properties but not in main application.properties.
**Fix:** Add to `src/main/resources/application.properties`:
```properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration-ms=86400000
```
