# Java Ecommerce — Services Guide
# api-gateway · auth-service · product-service · cart-service · order-service · payment-service · notification-service · JWT · Kafka · Redis · MongoDB

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

**cart-service**
35. [cart-service — Overview and Structure](#35-cart-service--overview-and-structure)
36. [cart-service — Redis Data Model](#36-cart-service--redis-data-model)
37. [cart-service — Service-to-Service Authentication](#37-cart-service--service-to-service-authentication)
38. [cart-service — Security Config](#38-cart-service--security-config)
39. [cart-service — Service Layer](#39-cart-service--service-layer)
40. [cart-service — Controller (Public and Internal Endpoints)](#40-cart-service--controller-public-and-internal-endpoints)
41. [cart-service — API Usage with curl](#41-cart-service--api-usage-with-curl)

**order-service**
42. [order-service — Overview and Structure](#42-order-service--overview-and-structure)
43. [order-service — Entity Design: Order, OrderItem, OrderStatus](#43-order-service--entity-design-order-orderitem-orderstatus)
44. [order-service — Price Snapshot Pattern](#44-order-service--price-snapshot-pattern)
45. [order-service — JPA Auditing (@CreatedDate / @LastModifiedDate)](#45-order-service--jpa-auditing-createddate--lastmodifieddate)
46. [order-service — Service-to-Service Clients](#46-order-service--service-to-service-clients)
47. [order-service — Service Layer: Order Creation Flow](#47-order-service--service-layer-order-creation-flow)
48. [order-service — Kafka Consumer](#48-order-service--kafka-consumer)
49. [order-service — Controller](#49-order-service--controller)
50. [order-service — API Usage with curl](#50-order-service--api-usage-with-curl)

**payment-service**
51. [payment-service — Overview and Structure](#51-payment-service--overview-and-structure)
52. [payment-service — Entity Design: Payment and PaymentStatus](#52-payment-service--entity-design-payment-and-paymentstatus)
53. [payment-service — Kafka Producer](#53-payment-service--kafka-producer)
54. [payment-service — Service Layer: Payment Flow](#54-payment-service--service-layer-payment-flow)
55. [payment-service — Security: ROLE_SERVICE Guard](#55-payment-service--security-role_service-guard)
56. [payment-service — Controller](#56-payment-service--controller)
57. [payment-service — Complete Async Flow End-to-End](#57-payment-service--complete-async-flow-end-to-end)
58. [payment-service — API Usage with curl](#58-payment-service--api-usage-with-curl)

**notification-service**
60. [notification-service — Overview and Structure](#60-notification-service--overview-and-structure)
61. [notification-service — MongoDB Data Model](#61-notification-service--mongodb-data-model)
62. [notification-service — Kafka Consumer](#62-notification-service--kafka-consumer)
63. [notification-service — Service Layer](#63-notification-service--service-layer)
64. [notification-service — Security Config](#64-notification-service--security-config)
65. [notification-service — Controller](#65-notification-service--controller)
66. [notification-service — Spring Boot 4 MongoDB Property Change](#66-notification-service--spring-boot-4-mongodb-property-change)
67. [notification-service — API Usage with curl](#67-notification-service--api-usage-with-curl)

**Errors**
34. [Common Errors and Fixes (auth/product/gateway)](#34-common-errors-and-fixes)
59. [Common Errors and Fixes (cart/order/payment/Kafka)](#59-common-errors-and-fixes-cartorderpaymentkafka)
68. [Common Errors and Fixes (notification-service/MongoDB)](#68-common-errors-and-fixes-notification-servicemongodb)

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

---

## 35. cart-service — Overview and Structure

Runs on port 8083. Manages each user's shopping cart using **Redis** as the storage backend. Cart data is ephemeral by design — it disappears when the cart is cleared or when the user places an order. Redis is a perfect fit because carts don't need SQL relations, they need fast reads/writes and optional TTL expiry.

```
cart-service/
├── config/
│   ├── AppConfig.java              — RestClient bean (used to call product-service)
│   ├── JwtAuthFilter.java          — JWT filter (same pattern as other services)
│   └── SecurityConfig.java         — all endpoints require auth; @EnableMethodSecurity
├── controller/
│   └── CartController.java         — public cart endpoints + internal service endpoints
├── dto/
│   ├── CartItemRequest.java        — record: productId + quantity (validated)
│   ├── CartItemResponse.java       — record: productId, name, price, quantity, subtotal
│   ├── CartResponse.java           — record: username, items[], total
│   └── ProductResponse.java        — record: snapshot of product data from product-service
├── exception/
│   ├── GlobalExceptionHandler.java — maps exceptions to HTTP responses
│   └── ProductNotFoundException.java
├── service/
│   └── CartService.java            — all cart operations with Redis
└── util/
    ├── JwtUtil.java                — JWT verify / extract
    └── ServiceTokenProvider.java   — generates short-lived ROLE_SERVICE tokens
```

### Port allocation
| Service | Port | Database | Purpose |
|---|---|---|---|
| cart-service | 8083 | Redis 6379 | Per-user cart storage |

### application.properties
```properties
spring.application.name=cart-service
server.port=8083
spring.data.redis.host=localhost
spring.data.redis.port=6379
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
product.service.url=http://localhost:8082
```

In Docker mode (`application-docker.properties`):
```properties
spring.data.redis.host=redis
product.service.url=http://product-service:8082
```

### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

---

## 36. cart-service — Redis Data Model

### Key structure
```
cart:{username}   →  Redis Hash
    field: "{productId}"    value: "{quantity}"
    field: "{productId}"    value: "{quantity}"
    ...
```

**Example — cart for user "alice":**
```
HSET cart:alice 1 2    ← productId=1, quantity=2
HSET cart:alice 3 1    ← productId=3, quantity=1
```

### Why a Hash, not a String or List?

| Structure | Problem |
|---|---|
| String (JSON blob) | Read-modify-write the entire JSON to change one item — not atomic |
| List | No natural key for productId — would need to scan the whole list |
| Hash | Each productId is a field — `HSET`, `HGET`, `HDEL` are all O(1) |

A Redis Hash lets you add, update, or remove individual items without touching the rest of the cart. `HGET cart:alice 1` returns just the quantity for product 1 — no full scan.

### Why quantities are stored as Strings in Redis
Redis hashes store byte arrays. Spring's `StringRedisTemplate` works with `String` keys and values. The quantities are stored as string digits (`"2"`, `"1"`) and parsed back to `int` in Java. This avoids needing a custom serializer.

### Why product data is NOT stored in Redis
The cart only stores `productId → quantity`. Product names and prices are fetched fresh from product-service every time you call `GET /cart`. This ensures:
- Cart always reflects the current product name (even after a rename)
- No stale price shown to user at browse time
- BUT: the price at checkout (order creation) is snapshotted — see [Order Service Price Snapshot Pattern](#44-order-service--price-snapshot-pattern)

---

## 37. cart-service — Service-to-Service Authentication

### The problem
cart-service calls product-service to validate that a product exists before adding it to the cart. product-service's write endpoints require `ROLE_ADMIN`. The `GET /products/{id}` endpoint is public — but we still want to send an authenticated request so the gateway can identify the caller.

More importantly, order-service calls cart-service's **internal** endpoints (`/cart/internal/{username}`). These internal endpoints must be accessible to order-service but not to regular users. The solution: a machine identity JWT with `ROLE_SERVICE`.

### ServiceTokenProvider
```java
@Component
public class ServiceTokenProvider {

    private static final long TTL_MS = 60_000; // 1 minute

    @Value("${jwt.secret}")
    private String secret;

    public String token() {
        return Jwts.builder()
                .subject("cart-service")          // identifies the caller service
                .claim("role", "ROLE_SERVICE")    // machine role — not ROLE_USER or ROLE_ADMIN
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TTL_MS))
                .signWith(signingKey())
                .compact();
    }
}
```

**Key design decisions:**
- TTL is 60 seconds — tokens are short-lived and generated on-demand. If compromised, they expire quickly.
- `sub: "cart-service"` identifies which service made the call (useful in logs).
- `ROLE_SERVICE` is a separate machine role — it cannot be obtained by any user registration flow.
- The token is signed with the **same shared secret** as all other JWTs — no separate PKI or secret management needed.

### How it flows
```
cart-service needs to call product-service:
  1. ServiceTokenProvider.token() → generates a 60s JWT with ROLE_SERVICE
  2. RestClient.get()
       .uri(productServiceUrl + "/products/" + productId)
       .header("Authorization", "Bearer " + token)
       .retrieve()

product-service JwtAuthFilter validates the token:
  3. isTokenValid(token) → true (same secret)
  4. extractRole(token) → "ROLE_SERVICE"
  5. Sets SecurityContext with ROLE_SERVICE authority
  6. GET /products/{id} is public → request passes
```

### Why not use the user's JWT for service-to-service calls
If cart-service forwarded the user's JWT to product-service, product-service would see the request as coming from the user. For internal endpoints protected by `@PreAuthorize("hasRole('SERVICE')")`, the user's token (which has `ROLE_USER`) would be rejected. Using a machine token with `ROLE_SERVICE` keeps service identity distinct from user identity.

---

## 38. cart-service — Security Config

All cart endpoints require authentication. The internal endpoints additionally require `ROLE_SERVICE` via `@PreAuthorize`.

```java
@Configuration
@EnableMethodSecurity   // ← activates @PreAuthorize on controller methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Authorization header missing or invalid\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()   // every endpoint needs a valid JWT
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### Why a custom AuthenticationEntryPoint
Without it, Spring Security returns `403 Forbidden` for unauthenticated requests (default behavior). The correct HTTP code for "no credentials provided" is `401 Unauthorized`. The custom entry point:
1. Returns `401` instead of `403`
2. Returns a JSON body instead of an HTML error page

### Why `@EnableMethodSecurity` is required
`@PreAuthorize("hasRole('SERVICE')")` on the internal endpoints is silently ignored without this annotation. Any authenticated user could call the internal endpoints, defeating their purpose. With `@EnableMethodSecurity`, Spring evaluates the `@PreAuthorize` expression after authentication succeeds — users with `ROLE_USER` are rejected at the method level.

---

## 39. cart-service — Service Layer

```java
@Service
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";

    // ADD: validate product exists, then increment quantity in Redis hash
    public void addItem(String username, CartItemRequest request) {
        fetchProduct(request.productId());   // throws ProductNotFoundException if not found

        String key = cartKey(username);
        String field = String.valueOf(request.productId());
        String existing = (String) redis.opsForHash().get(key, field);
        int newQty = (existing != null ? Integer.parseInt(existing) : 0) + request.quantity();
        redis.opsForHash().put(key, field, String.valueOf(newQty));
    }

    // GET: read all hash fields, fetch live product data for each, compute totals
    public CartResponse getCart(String username) {
        Map<Object, Object> entries = redis.opsForHash().entries(cartKey(username));
        List<CartItemResponse> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long productId = Long.parseLong((String) entry.getKey());
            int quantity   = Integer.parseInt((String) entry.getValue());
            ProductResponse product = fetchProduct(productId);
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(quantity));
            items.add(new CartItemResponse(productId, product.name(), product.price(), quantity, subtotal));
        }
        BigDecimal total = items.stream().map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(username, items, total);
    }

    // REMOVE: delete one hash field; fail-fast if product wasn't in cart
    public void removeItem(String username, Long productId) {
        long removed = redis.opsForHash().delete(cartKey(username), String.valueOf(productId));
        if (removed == 0) {
            throw new ProductNotFoundException("Product " + productId + " is not in the cart");
        }
    }

    // CLEAR: delete the entire hash (used by order-service after order placement)
    public void clearCart(String username) {
        redis.delete(cartKey(username));
    }

    // Calls product-service with a service JWT to validate product exists
    private ProductResponse fetchProduct(Long productId) {
        return restClient.get()
                .uri(productServiceUrl + "/products/" + productId)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ProductNotFoundException("Product not found: " + productId);
                })
                .body(ProductResponse.class);
    }

    private String cartKey(String username) {
        return CART_KEY_PREFIX + username;
    }
}
```

### Design decisions

**Why validate product on addItem, not just on getCart?**
If we skip validation on add, users can add non-existent product IDs. When they view the cart later, `fetchProduct` would throw an exception for every invalid ID — the cart becomes unreadable until the bad items are removed. Validating on add prevents the cart from ever containing phantom products.

**Why fetch live product data on every getCart?**
Cart items only store `productId → quantity`. Prices and names come from product-service at read time. This ensures the displayed price is always current. The tradeoff is N product-service calls per cart view (one per item). For a cart with 3-5 items this is acceptable. Caching with a short TTL could be added if needed.

**Why `redis.opsForHash()` vs `redis.opsForValue()`?**
`opsForValue()` stores one key → one string value. Updating a single cart item would require reading the whole JSON, modifying it, writing it back — a read-modify-write that is not atomic. `opsForHash()` lets you update a single field atomically with `HSET` without touching other fields.

---

## 40. cart-service — Controller (Public and Internal Endpoints)

```java
@RestController
@RequestMapping("/cart")
public class CartController {

    // ── User-facing endpoints ──────────────────────────────────────────────────
    // These are routed via api-gateway. The username comes from Principal
    // (set by JwtAuthFilter from the JWT sub claim).

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Principal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(Principal principal,
                                        @Valid @RequestBody CartItemRequest request) {
        cartService.addItem(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(Principal principal, @PathVariable Long productId) {
        cartService.removeItem(principal.getName(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ── Internal endpoints — NOT routed via api-gateway ───────────────────────
    // Accessible only within the Docker network. Protected by @PreAuthorize("hasRole('SERVICE')")
    // so only services presenting a ROLE_SERVICE token (from ServiceTokenProvider) can call them.

    @PreAuthorize("hasRole('SERVICE')")
    @GetMapping("/internal/{username}")
    public ResponseEntity<CartResponse> getCartInternal(@PathVariable String username) {
        return ResponseEntity.ok(cartService.getCart(username));
    }

    @PreAuthorize("hasRole('SERVICE')")
    @DeleteMapping("/internal/{username}")
    public ResponseEntity<Void> clearCartInternal(@PathVariable String username) {
        cartService.clearCart(username);
        return ResponseEntity.noContent().build();
    }
}
```

### Why internal endpoints use `{username}` path variable, not `Principal`

For user-facing endpoints, `Principal.getName()` reads the `sub` claim from the user's JWT — which is the username. This is correct because the user can only access their own cart.

For internal endpoints called by order-service, `Principal.getName()` would return `"order-service"` (the `sub` of the service token) — not the actual customer username. Order-service passes the customer's username in the URL path:

```
GET /cart/internal/alice
Authorization: Bearer <service-token with sub=order-service, role=ROLE_SERVICE>
```

This way cart-service looks up `cart:alice` in Redis, not `cart:order-service`.

### Endpoint summary

| Endpoint | Auth | Role | Description |
|---|---|---|---|
| `GET /cart` | Required | Any user | Get caller's cart |
| `POST /cart/items` | Required | Any user | Add item to caller's cart |
| `DELETE /cart/items/{productId}` | Required | Any user | Remove one item |
| `DELETE /cart` | Required | Any user | Clear caller's cart |
| `GET /cart/internal/{username}` | Required | ROLE_SERVICE | Get any user's cart (for order-service) |
| `DELETE /cart/internal/{username}` | Required | ROLE_SERVICE | Clear any user's cart (after order) |

---

## 41. cart-service — API Usage with curl

```bash
# --- Get a user JWT first ---
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Add item (product 1, qty 2)
curl -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
# → 201 Created (no body)

# Add another item
curl -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":3,"quantity":1}'

# View cart (fetches live prices from product-service)
curl http://localhost:8080/cart \
  -H "Authorization: Bearer $TOKEN"
# → {"username":"alice","items":[{"productId":1,"productName":"Classic Burger","price":11.99,"quantity":2,"subtotal":23.98},...],"total":28.97}

# Remove one item
curl -X DELETE http://localhost:8080/cart/items/1 \
  -H "Authorization: Bearer $TOKEN"
# → 204 No Content

# Clear the whole cart
curl -X DELETE http://localhost:8080/cart \
  -H "Authorization: Bearer $TOKEN"
# → 204 No Content

# Add non-existent product → 404
curl -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":9999,"quantity":1}'
# → {"error":"Product not found: 9999"}

# No token → 401
curl http://localhost:8080/cart
# → {"error":"Authorization header missing or invalid"}
```

---

## 42. order-service — Overview and Structure

Runs on port 8084. Handles order creation, retrieval, and cancellation. Stores orders in **PostgreSQL**. Talks to cart-service (to read and clear the cart) and payment-service (to initiate payment) via internal HTTP calls. Listens to Kafka topic `payment-events` to update order status asynchronously.

```
order-service/
├── client/
│   ├── CartClient.java          — calls cart-service /cart/internal/{username}
│   └── PaymentClient.java       — calls payment-service /payments (non-fatal on failure)
├── config/
│   ├── AppConfig.java           — RestClient bean
│   ├── JwtAuthFilter.java       — JWT filter
│   ├── KafkaConsumerConfig.java — ConsumerFactory + ConcurrentKafkaListenerContainerFactory
│   └── SecurityConfig.java      — all endpoints require auth
├── controller/
│   └── OrderController.java     — POST/GET/DELETE /orders
├── dto/
│   ├── CartItemResponse.java    — mirrors cart-service's response shape
│   ├── CartResponse.java
│   ├── CreateOrderRequest.java  — record: shippingAddress
│   ├── InitiatePaymentRequest.java — record: orderId, amount, username
│   ├── OrderItemResponse.java   — record: product snapshot fields
│   ├── OrderResponse.java       — record: full order with items and timestamps
│   └── PaymentEvent.java        — record: matches payment-service Kafka message
├── entity/
│   ├── Order.java               — JPA entity with @EnableJpaAuditing
│   ├── OrderItem.java           — child entity with price snapshot
│   └── OrderStatus.java         — PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── OrderNotFoundException.java
├── kafka/
│   └── PaymentEventConsumer.java  — @KafkaListener on "payment-events"
├── repository/
│   └── OrderRepository.java
├── service/
│   └── OrderService.java
└── util/
    ├── JwtUtil.java
    └── ServiceTokenProvider.java  — generates ROLE_SERVICE tokens for outbound calls
```

### Database
Uses PostgreSQL (`orderdb`). Separate from MySQL (auth/product) because:
- PostgreSQL has better support for JSON, complex queries, and analytics — useful for order history
- Separating databases means an auth-service outage doesn't affect order lookup
- Different teams could own different databases independently

---

## 43. order-service — Entity Design: Order, OrderItem, OrderStatus

### OrderStatus enum
```java
public enum OrderStatus {
    PENDING,    // just created — payment not yet confirmed
    CONFIRMED,  // payment succeeded (set by Kafka consumer)
    SHIPPED,    // future — set by shipping system
    DELIVERED,  // future — set on delivery confirmation
    CANCELLED   // payment failed or user cancelled
}
```

### Order entity
```java
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;          // the customer who placed the order

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;       // starts PENDING, Kafka consumer changes it

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;   // total at time of order (snapshot)

    @Column(nullable = false)
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreatedDate @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### OrderItem entity
```java
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;  // snapshot — see Price Snapshot Pattern

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;    // snapshot

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal; // price × quantity, computed at order time
}
```

### Relationship: Order ↔ OrderItem
```
Order (1) ←─── (Many) OrderItem
  cascade = ALL        → saving an Order also saves all its OrderItems
  orphanRemoval = true → removing an item from order.getItems() deletes it from DB
  FetchType.LAZY       → items not loaded unless explicitly accessed
```

`CascadeType.ALL` means you only call `orderRepository.save(order)` — JPA automatically persists all the `OrderItem` objects in `order.getItems()`. You don't need a separate `orderItemRepository.save()`.

---

## 44. order-service — Price Snapshot Pattern

### The problem
A customer places an order for a Burger at $9.99. A week later, the admin raises the price to $12.99. When the customer looks at their old order, should it show $9.99 or $12.99?

**Answer:** It must show $9.99 — the price they agreed to pay.

### The solution — snapshot at order creation time
When an order is created, `OrderItem` copies `productName` and `price` from the cart (which got them from product-service) at that exact moment:

```java
cart.items().forEach(item -> {
    OrderItem orderItem = new OrderItem(
            order,
            item.productId(),
            item.productName(),   // ← copied from cart at this moment
            item.price(),         // ← copied from cart at this moment
            item.quantity(),
            item.subtotal()
    );
    order.getItems().add(orderItem);
});
```

After this point, product-service can change the price of Burger to any value — the `order_items` table permanently holds `price = 9.99` for that order.

### Why not store only productId and look up the price later?
If you stored only `productId` and joined with products on every order lookup, the displayed price would change whenever the product price changes. Historical orders would be wrong — this is a data integrity issue.

### Trade-off
The snapshot means the order history can't automatically reflect product name corrections. If "Classic Burgur" was misspelled and later corrected to "Classic Burger", old orders still show the misspelled name. This is usually acceptable — receipts don't retroactively change.

---

## 45. order-service — JPA Auditing (@CreatedDate / @LastModifiedDate)

### What it does
Automatically populates `createdAt` and `updatedAt` timestamps without any manual code.

### Setup
**Step 1 — Enable auditing on the application class:**
```java
@SpringBootApplication
@EnableJpaAuditing        // ← activates the auditing infrastructure
public class OrderServiceApplication { ... }
```

**Step 2 — Register the listener on the entity:**
```java
@Entity
@EntityListeners(AuditingEntityListener.class)   // ← wires auditing to this entity
public class Order {
    @CreatedDate
    @Column(updatable = false)   // ← prevents this from being changed after creation
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;   // ← updated on every save
}
```

### How it works
- When `orderRepository.save(order)` is called for the first time: JPA sets `createdAt = now()` and `updatedAt = now()`
- On subsequent saves (e.g., when the Kafka consumer changes status to CONFIRMED): JPA sets `updatedAt = now()`, `createdAt` is unchanged because of `updatable = false`

### Why `updatable = false` on createdAt
Without it, Hibernate's UPDATE statement could overwrite `createdAt`. The `updatable = false` constraint tells Hibernate to exclude this column from UPDATE statements — it only appears in the INSERT.

### Why this matters in the async Kafka flow
The `updatedAt` field tells you when the order status was last changed. When an order goes from `PENDING` to `CONFIRMED`, the `updatedAt` timestamp advances. This creates a natural audit trail:
- `createdAt`: when the customer clicked "Place Order"
- `updatedAt`: when payment was confirmed (or failed)

---

## 46. order-service — Service-to-Service Clients

Both `CartClient` and `PaymentClient` use `ServiceTokenProvider` to generate a fresh 60-second ROLE_SERVICE JWT for every call.

### CartClient
```java
@Component
public class CartClient {

    public CartResponse getCart(String username) {
        return restClient.get()
                .uri(cartServiceUrl + "/cart/internal/" + username)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .body(CartResponse.class);
    }

    public void clearCart(String username) {
        restClient.delete()
                .uri(cartServiceUrl + "/cart/internal/" + username)
                .header("Authorization", "Bearer " + serviceTokenProvider.token())
                .retrieve()
                .toBodilessEntity();
    }
}
```

The `/cart/internal/{username}` endpoint is protected by `@PreAuthorize("hasRole('SERVICE')")` in cart-service. Without the service token, this returns 403. The user's own token (ROLE_USER) would also be rejected.

### PaymentClient — non-fatal error handling
```java
@Component
public class PaymentClient {

    public void initiatePayment(InitiatePaymentRequest request) {
        try {
            restClient.post()
                    .uri(paymentServiceUrl + "/payments")
                    .header("Authorization", "Bearer " + serviceTokenProvider.token())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            // Non-fatal: order stays PENDING. Kafka consumer will handle the status update.
            log.error("Failed to initiate payment for order {}: {}", request.orderId(), ex.getMessage());
        }
    }
}
```

**Why swallow the exception here?**
The order has already been saved to the database and the cart has been cleared. If payment initiation fails (e.g., payment-service is temporarily down), we don't want to crash the order creation flow — the order is valid, it just needs payment. The order stays `PENDING` permanently until:
1. Payment-service comes back up and processes the payment, OR
2. The user or admin cancels the order manually

This is the correct behavior for an async architecture — the order lifecycle is decoupled from the payment lifecycle.

### InitiatePaymentRequest
```java
public record InitiatePaymentRequest(Long orderId, BigDecimal amount, String username) {}
```

order-service sends `orderId`, `amount` (total), and `username` to payment-service. payment-service uses these to create the `Payment` record.

---

## 47. order-service — Service Layer: Order Creation Flow

```java
@Transactional
public OrderResponse createOrder(String username, CreateOrderRequest request) {

    // Step 1: Fetch the user's cart from cart-service
    CartResponse cart = cartClient.getCart(username);

    // Step 2: Validate cart is not empty
    if (cart.items() == null || cart.items().isEmpty()) {
        throw new IllegalStateException("Cannot place an order with an empty cart");
    }

    // Step 3: Build Order entity with price snapshots from cart
    Order order = new Order(username, cart.total(), request.shippingAddress());
    cart.items().forEach(item -> {
        order.getItems().add(new OrderItem(
                order, item.productId(), item.productName(),
                item.price(), item.quantity(), item.subtotal()));
    });

    // Step 4: Save order (+ all OrderItems via CascadeType.ALL)
    Order saved = orderRepository.save(order);

    // Step 5: Clear the cart — only AFTER the order is persisted
    cartClient.clearCart(username);

    // Step 6: Trigger payment — fire and forget, order stays PENDING
    paymentClient.initiatePayment(
            new InitiatePaymentRequest(saved.getId(), saved.getTotalAmount(), username));

    // Step 7: Return the PENDING order to the client immediately
    return toResponse(saved);
}
```

**Why clear the cart AFTER saving the order?**
If order save fails, the cart should remain intact — the user can try again. If we cleared the cart first, a DB failure would leave the user with no cart and no order.

**Why the method is `@Transactional`?**
The `orderRepository.save(order)` and all OrderItem inserts must succeed or fail together. If one item fails to persist, the whole order rolls back. `@Transactional` ensures atomicity at the database level.

**Note:** `cartClient.clearCart()` is outside the JPA transaction — it's an HTTP call to another service. If cart clearing fails after the order is saved, the user will see their cart still populated but the order exists. This is a known trade-off in distributed systems. A proper solution would use a saga pattern with compensating transactions.

### getOrder — ownership enforcement
Regular users can only see their own orders. Admins can see any order:

```java
public OrderResponse getOrder(String username, Long orderId, boolean isAdmin) {
    if (isAdmin) {
        return toResponse(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)));
    }
    // Non-admin: must be their order
    return toResponse(orderRepository.findByIdAndUsername(orderId, username)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)));
}
```

### cancelOrder — only PENDING orders can be cancelled
Once payment is confirmed (`CONFIRMED`) the order is in-flight. Cancelling a confirmed order would require a refund flow — out of scope here:

```java
if (order.getStatus() != OrderStatus.PENDING) {
    throw new IllegalStateException(
        "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
}
order.setStatus(OrderStatus.CANCELLED);
```

---

## 48. order-service — Kafka Consumer

### Why Kafka for order status updates (not a synchronous callback)

**Option A — payment-service calls order-service back via REST:**
- Tight coupling: payment-service must know order-service's URL
- If order-service is down when payment finishes, the status update is lost
- Retry logic must be implemented manually

**Option B — Kafka event (what we use):**
- Loose coupling: payment-service only knows the topic name, not any service URL
- If order-service is down, Kafka retains the message — it will be consumed when order-service restarts
- New consumers (notification-service, inventory-service) can subscribe to the same topic without changing payment-service

### KafkaConsumerConfig (required in Spring Boot 4)

Spring Boot 4 does NOT auto-create `kafkaListenerContainerFactory`. Both `@EnableKafka` and an explicit `@Configuration` providing the factory bean are required:

```java
// OrderServiceApplication.java
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka           // ← REQUIRED in Spring Boot 4 — enables @KafkaListener processing
public class OrderServiceApplication { ... }
```

```java
// KafkaConsumerConfig.java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PaymentEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);  // ← ignore __TypeId__ header
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

**`USE_TYPE_INFO_HEADERS = false` explained:**
By default, Spring's `JsonSerializer` on the producer embeds a `__TypeId__` header in every Kafka message. The header contains the fully-qualified class name of the Java object (e.g., `com.fooddelivery.paymentservice.dto.PaymentEvent`). When the consumer tries to deserialize, it looks for that class. Since order-service doesn't have the `paymentservice` package, it throws `ClassNotFoundException`.

Setting `USE_TYPE_INFO_HEADERS = false` tells the consumer to ignore the `__TypeId__` header and instead use `VALUE_DEFAULT_TYPE` (`com.fooddelivery.orderservice.dto.PaymentEvent`) — the local version of the same record.

### PaymentEventConsumer
```java
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    @Transactional
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for order {}", event.eventType(), event.orderId());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for payment event — skipping", event.orderId());
            return;
        }

        switch (event.eventType()) {
            case "PAYMENT_SUCCESS" -> {
                order.setStatus(OrderStatus.CONFIRMED);
                log.info("Order {} confirmed after successful payment {}", order.getId(), event.paymentId());
            }
            case "PAYMENT_FAILED" -> {
                order.setStatus(OrderStatus.CANCELLED);
                log.info("Order {} cancelled after failed payment {}: {}",
                        order.getId(), event.paymentId(), event.failureReason());
            }
            default -> log.warn("Unknown payment event type: {}", event.eventType());
        }

        orderRepository.save(order);
    }
}
```

### PaymentEvent record
```java
public record PaymentEvent(
    String eventType,      // "PAYMENT_SUCCESS" or "PAYMENT_FAILED"
    Long paymentId,        // payment-service's payment ID
    Long orderId,          // which order this payment is for
    String username,       // customer
    BigDecimal amount,
    String failureReason   // null on success
) {}
```

This record exists in **both** order-service and payment-service with the same field structure — they are intentionally separate classes (no shared library). The consumer ignores the type header and deserializes using its own local class.

---

## 49. order-service — Controller

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(Authentication auth,
                                                     @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getMyOrders(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(Authentication auth, @PathVariable Long id) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.getOrder(auth.getName(), id, isAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(Authentication auth, @PathVariable Long id) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(orderService.cancelOrder(auth.getName(), id, isAdmin));
    }
}
```

**Why `Authentication` instead of `Principal`?**
`Authentication` gives you both `getName()` (username) and `getAuthorities()` (roles). `Principal` only gives you `getName()`. The controller needs both to decide whether to show all orders or only the user's own orders.

**Why `DELETE` for cancel, not `PATCH`?**
Semantically, `PATCH /orders/{id}` would be more RESTful for status changes. `DELETE` is used here for simplicity — in a real system `PATCH /orders/{id} {"status":"CANCELLED"}` or a dedicated `POST /orders/{id}/cancel` endpoint would be cleaner.

### Endpoint summary

| Endpoint | Auth | What it does |
|---|---|---|
| `POST /orders` | Required | Create order from current cart |
| `GET /orders` | Required | List caller's orders (most recent first) |
| `GET /orders/{id}` | Required | Get specific order (admin sees any, user sees own) |
| `DELETE /orders/{id}` | Required | Cancel PENDING order (admin can cancel any) |

---

## 50. order-service — API Usage with curl

```bash
# --- Get token ---
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# --- Add items to cart first ---
curl -s -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

curl -s -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":3,"quantity":1}'

# --- Place order ---
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress":"123 Main St, Springfield"}'
# → {"id":4,"username":"alice","status":"PENDING","totalAmount":28.97,...}
# Cart is now cleared automatically

# Wait ~5 seconds for Kafka event to arrive, then:

# --- Check order status ---
curl -s http://localhost:8080/orders/4 \
  -H "Authorization: Bearer $TOKEN"
# → {"id":4,"status":"CONFIRMED",...}  ← updated by Kafka consumer

# --- List all my orders ---
curl -s http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN"
# → [{"id":4,...},{"id":3,...}]  ← most recent first

# --- Cancel a PENDING order ---
curl -s -X DELETE http://localhost:8080/orders/4 \
  -H "Authorization: Bearer $TOKEN"
# → {"id":4,"status":"CANCELLED",...}   (only works if still PENDING)

# --- Empty cart → 500 ---
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress":"Test"}'
# → {"error":"Cannot place an order with an empty cart"}
```

---

## 51. payment-service — Overview and Structure

Runs on port 8085. Handles payment processing for orders. Stores payments in **PostgreSQL** (`paymentdb` — separate from `orderdb`). Produces Kafka events to the `payment-events` topic after each payment outcome. Only order-service can initiate payments (protected by `ROLE_SERVICE`).

```
payment-service/
├── config/
│   ├── JwtAuthFilter.java      — JWT filter (same pattern as other services)
│   ├── KafkaProducerConfig.java — typed KafkaTemplate<String, PaymentEvent> bean
│   └── SecurityConfig.java     — @EnableMethodSecurity for ROLE_SERVICE guard
├── controller/
│   └── PaymentController.java  — POST /payments (SERVICE only), GET /payments/{id}
├── dto/
│   ├── InitiatePaymentRequest.java — record: orderId, amount, username
│   ├── PaymentEvent.java           — record: Kafka message payload
│   └── PaymentResponse.java        — record: response to caller
├── entity/
│   ├── Payment.java            — JPA entity with @EnableJpaAuditing
│   └── PaymentStatus.java      — PENDING, SUCCESS, FAILED
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── PaymentNotFoundException.java
├── repository/
│   └── PaymentRepository.java  — findByOrderId custom query
├── service/
│   └── PaymentService.java     — payment flow + Kafka publish
└── util/
    └── JwtUtil.java
```

### Port allocation and database
| Service | Port | Database | Why separate DB? |
|---|---|---|---|
| payment-service | 8085 | PostgreSQL `paymentdb` | Financial data needs isolation; separate backup/compliance policies |

### application.properties
```properties
spring.application.name=payment-service
server.port=8085
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentdb
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
spring.kafka.bootstrap-servers=localhost:9092
```

Docker profile (`application-docker.properties`):
```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/paymentdb
spring.kafka.bootstrap-servers=kafka:9092
```

**Creating paymentdb in Docker (one-time):**
```bash
docker exec ecommerce-postgres psql -U postgres -c "CREATE DATABASE paymentdb;"
```

---

## 52. payment-service — Entity Design: Payment and PaymentStatus

### PaymentStatus
```java
public enum PaymentStatus {
    PENDING,   // payment record created, processing not yet complete
    SUCCESS,   // payment processed successfully
    FAILED     // processing failed (insufficient funds, gateway error, etc.)
}
```

### Payment entity
```java
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;           // foreign key into order-service (logical, not JPA join)

    @Column(nullable = false)
    private String username;        // the customer

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;      // amount charged

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;   // starts PENDING, updated to SUCCESS or FAILED

    private String failureReason;   // null on success; populated on failure

    @CreatedDate @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### Why `orderId` is a plain `Long` (not `@ManyToOne Order`)
`orderId` is a **logical foreign key** — not a JPA relationship. payment-service has no `Order` entity; that lives in order-service's database. In a microservices architecture, services don't share database tables. The `orderId` is just a number that can be used to correlate payments to orders across services.

Using `@ManyToOne Order` here would require payment-service to import order-service's entity class, which would couple the services at the code level — exactly what microservices avoid.

---

## 53. payment-service — Kafka Producer

### Why an explicit KafkaProducerConfig is needed

Spring Boot auto-configuration provides `KafkaTemplate<Object, Object>` (using `Object` generic types). If you inject `KafkaTemplate<String, PaymentEvent>` directly, Spring cannot find a matching bean — it needs exact generic type matching.

The fix is to define your own typed `ProducerFactory` and `KafkaTemplate` beans:

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                JsonSerializer.ADD_TYPE_INFO_HEADERS, false   // ← don't embed __TypeId__ header
        ));
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate(
            ProducerFactory<String, PaymentEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### `ADD_TYPE_INFO_HEADERS = false` explained
By default, `JsonSerializer` adds a `__TypeId__` header to every Kafka message containing the Java class name (`com.fooddelivery.paymentservice.dto.PaymentEvent`). The consumer (order-service) doesn't have this class in its classpath — it has its own `com.fooddelivery.orderservice.dto.PaymentEvent`. If the consumer uses the header to determine the deserialization type, it throws `ClassNotFoundException`.

Setting `ADD_TYPE_INFO_HEADERS = false` prevents the producer from embedding the header. The consumer then uses its configured `VALUE_DEFAULT_TYPE` to deserialize the JSON into its own local class.

### Message partitioning
```java
kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
//                          ↑ partition key
```

Using `orderId` as the Kafka partition key ensures that all events for the same order are routed to the same partition. Within a partition, Kafka guarantees order. This means if there are multiple payment attempts for the same order, the consumer processes them in order.

---

## 54. payment-service — Service Layer: Payment Flow

```java
@Service
public class PaymentService {

    private static final String TOPIC = "payment-events";

    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {

        // Step 1: Create payment record in PENDING state
        Payment payment = new Payment(request.orderId(), request.username(), request.amount());
        paymentRepository.save(payment);

        try {
            // Step 2: Process payment
            // Currently a stub — always succeeds.
            // Replace with Stripe/PayPal/Braintree SDK call here.
            process(payment);

            // Step 3a: Payment succeeded — update status
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // Step 4a: Publish PAYMENT_SUCCESS event to Kafka
            publish(new PaymentEvent(
                    "PAYMENT_SUCCESS", payment.getId(), payment.getOrderId(),
                    payment.getUsername(), payment.getAmount(), null));

        } catch (Exception ex) {
            // Step 3b: Payment failed — record the reason
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            // Step 4b: Publish PAYMENT_FAILED event to Kafka
            publish(new PaymentEvent(
                    "PAYMENT_FAILED", payment.getId(), payment.getOrderId(),
                    payment.getUsername(), payment.getAmount(), ex.getMessage()));
        }

        return toResponse(payment);
    }

    private void process(Payment payment) {
        // Stub — replace with real payment gateway integration.
        // Throw RuntimeException to simulate a failure.
    }

    private void publish(PaymentEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        log.debug("Published {} to topic {}", event.eventType(), TOPIC);
    }
}
```

### Two saves — why?
`paymentRepository.save(payment)` is called twice in the success path:
1. First save: `status = PENDING` — establishes the payment record before processing starts. If the app crashes mid-processing, we have a record.
2. Second save: `status = SUCCESS` — updates after processing completes.

This gives a complete audit trail — you can see payments that started processing but never completed (they stay `PENDING` in the DB).

### Simulating a payment failure (for testing)
To test the `PAYMENT_FAILED` → order `CANCELLED` flow, throw an exception in `process()`:

```java
private void process(Payment payment) {
    throw new RuntimeException("Insufficient funds");
}
```

Then restart payment-service. The next order will fail payment and order-service's Kafka consumer will set the order to `CANCELLED`.

---

## 55. payment-service — Security: ROLE_SERVICE Guard

The `POST /payments` endpoint must only be callable by order-service — not by users or external clients. The protection is `@PreAuthorize("hasRole('SERVICE')")`:

```java
@PreAuthorize("hasRole('SERVICE')")
@PostMapping
public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request));
}
```

order-service calls this endpoint with its `ServiceTokenProvider` token (60s JWT with `role: ROLE_SERVICE`). A user's JWT has `role: ROLE_USER` or `role: ROLE_ADMIN` — neither passes the `hasRole('SERVICE')` check.

The GET endpoints have no role restriction — any authenticated user can look up payment status for any payment ID. A more restrictive design would also guard these by ownership (only the order owner can see their payment).

### Why this endpoint is NOT exposed via api-gateway

The api-gateway routes table does not include a route for `POST /payments`. Even if a user guesses the URL, the gateway's `RoutingFilter` returns 404 because there's no route match. The service-to-service call goes directly over the Docker internal network (`http://payment-service:8085`), bypassing the gateway entirely.

This is intentional: internal service endpoints should never be reachable from the internet.

---

## 56. payment-service — Controller

```java
@RestController
@RequestMapping("/payments")
public class PaymentController {

    // Only order-service (ROLE_SERVICE) can create a payment
    @PreAuthorize("hasRole('SERVICE')")
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request));
    }

    // Any authenticated user can look up payment by payment ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    // Look up payment by order ID (useful from order-service or admin tools)
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}
```

### PaymentResponse record
```java
public record PaymentResponse(
    Long id,
    Long orderId,
    String username,
    BigDecimal amount,
    PaymentStatus status,
    String failureReason,       // null on success
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

## 57. payment-service — Complete Async Flow End-to-End

This is the complete sequence from "user clicks place order" to "order is confirmed":

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Step 1 — User places order                                                │
│                                                                            │
│  POST /orders                                                              │
│  Authorization: Bearer <user-jwt>                                          │
│        │                                                                   │
│        ▼                                                                   │
│  api-gateway validates JWT → forwards to order-service:8084                │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 2 — order-service fetches and clears the cart                        │
│                                                                            │
│  CartClient.getCart("alice")                                               │
│    → GET http://cart-service:8083/cart/internal/alice                      │
│       Authorization: Bearer <service-jwt sub=order-service, role=SERVICE>  │
│    ← CartResponse{items=[...], total=28.97}                                │
│                                                                            │
│  Order saved to PostgreSQL orderdb (status=PENDING)                        │
│                                                                            │
│  CartClient.clearCart("alice")                                             │
│    → DELETE http://cart-service:8083/cart/internal/alice                   │
│       Authorization: Bearer <service-jwt>                                  │
│    ← 204 No Content                                                        │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 3 — order-service triggers payment (fire and forget)                 │
│                                                                            │
│  PaymentClient.initiatePayment(orderId=4, amount=28.97, username="alice")  │
│    → POST http://payment-service:8085/payments                             │
│       Authorization: Bearer <service-jwt sub=order-service, role=SERVICE>  │
│                                                                            │
│  order-service returns 201 CREATED to client immediately                   │
│  → {"id":4,"status":"PENDING",...}                                         │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 4 — payment-service processes the payment                            │
│                                                                            │
│  Payment saved to PostgreSQL paymentdb (status=PENDING)                    │
│  process() stub runs → always succeeds                                     │
│  Payment updated (status=SUCCESS)                                          │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 5 — payment-service publishes Kafka event                            │
│                                                                            │
│  kafkaTemplate.send("payment-events", "4", PaymentEvent{                   │
│      eventType: "PAYMENT_SUCCESS",                                         │
│      paymentId: 2,                                                         │
│      orderId: 4,                                                           │
│      username: "alice",                                                    │
│      amount: 28.97                                                         │
│  })                                                                        │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 6 — order-service Kafka consumer processes the event                 │
│                                                                            │
│  @KafkaListener topic="payment-events" groupId="order-service"             │
│  PaymentEventConsumer.onPaymentEvent(event)                                │
│    → order = orderRepository.findById(4)                                   │
│    → order.setStatus(CONFIRMED)                                            │
│    → orderRepository.save(order)                                           │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│  Step 7 — User polls and sees CONFIRMED                                    │
│                                                                            │
│  GET /orders/4                                                             │
│  ← {"id":4,"status":"CONFIRMED","updatedAt":"2026-05-19T15:24:09"}         │
└────────────────────────────────────────────────────────────────────────────┘
```

**Timeline:**
- Steps 1-3 (request to response): ~300ms (synchronous HTTP calls)
- Steps 4-6 (async Kafka flow): ~3-8 seconds (Kafka propagation + consumer poll interval)
- Step 7: user gets CONFIRMED on next poll after ~5-8 seconds

---

## 58. payment-service — API Usage with curl

```bash
# The POST endpoint requires a ROLE_SERVICE token — not normally callable directly.
# You trigger it by placing an order, which calls it internally.

# --- Look up payment by ID (any authenticated user) ---
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8080/payments/1 \
  -H "Authorization: Bearer $TOKEN"
# → {"id":1,"orderId":3,"username":"alice","amount":28.97,
#    "status":"SUCCESS","failureReason":null,
#    "createdAt":"2026-05-19T14:45:59","updatedAt":"2026-05-19T14:46:01"}

# --- Look up payment by order ID ---
curl -s http://localhost:8080/payments/order/3 \
  -H "Authorization: Bearer $TOKEN"
# → same response

# --- Direct call to payment-service (bypassing gateway) ---
# Useful during development when testing payment-service in isolation
curl -s http://localhost:8085/payments/1 \
  -H "Authorization: Bearer $TOKEN"

# --- Payment not found → 404 ---
curl -s http://localhost:8080/payments/9999 \
  -H "Authorization: Bearer $TOKEN"
# → {"error":"Payment not found: 9999"}
```

---

## 59. Common Errors and Fixes (cart/order/payment/Kafka)

### MySQL port 3306 conflict
```
[08S01] Communications link failure
```
**Cause:** A local MySQL installation is already bound to port 3306. Docker cannot expose its MySQL on the same host port.

**Fix:** Remap the host port in `docker-compose.yml`:
```yaml
mysql:
  ports:
    - "3307:3306"   # host:container — container still uses 3306 internally
```
Services inside the Docker network still reach MySQL via `mysql:3306` (the container port). Only external connections (e.g., MySQL Workbench from your laptop) use the remapped `localhost:3307`.

---

### bitnami/kafka image not found
```
Error response from daemon: manifest for bitnami/kafka:3.7 not found
```
**Cause:** `bitnami/kafka:3.7` (without a patch version) doesn't exist on Docker Hub. Also, Bitnami Kafka images with specific versions are sometimes unavailable in certain regions.

**Fix:** Use Apache's official image in KRaft mode (no ZooKeeper):
```yaml
kafka:
  image: apache/kafka:3.7.0   # official Apache image
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    CLUSTER_ID: "5L6g3nShT-eMCtK--X86sw"   # must be a stable base64 UUID
```

---

### KafkaTemplate bean not found — wrong generic type
```
NoSuchBeanDefinitionException: No qualifying bean of type
'org.springframework.kafka.core.KafkaTemplate<java.lang.String, PaymentEvent>'
```
**Cause:** Spring Boot auto-configuration only creates `KafkaTemplate<Object, Object>`. Injecting a typed `KafkaTemplate<String, PaymentEvent>` fails because the generic type doesn't match.

**Fix:** Define explicit typed beans in a `@Configuration` class:
```java
@Bean
public ProducerFactory<String, PaymentEvent> producerFactory() {
    return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
            JsonSerializer.ADD_TYPE_INFO_HEADERS, false
    ));
}

@Bean
public KafkaTemplate<String, PaymentEvent> kafkaTemplate(
        ProducerFactory<String, PaymentEvent> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
}
```

---

### @KafkaListener silently ignored — no consumer logs at startup (Spring Boot 4)
**Symptom:** The service starts successfully but there are no Kafka consumer initialization logs. `@KafkaListener` methods never fire.

**Cause:** In Spring Boot 4, `@EnableKafka` is NOT auto-applied. Without it, `@KafkaListener` annotation processing is never activated.

Additionally, Spring Boot 4 does NOT auto-create `kafkaListenerContainerFactory`. If the bean is missing, the app fails on startup with `NoSuchBeanDefinitionException: No bean named 'kafkaListenerContainerFactory' available`.

**Fix — two things required together:**

1. Add `@EnableKafka` to the application class:
```java
@SpringBootApplication
@EnableJpaAuditing
@EnableKafka        // ← required in Spring Boot 4
public class OrderServiceApplication { ... }
```

2. Provide explicit `kafkaListenerContainerFactory` bean:
```java
@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

---

### Kafka deserialization fails — ClassNotFoundException
```
Caused by: java.lang.ClassNotFoundException: com.fooddelivery.paymentservice.dto.PaymentEvent
```
**Cause:** Spring's `JsonSerializer` embeds a `__TypeId__` Kafka header containing the producer's fully-qualified class name. The consumer tries to find `com.fooddelivery.paymentservice.dto.PaymentEvent` — a class that doesn't exist in the order-service classpath.

**Fix — two parts:**

On the **producer** (payment-service): don't embed the type header:
```java
JsonSerializer.ADD_TYPE_INFO_HEADERS, false
```

On the **consumer** (order-service): ignore the header even if present, use local type:
```java
props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());
```

Both services have their own local `PaymentEvent` record with identical fields. JSON deserialization succeeds because the field names match, regardless of the class package.

---

### paymentdb does not exist
```
FATAL: database "paymentdb" does not exist
```
**Cause:** PostgreSQL only auto-creates a database if you configure it in `POSTGRES_DB`. The docker-compose file sets `POSTGRES_DB: orderdb` — `paymentdb` was never created.

**Fix:** Create it manually (one-time):
```bash
docker exec ecommerce-postgres psql -U postgres -c "CREATE DATABASE paymentdb;"
```

Or add it to `docker-compose.yml` using PostgreSQL's init scripts:
```yaml
postgres:
  environment:
    POSTGRES_DB: orderdb       # primary DB
    POSTGRES_MULTIPLE_DATABASES: "orderdb,paymentdb"  # requires custom init script
```

---

### Cart internal endpoint returns 403 for order-service
**Cause:** The internal endpoints use `@PreAuthorize("hasRole('SERVICE')")`, but `@EnableMethodSecurity` is missing from `SecurityConfig`.

**Fix:** Add `@EnableMethodSecurity` to `SecurityConfig`:
```java
@Configuration
@EnableMethodSecurity   // ← activates @PreAuthorize
public class SecurityConfig { ... }
```
Without it, `@PreAuthorize` is silently ignored and all authenticated requests pass. With `ROLE_USER` or `ROLE_ADMIN` tokens, the method would run for anyone — or with `anyRequest().authenticated()`, everyone authenticated could call internal endpoints.

---

### Order stays PENDING forever — Kafka consumer not receiving events
**Diagnostic steps:**
```bash
# 1. Check if payment-service actually published the event
docker compose logs payment-service | grep "Published"

# 2. Check if order-service consumer is subscribed
docker compose logs order-service | grep "Subscribed to topic"

# 3. Check for consumer errors
docker compose logs order-service | grep -i "error\|exception"

# 4. Check the topic exists in Kafka
docker exec ecommerce-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 --list

# 5. Check consumer group lag
docker exec ecommerce-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --describe --group order-service
```

**Common causes:**
- `@EnableKafka` missing on order-service application class → consumer never starts
- `kafkaListenerContainerFactory` bean missing → startup failure
- `USE_TYPE_INFO_HEADERS = false` missing → deserialization fails, consumer crashes on every message
- `payment-events` topic has messages with bad type headers (from before the fix) → delete topic and retry

**Delete and recreate the topic to clear bad messages:**
```bash
docker exec ecommerce-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 --delete --topic payment-events
# Kafka auto-creates it fresh on the next send
```

---

## 60. notification-service — Overview and Structure

### What it does
notification-service is a pure event-driven service. It never receives direct calls from other microservices. Instead, it sits on the same Kafka `payment-events` topic as order-service and independently consumes every payment event to build a per-user notification feed stored in MongoDB.

### Full data flow
```
POST /orders (user)
    │
    ▼
order-service          ── HTTP ──▶  payment-service
    │                                     │
    │                         processes payment
    │                                     │
    │                         publishes to Kafka
    │                                     │
    ▼                                     ▼
order-service                   notification-service
[PaymentEventConsumer]          [PaymentEventListener]
updates Order to                saves Notification
CONFIRMED/CANCELLED             to MongoDB
                                     │
                                     ▼
                            GET /notifications (user)
```

Both order-service and notification-service use a separate Kafka consumer group, so each independently receives every message from `payment-events`. This is the fan-out pattern — one published event, multiple independent consumers.

### Technology choices
| Concern | Choice | Why |
|---|---|---|
| Database | MongoDB | Notifications are schema-flexible documents, not relational data. No joins needed. Natural fit for append-only event logs. |
| Transport | Kafka consumer | Decoupled from payment-service. Notification delivery survives payment-service downtime. |
| Auth | Spring Security + JWT | Reads the same JWT the user carries — no dedicated session or token exchange. |
| Port | 8087 | Defined in `application.properties`, exposed in docker-compose |

### Module structure
```
notification-service/
├── pom.xml
└── src/main/java/com/fooddelivery/notificationservice/
    ├── NotificationServiceApplication.java   ← @EnableKafka + @EnableMongoAuditing
    ├── config/
    │   ├── KafkaConsumerConfig.java          ← explicit consumer factory (required in Spring Boot 4)
    │   ├── SecurityConfig.java               ← stateless JWT security
    │   └── JwtAuthFilter.java                ← reads JWT from Authorization header
    ├── controller/
    │   └── NotificationController.java       ← REST endpoints for users
    ├── dto/
    │   └── PaymentEvent.java                 ← mirrors payment-service's event record
    ├── entity/
    │   └── Notification.java                 ← MongoDB @Document
    ├── exception/
    │   └── GlobalExceptionHandler.java
    ├── kafka/
    │   └── PaymentEventListener.java         ← @KafkaListener on payment-events
    ├── repository/
    │   └── NotificationRepository.java       ← MongoRepository with derived queries
    ├── service/
    │   └── NotificationService.java          ← event handling + CRUD logic
    └── util/
        └── JwtUtil.java                      ← shared JWT parsing (same as other services)
```

### Application class
```java
@SpringBootApplication
@EnableKafka            // required in Spring Boot 4 — activates @KafkaListener processing
@EnableMongoAuditing    // required for @CreatedDate to populate on save
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

**Why two separate annotations?**
- `@EnableKafka` tells Spring to scan for `@KafkaListener` beans and wire up the listener container. Without it, `@KafkaListener` methods are silently ignored — no error, no consumer, no events received.
- `@EnableMongoAuditing` tells Spring Data MongoDB to process `@CreatedDate` / `@LastModifiedDate` fields. Without it, those fields are never populated on save (they stay `null`).

### Dependencies (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <!-- JWT parsing — same version as other services -->
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
</dependencies>
```

### Configuration files
**`application.properties`** (local dev):
```properties
spring.application.name=notification-service
server.port=8087

spring.mongodb.uri=mongodb://localhost:27017/notificationdb

jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437

spring.kafka.bootstrap-servers=localhost:9092

logging.level.com.fooddelivery.notificationservice=DEBUG
```

**`application-docker.properties`** (Docker Compose):
```properties
spring.mongodb.uri=mongodb://mongodb:27017/notificationdb
spring.kafka.bootstrap-servers=kafka:9092
```

> **Note:** The property is `spring.mongodb.uri` — not `spring.data.mongodb.uri`. In Spring Boot 4, the MongoDB property prefix changed. See [Section 66](#66-notification-service--spring-boot-4-mongodb-property-change) for the full explanation.

---

## 61. notification-service — MongoDB Data Model

### Why MongoDB for notifications?

Notifications are append-only event records. Every notification has the same top-level shape (username, type, message, timestamps) but could differ in payload (some carry orderId + paymentId, future types might carry different fields). MongoDB's flexible document model handles this naturally without requiring schema migrations.

Crucially, there are no joins. Fetching all notifications for a user is a single collection scan filtered by `username` — no foreign key lookups needed.

### PaymentEvent DTO
```java
public record PaymentEvent(
        String eventType,    // "PAYMENT_SUCCESS" or "PAYMENT_FAILED"
        Long paymentId,
        Long orderId,
        String username,
        BigDecimal amount,
        String failureReason // non-null only for PAYMENT_FAILED
) {}
```

This is a Java record (immutable, auto-generated constructor/getters/equals/hashCode). It mirrors the record in payment-service exactly — same field names, same types. The JSON deserializer on the consumer side reconstructs it from the Kafka message bytes.

> `failureReason` is `null` for `PAYMENT_SUCCESS` events. The service handles this gracefully in the message template.

### Notification entity
```java
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;           // MongoDB ObjectId — Spring Data stores as hex String

    @Indexed
    private String username;     // indexed for fast per-user queries

    private String type;         // "ORDER_CONFIRMED" or "ORDER_CANCELLED"
    private String message;      // human-readable text shown to the user
    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private boolean read;        // false until the user explicitly marks it read

    @CreatedDate
    private LocalDateTime createdAt;   // auto-populated by @EnableMongoAuditing

    public Notification(String username, String type, String message,
                        Long orderId, Long paymentId, BigDecimal amount) {
        this.username  = username;
        this.type      = type;
        this.message   = message;
        this.orderId   = orderId;
        this.paymentId = paymentId;
        this.amount    = amount;
        this.read      = false;   // always starts unread
    }
    // getters + setRead()
}
```

### Key annotation decisions

**`@Document(collection = "notifications")`**
Marks this as a MongoDB document. The `collection` name is the MongoDB collection (equivalent to a table in SQL). Without this, Spring Data MongoDB would derive the collection name from the class name (`notification`), which works but being explicit avoids surprises.

**`@Id` on a `String` field**
MongoDB uses `ObjectId` as its native ID type — a 12-byte BSON type that encodes timestamp + machine ID + counter. Spring Data MongoDB automatically converts `ObjectId` ↔ `String` when you declare `@Id` on a `String` field. You get hex strings like `"6a0c995b219c883cb6ca9f6e"` in JSON, which are URL-safe and easy to pass as path variables.

**`@Indexed` on `username`**
Every user-facing read query filters by `username`. Without an index, MongoDB does a full collection scan on every request. With the index, lookups are O(log n) regardless of how many total notifications exist. Spring Data MongoDB creates this index at startup when `@EnableMongoAuditing` is active.

**`@CreatedDate`**
Spring Data's auditing annotation. Automatically fills this field with `LocalDateTime.now()` when the document is first saved. Requires `@EnableMongoAuditing` on the application class — without it, the field is always `null`.

**`read = false` in constructor**
Every notification starts unread. The service only sets `read = true` explicitly when the user calls the mark-read endpoint. This means the unread count is always accurate without a separate status table.

### MongoRepository
```java
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // All notifications for a user, newest first
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);

    // Only unread notifications, newest first
    List<Notification> findByUsernameAndReadFalseOrderByCreatedAtDesc(String username);

    // Count of unread — used for the badge endpoint
    long countByUsernameAndReadFalse(String username);
}
```

Spring Data MongoDB derives all three queries from the method names at startup — no implementation code needed. The rules:
- `findBy<Field>` → filter on that field
- `And<Field>` → add another filter condition
- `<Field>False` → field must equal `false`
- `OrderBy<Field>Desc` → sort descending by that field
- `count` prefix → returns `long` instead of `List`

The `String` type parameter in `MongoRepository<Notification, String>` is the ID type — matches the `String id` field annotated with `@Id`.

### Sample MongoDB document
```json
{
  "_id": ObjectId("6a0c995b219c883cb6ca9f6e"),
  "username": "testuser",
  "type": "ORDER_CONFIRMED",
  "message": "Your order #6 has been confirmed! Payment of $9.99 was successful.",
  "orderId": 6,
  "paymentId": 4,
  "amount": 9.99,
  "read": false,
  "createdAt": ISODate("2026-05-19T17:09:47.623Z"),
  "_class": "com.fooddelivery.notificationservice.entity.Notification"
}
```

> MongoDB automatically adds `_class` to store the fully-qualified class name. This supports polymorphic document hierarchies — for a flat collection like this, it's safe to ignore.

---

## 62. notification-service — Kafka Consumer

### Consumer group separation
The `payment-events` topic is consumed by two independent services:
- `order-service` with group `order-service`
- `notification-service` with group `notification-service`

Each Kafka consumer group gets its own offset pointer into the topic partition. Publishing one message to `payment-events` results in both services receiving it independently — this is Kafka's fan-out. If you used the same group ID for both, only one of them would receive each message (Kafka load-balances within a group).

### KafkaConsumerConfig
```java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PaymentEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

### Why the explicit `KafkaConsumerConfig` bean?
In Spring Boot 3.x, `ConcurrentKafkaListenerContainerFactory` was auto-created by Spring Boot auto-configuration if a `spring-kafka` dependency was on the classpath. In **Spring Boot 4**, this auto-creation was removed. If you only add `@KafkaListener` without providing the `kafkaListenerContainerFactory` bean, Spring throws:

```
NoSuchBeanDefinitionException: No bean named 'kafkaListenerContainerFactory' available
```

The explicit `@Configuration` class declaring both beans (`ConsumerFactory` and `ConcurrentKafkaListenerContainerFactory`) is the fix.

### Key consumer properties

**`AUTO_OFFSET_RESET_CONFIG = "earliest"`**
When this consumer group starts for the first time (no committed offset yet), start reading from the beginning of the topic. This ensures the service processes all historical events even if it was deployed after payment-service started publishing. The alternative `"latest"` would skip all prior events.

**`USE_TYPE_INFO_HEADERS = false`**
Instructs the `JsonDeserializer` to ignore the `__TypeId__` Kafka header when deserializing messages. This header contains the producer's fully-qualified class name (e.g. `com.fooddelivery.paymentservice.dto.PaymentEvent`). If the consumer tries to load that class, it throws `ClassNotFoundException` because the producer's package doesn't exist in the consumer's JVM. Setting this to `false` ignores the header entirely.

**`VALUE_DEFAULT_TYPE = PaymentEvent.class.getName()`**
Since we're ignoring `__TypeId__`, the deserializer needs to know what class to deserialize the JSON bytes into. This tells it to always use `PaymentEvent` regardless of headers.

**`TRUSTED_PACKAGES = "*"`**
Allows the deserializer to instantiate any class. Normally you'd restrict this to your own packages, but since we're using `VALUE_DEFAULT_TYPE` and `USE_TYPE_INFO_HEADERS = false`, the deserializer always targets `PaymentEvent` anyway — the trust list is irrelevant but still required to avoid a whitelist validation error.

### PaymentEventListener
```java
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationService notificationService;

    public PaymentEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for order {} user '{}'",
                event.eventType(), event.orderId(), event.username());
        notificationService.handlePaymentEvent(event);
    }
}
```

The `@KafkaListener` annotation on `onPaymentEvent` tells Spring Kafka to invoke this method for every message in `payment-events` for this consumer group. The `groupId` here should match the one in `KafkaConsumerConfig` — declaring it in both places is redundant but makes the intent explicit at the method level.

The method delegates immediately to `notificationService.handlePaymentEvent()` rather than doing any logic itself. This keeps the listener thin — it's purely a Kafka-to-service bridge.

---

## 63. notification-service — Service Layer

### handlePaymentEvent — event-to-notification translation
```java
public void handlePaymentEvent(PaymentEvent event) {
    String type;
    String message;

    switch (event.eventType()) {
        case "PAYMENT_SUCCESS" -> {
            type    = "ORDER_CONFIRMED";
            message = String.format(
                "Your order #%d has been confirmed! Payment of $%s was successful.",
                event.orderId(), event.amount());
        }
        case "PAYMENT_FAILED" -> {
            type    = "ORDER_CANCELLED";
            message = String.format(
                "Payment failed for order #%d. Your order has been cancelled. Reason: %s",
                event.orderId(),
                event.failureReason() != null ? event.failureReason() : "Unknown error");
        }
        default -> {
            log.warn("Unknown payment event type: {}", event.eventType());
            return;   // discard unknown event types gracefully
        }
    }

    Notification notification = new Notification(
            event.username(), type, message,
            event.orderId(), event.paymentId(), event.amount());

    notificationRepository.save(notification);
    log.info("Saved notification [{}] for user '{}' — order {}",
             type, event.username(), event.orderId());
}
```

**Event type mapping:**
| Kafka `eventType` | MongoDB `type` | Message shown to user |
|---|---|---|
| `PAYMENT_SUCCESS` | `ORDER_CONFIRMED` | "Your order #6 has been confirmed! Payment of $9.99 was successful." |
| `PAYMENT_FAILED` | `ORDER_CANCELLED` | "Payment failed for order #6. Your order has been cancelled. Reason: ..." |
| anything else | — | logged as warning, discarded |

The `default` branch with `return` is important: if payment-service ever adds a new event type (e.g. `PAYMENT_REFUNDED`), the notification-service won't crash — it just logs and moves on. The Kafka consumer commits the offset and continues processing subsequent messages.

### Read/unread management
```java
public List<Notification> getUnread(String username) {
    return notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc(username);
}

public List<Notification> getAll(String username) {
    return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
}

public long countUnread(String username) {
    return notificationRepository.countByUsernameAndReadFalse(username);
}
```

All three methods take `username` from the authenticated `Principal` (set by the JWT filter), not from a request parameter. This means users can only ever query their own notifications — they cannot pass someone else's username.

### markRead — ownership validation
```java
public void markRead(String username, String notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

    if (!notification.getUsername().equals(username)) {
        throw new IllegalArgumentException("Notification does not belong to this user");
    }

    notification.setRead(true);
    notificationRepository.save(notification);
}
```

The service fetches by ID first, then validates that `notification.getUsername()` matches the authenticated user's username. This prevents user A from marking user B's notifications as read by guessing MongoDB ObjectId strings.

### markAllRead — bulk update
```java
public void markAllRead(String username) {
    List<Notification> unread =
            notificationRepository.findByUsernameAndReadFalseOrderByCreatedAtDesc(username);
    unread.forEach(n -> n.setRead(true));
    notificationRepository.saveAll(unread);
}
```

This fetches all unread notifications for the user, sets `read = true` on each in memory, then calls `saveAll` to persist them all. `saveAll` issues one upsert per document — not a single bulk update. For users with hundreds of unread notifications this is inefficient, but for a typical notification feed it's acceptable and keeps the code simple. A production system would use `MongoTemplate` to issue a single `updateMany` query.

---

## 64. notification-service — Security Config

### SecurityConfig
```java
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Authorization header missing or invalid\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

All endpoints require authentication — there are no public routes on this service. Unauthenticated requests get a JSON 401 response rather than the default HTML Spring Security error page.

### JwtAuthFilter
The filter reads the `Authorization: Bearer <token>` header, validates the JWT, and sets the `SecurityContext` with the username. This makes `Principal` available in controller method parameters.

The filter is registered before `UsernamePasswordAuthenticationFilter` in the Spring Security chain — this is standard placement for custom JWT filters. It runs on every request before any authorization checks.

Unlike cart-service or order-service, notification-service has no internal `ROLE_SERVICE` endpoints. All endpoints are user-facing, so the only role check is "is any valid JWT present?" — `anyRequest().authenticated()` covers this.

---

## 65. notification-service — Controller

```java
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getUnread(Principal principal) {
        return ResponseEntity.ok(notificationService.getUnread(principal.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll(Principal principal) {
        return ResponseEntity.ok(notificationService.getAll(principal.getName()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countUnread(Principal principal) {
        long count = notificationService.countUnread(principal.getName());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Principal principal, @PathVariable String id) {
        notificationService.markRead(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
```

### Endpoint table
| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/notifications` | Unread notifications only, newest first | `200 OK` — array |
| `GET` | `/notifications/all` | All notifications (read + unread), newest first | `200 OK` — array |
| `GET` | `/notifications/count` | Count of unread | `200 OK` — `{"unread": N}` |
| `PATCH` | `/notifications/{id}/read` | Mark one notification as read | `204 No Content` |
| `PATCH` | `/notifications/read-all` | Mark all notifications as read | `204 No Content` |

### `Principal` injection
Spring MVC injects the `Principal` interface automatically into controller methods. Its `.getName()` returns the username from the `Authentication` object set by `JwtAuthFilter`. This is cleaner than taking `@RequestHeader("X-Username")` because it relies on Spring Security's authentication chain rather than trusting an HTTP header value.

### Why `PATCH` for read endpoints?
`PATCH` is the semantically correct HTTP method for a partial update — we're updating only the `read` field, not replacing the entire notification resource. `PUT` would imply replacing the whole document. `POST` implies creating a new resource. `PATCH` is the idiomatic choice here.

### Why `204 No Content` on mark-read?
The client already has the notification data (it fetched it earlier). There's nothing meaningful to return after marking it read. `204 No Content` avoids sending an empty body, which would be `200 OK` with `null` or `{}` — both slightly misleading.

---

## 66. notification-service — Spring Boot 4 MongoDB Property Change

This is the most important lesson from building notification-service. The MongoDB URI property changed between Spring Boot 3 and Spring Boot 4, and the old property is **silently ignored** — no error, no warning, just defaults.

### The change
| Spring Boot version | MongoDB URI property |
|---|---|
| 3.x | `spring.data.mongodb.uri` |
| 4.x | `spring.mongodb.uri` |

### What happens with the wrong property
If you write `spring.data.mongodb.uri=mongodb://mongodb:27017/notificationdb` in Spring Boot 4, the auto-configuration class (`MongoProperties` with `@ConfigurationProperties(prefix = "spring.mongodb")`) never sees this property. It falls back to its default:

```
DEFAULT_URI = "mongodb://localhost/test"
```

The service starts successfully, connects to `localhost:27017`, and you only discover the problem at runtime when a MongoDB operation fails:

```
MongoSocketOpenException: Exception opening socket
  caused by: java.net.ConnectException: Connection refused
  address=localhost:27017, type=UNKNOWN, state=CONNECTING
```

This is especially confusing in Docker because the service starts, the Kafka consumer subscribes, events arrive — and then the first save attempt fails.

### How to verify the property name
The `@ConfigurationProperties` prefix is embedded in the compiled `MongoProperties.class` in the `spring-boot-mongodb-4.x.x.jar`. You can inspect it:

```bash
# Find the jar
find ~/.m2 -name "spring-boot-mongodb-4*.jar"

# Extract and inspect
cd /tmp
unzip -o <jar-path> 'org/springframework/boot/mongodb/autoconfigure/MongoProperties.class'
javap -verbose org/springframework/boot/mongodb/autoconfigure/MongoProperties.class | grep "ConfigurationProperties" -A 3
# Output: value="spring.mongodb"
```

In Spring Boot 4, MongoDB autoconfiguration moved from `spring-boot-autoconfigure` into a dedicated `spring-boot-mongodb` module — the package changed from `org.springframework.boot.autoconfigure.mongo` to `org.springframework.boot.mongodb.autoconfigure`.

### The fix (three places)
**`application.properties`:**
```properties
# Spring Boot 4 — NOT spring.data.mongodb.uri
spring.mongodb.uri=mongodb://localhost:27017/notificationdb
```

**`application-docker.properties`:**
```properties
spring.mongodb.uri=mongodb://mongodb:27017/notificationdb
```

**`docker-compose.yml` environment variable:**
```yaml
# Environment variables follow Spring Boot relaxed binding:
# spring.mongodb.uri → SPRING_MONGODB_URI (dots → underscores, uppercase)
environment:
  SPRING_MONGODB_URI: mongodb://mongodb:27017/notificationdb
```

> **Wrong:** `SPRING_DATA_MONGODB_URI` → ignored by Spring Boot 4
> **Correct:** `SPRING_MONGODB_URI` → bound to `spring.mongodb.uri`

### Other affected properties
The same prefix rename affects all MongoDB connection properties:

| Spring Boot 3.x | Spring Boot 4.x |
|---|---|
| `spring.data.mongodb.uri` | `spring.mongodb.uri` |
| `spring.data.mongodb.host` | `spring.mongodb.host` |
| `spring.data.mongodb.port` | `spring.mongodb.port` |
| `spring.data.mongodb.database` | `spring.mongodb.database` |
| `spring.data.mongodb.username` | `spring.mongodb.username` |
| `spring.data.mongodb.password` | `spring.mongodb.password` |

---

## 67. notification-service — API Usage with curl

### Prerequisites
```bash
# Login to get a JWT token
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Verify you have a token
echo $TOKEN
```

### Trigger a notification (place an order)
Notifications are created automatically by the Kafka consumer — you don't create them directly. To generate one, place an order:

```bash
# 1. Add something to cart
curl -s -X POST http://localhost:8080/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"productId":1,"quantity":2}'

# 2. Place the order (triggers payment → Kafka → notification)
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"shippingAddress":"123 Main St"}' | python3 -m json.tool
# → {"id":6,"status":"PENDING","totalAmount":23.98,...}

# 3. Wait ~3 seconds for the full async chain to complete
# payment-service processes → publishes to Kafka → notification-service saves
```

### Get unread notifications
```bash
curl -s http://localhost:8080/notifications \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# → [
#     {
#       "id": "6a0c995b219c883cb6ca9f6e",
#       "username": "testuser",
#       "type": "ORDER_CONFIRMED",
#       "message": "Your order #6 has been confirmed! Payment of $23.98 was successful.",
#       "orderId": 6,
#       "paymentId": 4,
#       "amount": 23.98,
#       "read": false,
#       "createdAt": "2026-05-19T17:09:47.623"
#     }
#   ]
```

### Get all notifications (read + unread)
```bash
curl -s http://localhost:8080/notifications/all \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# → returns all notifications newest first, including ones already marked read
```

### Get unread count (for notification badge)
```bash
curl -s http://localhost:8080/notifications/count \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
# → {"unread": 1}
```

### Mark one notification as read
```bash
# Use the "id" from the GET /notifications response
NOTIF_ID="6a0c995b219c883cb6ca9f6e"

curl -s -X PATCH http://localhost:8080/notifications/${NOTIF_ID}/read \
  -H "Authorization: Bearer $TOKEN"
# → 204 No Content

# Verify it's now read
curl -s http://localhost:8080/notifications/count \
  -H "Authorization: Bearer $TOKEN"
# → {"unread": 0}
```

### Mark all notifications as read
```bash
curl -s -X PATCH http://localhost:8080/notifications/read-all \
  -H "Authorization: Bearer $TOKEN"
# → 204 No Content
```

### Verify data directly in MongoDB
```bash
# Connect to MongoDB container
docker exec -it ecommerce-mongodb mongosh notificationdb

# Inside mongosh:
db.notifications.find().pretty()
# Shows all documents in the collection

db.notifications.find({username: "testuser"}).pretty()
# Filter by user

db.notifications.countDocuments({read: false})
# Count unread across all users

db.notifications.getIndexes()
# Should show index on 'username' field
```

### Test directly against notification-service (bypassing gateway)
```bash
# Useful when debugging gateway routing issues
curl -s http://localhost:8087/notifications \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## 68. Common Errors and Fixes (notification-service/MongoDB)

---

### MongoDB connects to localhost instead of the Docker container host

**Symptom:**
```
MongoSocketOpenException: Exception opening socket
  caused by: java.net.ConnectException: Connection refused
  ...servers=[{address=localhost:27017, type=UNKNOWN, state=CONNECTING...
```

The service starts successfully, the Kafka consumer subscribes, and events arrive — but every MongoDB save fails.

**Cause:** The MongoDB URI property changed in Spring Boot 4. `spring.data.mongodb.uri` is silently ignored. The auto-configuration falls back to `mongodb://localhost/test`.

**Fix:** Use `spring.mongodb.uri` everywhere:
```properties
# application.properties
spring.mongodb.uri=mongodb://localhost:27017/notificationdb

# application-docker.properties
spring.mongodb.uri=mongodb://mongodb:27017/notificationdb
```

And in docker-compose:
```yaml
environment:
  SPRING_MONGODB_URI: mongodb://mongodb:27017/notificationdb
  # NOT: SPRING_DATA_MONGODB_URI (ignored in Spring Boot 4)
```

See [Section 66](#66-notification-service--spring-boot-4-mongodb-property-change) for the full explanation and property rename table.

---

### `@CreatedDate` field is always null in MongoDB documents

**Symptom:** Notifications save successfully but `createdAt` is `null` in every document.

**Cause:** `@EnableMongoAuditing` is missing from the application class. Without it, Spring Data MongoDB doesn't process `@CreatedDate` / `@LastModifiedDate` annotations.

**Fix:** Add `@EnableMongoAuditing` to `NotificationServiceApplication`:
```java
@SpringBootApplication
@EnableKafka
@EnableMongoAuditing   // ← this line
public class NotificationServiceApplication { ... }
```

---

### Kafka consumer not receiving events (`@KafkaListener` silently ignored)

**Symptom:** The service starts with no errors, but `PaymentEventListener.onPaymentEvent` is never called even though payment-service published events.

**Cause:** `@EnableKafka` is missing from the application class. In Spring Boot 4, `@KafkaListener` is not activated automatically. Without `@EnableKafka`, listener methods are discovered but never wired to a consumer.

**Fix:**
```java
@SpringBootApplication
@EnableKafka           // ← this line
@EnableMongoAuditing
public class NotificationServiceApplication { ... }
```

---

### `NoSuchBeanDefinitionException: No bean named 'kafkaListenerContainerFactory'`

**Symptom:** Service fails to start with:
```
NoSuchBeanDefinitionException: No bean named 'kafkaListenerContainerFactory' available
```

**Cause:** In Spring Boot 4, `ConcurrentKafkaListenerContainerFactory` is no longer auto-created. The `@KafkaListener` infrastructure requires this bean by name.

**Fix:** Add an explicit `KafkaConsumerConfig` class that declares both the `ConsumerFactory` and `kafkaListenerContainerFactory` beans. The bean name `kafkaListenerContainerFactory` must match exactly — Spring Kafka looks it up by name.

```java
@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentEvent>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

---

### `ClassNotFoundException` during Kafka deserialization

**Symptom:**
```
ClassNotFoundException: com.fooddelivery.paymentservice.dto.PaymentEvent
```
Consumer crashes on every message; no notifications are saved.

**Cause:** The `JsonSerializer` on payment-service embeds `__TypeId__: com.fooddelivery.paymentservice.dto.PaymentEvent` into each Kafka message header. The consumer tries to load that class but it doesn't exist in the notification-service JVM.

**Fix (consumer side):**
```java
props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEvent.class.getName());
```

**Fix (producer side, in payment-service):**
```java
props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
```

After fixing, delete any messages with bad headers by deleting and recreating the topic:
```bash
docker exec ecommerce-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:9092 --delete --topic payment-events
```

---

### GET /notifications returns 404 via the gateway

**Symptom:** Requests to `http://localhost:8080/notifications` return `404 Not Found`. Direct calls to `http://localhost:8087/notifications` work.

**Cause:** The api-gateway Docker image was built before the `/notifications` route was added to `application-docker.yml`. The running container has the old route table without the notification-service entry.

**Fix:** Rebuild and restart the api-gateway:
```bash
docker compose build api-gateway
docker compose up -d api-gateway
```

**Prevention:** After changing gateway configuration, always rebuild the gateway image. The route table is baked into the jar at build time.
