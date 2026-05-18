# Java Ecommerce — Services Guide
# product-service · auth-service · JWT · Spring Security · Validations · Design Patterns

---

## Table of Contents

**Multi-Module**
1. [Multi-Module Project Structure](#1-multi-module-project-structure)

**product-service**
2. [product-service — Overview and Structure](#2-product-service--overview-and-structure)
3. [product-service — Dependencies](#3-product-service--dependencies)
4. [product-service — Entity and Category Design](#4-product-service--entity-and-category-design)
5. [product-service — Validations (Complete Guide)](#5-product-service--validations-complete-guide)
6. [product-service — Role-Based Security](#6-product-service--role-based-security)
7. [product-service — JWT Flow: How Auth and Product Services Communicate](#7-product-service--jwt-flow-how-auth-and-product-services-communicate)
8. [product-service — Immutable Objects](#8-product-service--immutable-objects)
9. [product-service — Deep Copy vs Shallow Copy](#9-product-service--deep-copy-vs-shallow-copy)
10. [product-service — Fail-Fast vs Fail-Safe](#10-product-service--fail-fast-vs-fail-safe)
11. [product-service — Service Layer](#11-product-service--service-layer)
12. [product-service — Controller and Exception Handler](#12-product-service--controller-and-exception-handler)
13. [product-service — Testing Strategy](#13-product-service--testing-strategy)
14. [product-service — API Usage with curl](#14-product-service--api-usage-with-curl)

**auth-service**
15. [auth-service — Overview](#15-auth-service--overview)
16. [auth-service — Dependencies](#16-auth-service--dependencies)
17. [auth-service — Database Setup](#17-auth-service--database-setup)
18. [auth-service — Entity and Repository](#18-auth-service--entity-and-repository)
19. [auth-service — DTOs as Java Records](#19-auth-service--dtos-as-java-records)
20. [auth-service — JWT Implementation](#20-auth-service--jwt-implementation)
21. [auth-service — Security Filter](#21-auth-service--security-filter)
22. [auth-service — Security Config](#22-auth-service--security-config)
23. [auth-service — Service Layer](#23-auth-service--service-layer)
24. [auth-service — Controller and Exception Handler](#24-auth-service--controller-and-exception-handler)
25. [auth-service — Testing Strategy](#25-auth-service--testing-strategy)
26. [auth-service — Test Setup](#26-auth-service--test-setup)
27. [Creating the First Admin User](#27-creating-the-first-admin-user)
28. [auth-service — API Usage with curl](#28-auth-service--api-usage-with-curl)

**Errors**
29. [Common Errors and Fixes](#29-common-errors-and-fixes)

---

## 1. Multi-Module Project Structure

The root project is a Maven aggregator that owns all child modules.

```
Java-Ecommerce-New/           ← root aggregator (pom packaging)
├── pom.xml                   ← parent pom — shared plugins, properties
├── product-service/          ← child module (port 8082)
│   └── pom.xml
└── auth-service/             ← child module (port 8081)
    └── pom.xml
```

### Planned full architecture (startup order)
```
auth-service        (port 8081) — user login, JWT issue
product-service     (port 8082) — product CRUD
api-gateway         (port 8080) — routing, JWT validation, rate limiting
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
    <module>product-service</module>
    <module>auth-service</module>
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

## 2. product-service — Overview and Structure

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
| api-gateway | 8080 (future) | Entry point — faces the internet |

---

## 3. product-service — Dependencies

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

## 4. product-service — Entity and Category Design

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

## 5. product-service — Validations (Complete Guide)

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

## 6. product-service — Role-Based Security

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

## 7. product-service — JWT Flow: How Auth and Product Services Communicate

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

### Future state — API Gateway takes over JWT validation

When the API Gateway is added, it validates the JWT once and passes user info as trusted headers to downstream services:

```
Client → API Gateway (validates JWT)
              │
              ├── X-Username: alice
              ├── X-User-Role: ROLE_ADMIN
              │
              ├──▶ product-service  (reads headers, no JWT needed)
              ├──▶ cart-service     (reads headers, no JWT needed)
              └──▶ order-service    (reads headers, no JWT needed)
```

This is why `JwtUtil` and `JwtAuthFilter` are kept in each service for now — they work standalone. Once the gateway is in place, these can be removed and replaced by a `GatewayHeaderFilter` that reads the trusted headers.

---

## 8. product-service — Immutable Objects

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

## 9. product-service — Deep Copy vs Shallow Copy

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

## 10. product-service — Fail-Fast vs Fail-Safe

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

## 11. product-service — Service Layer

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

## 12. product-service — Controller and Exception Handler

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

## 13. product-service — Testing Strategy

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

## 14. product-service — API Usage with curl

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

## 15. auth-service — Overview

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

## 16. auth-service — Dependencies

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

## 17. auth-service — Database Setup

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

## 18. auth-service — Entity and Repository

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

## 19. auth-service — DTOs as Java Records

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

## 20. auth-service — JWT Implementation

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

## 21. auth-service — Security Filter

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

## 22. auth-service — Security Config

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

## 23. auth-service — Service Layer

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

## 24. auth-service — Controller and Exception Handler

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

## 25. auth-service — Testing Strategy

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

## 26. auth-service — Test Setup

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

## 27. Creating the First Admin User

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

## 28. auth-service — API Usage with curl

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

## 29. Common Errors and Fixes

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
