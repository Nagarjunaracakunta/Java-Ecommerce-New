# Java Ecommerce — Services Guide
# main-service · auth-service · JWT · Spring Security · Testing

---

## Table of Contents
1. [Multi-Module Project Structure](#1-multi-module-project-structure)
2. [main-service](#2-main-service)
3. [auth-service — Overview](#3-auth-service--overview)
4. [auth-service — Dependencies](#4-auth-service--dependencies)
5. [auth-service — Database Setup](#5-auth-service--database-setup)
6. [auth-service — Entity and Repository](#6-auth-service--entity-and-repository)
7. [auth-service — DTOs as Java Records](#7-auth-service--dtos-as-java-records)
8. [auth-service — JWT Implementation](#8-auth-service--jwt-implementation)
9. [auth-service — Security Filter](#9-auth-service--security-filter)
10. [auth-service — Security Config](#10-auth-service--security-config)
11. [auth-service — Service Layer](#11-auth-service--service-layer)
12. [auth-service — Controller and Exception Handler](#12-auth-service--controller-and-exception-handler)
13. [auth-service — Testing Strategy](#13-auth-service--testing-strategy)
14. [auth-service — Test Setup](#14-auth-service--test-setup)
15. [Creating the First Admin User](#15-creating-the-first-admin-user)
16. [API Usage with curl](#16-api-usage-with-curl)
17. [Common Errors and Fixes](#17-common-errors-and-fixes)

---

## 1. Multi-Module Project Structure

The root project is a Maven aggregator that owns both child modules.

```
Java-Ecommerce-New/           ← root aggregator (pom packaging)
├── pom.xml                   ← parent pom — shared plugins, properties
├── main-service/             ← child module (port 8080)
│   └── pom.xml               ← inherits parent, adds its own deps
└── auth-service/             ← child module (port 8081)
    └── pom.xml               ← inherits parent, adds its own deps
```

### Root pom.xml key sections
```xml
<packaging>pom</packaging>

<modules>
    <module>main-service</module>
    <module>auth-service</module>
</modules>
```

### How child modules inherit plugins
- Root pom puts shared plugin config inside `<pluginManagement>` — this defines the config but does NOT run anything.
- Each child module activates a plugin by declaring it under its own `<build><plugins>` without repeating config.
- This keeps all version numbers and settings in one place.

### Build commands
```bash
# Build everything from root
cd Java-Ecommerce-New
mvn clean package

# Build only auth-service
cd auth-service
mvn clean package
```

---

## 2. main-service

### What it does
Runs on port 8080. Currently contains startup bootstrap logic using `CommandLineRunner` and SLF4J logging.

### application.properties
```properties
spring.application.name=Java-Ecommerce-New
spring.security.user.name=user
spring.security.user.password=password
```

### CommandLineRunner — StartUpRunner
`CommandLineRunner` is a Spring Boot hook that runs automatically after the application context has fully loaded. Used for startup tasks like loading seed data.

```java
@Component
public class StartUpRunner implements CommandLineRunner {
    private final DataLoader dataLoader;

    public StartUpRunner(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        dataLoader.runTasks(args);
    }
}
```

### DataLoader — SLF4J logging with Lombok
`@Slf4j` (Lombok annotation) generates a `log` field automatically — no need to write `LoggerFactory.getLogger(...)` yourself.

```java
@Slf4j
@Component
public class DataLoader {

    public void runTasks(String... args) {
        log.info("DataLoader is running on startup....{}",
                args.length > 0 ? String.join(", ", args) : "no args");
    }
}
```

### Why use SLF4J over System.out.println
| `System.out.println` | SLF4J (`log.info`) |
|---|---|
| Always prints | Can be filtered by log level |
| No log level | DEBUG / INFO / WARN / ERROR levels |
| No timestamp | Includes timestamp, class, thread |
| Cannot be disabled | Disabled in prod by setting level |

### Lombok annotation processor in pom.xml
Lombok generates code at compile time, so it needs to be registered as an annotation processor — not just a dependency.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### Tests
```
JavaEcommerceNewApplicationTests  — @SpringBootTest contextLoads (verifies context starts)
DataLoaderTest                    — plain unit test, calls runTasks() directly
```

---

## 3. auth-service — Overview

Runs on port 8081. Handles user registration, login with JWT token generation, and admin role promotion.

```
auth-service/
├── controller/
│   └── AuthController.java        — REST endpoints
├── authservice/
│   └── AuthService.java           — business logic
├── config/
│   ├── SecurityConfig.java        — Spring Security rules
│   └── JwtAuthFilter.java         — JWT validation filter
├── dto/
│   ├── LoginRequest.java          — record: username + password
│   ├── LoginResponse.java         — record: token + tokenType + expiresIn
│   └── RegisterRequest.java       — record: username + password
├── entity/
│   ├── UserEntity.java            — JPA entity mapped to `users` table
│   └── Role.java                  — enum: ROLE_USER, ROLE_ADMIN
├── exception/
│   └── GlobalExceptionHandler.java — maps exceptions to HTTP responses
├── repository/
│   └── UserRepository.java        — Spring Data JPA repository
└── util/
    └── JwtUtil.java               — JWT generate / extract / validate
```

---

## 4. auth-service — Dependencies

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

<!-- MySQL (runtime only — not needed at compile time) -->
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

## 5. auth-service — Database Setup

### Why two application.properties files
| File | Active when | Database |
|---|---|---|
| `src/main/resources/application.properties` | Running the app | MySQL on 127.0.0.1:3306 |
| `src/test/resources/application.properties` | Running tests | H2 in-memory |

Spring Boot automatically picks up `src/test/resources/application.properties` when running tests. It overrides the main config — so MySQL is never touched during test runs.

### Main application.properties
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

### Test application.properties
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

### `ddl-auto` values explained
| Value | Behaviour | Use case |
|---|---|---|
| `update` | Adds new columns, never drops | Development / production |
| `create-drop` | Creates schema on start, drops on stop | Tests only |
| `create` | Creates schema on start, never drops | Initial setup |
| `validate` | Only checks schema matches entity | Production safety check |

---

## 6. auth-service — Entity and Repository

### Role enum
```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}
```
The `ROLE_` prefix is required by Spring Security. `hasRole('ADMIN')` internally checks for authority `ROLE_ADMIN`.

### UserEntity
```java
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)   // stores "ROLE_USER" not "0"
    @Column(nullable = false)
    private Role role;

    // constructors, getters, setRole()
}
```

`@Enumerated(EnumType.STRING)` stores the enum name as text in the DB (`ROLE_USER`). Without this it stores the ordinal number (0, 1) — which breaks if enum order ever changes.

### UserRepository
```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
```
Spring Data JPA generates the SQL for these methods automatically from the method name — no `@Query` needed.

### Role assignment — why the server controls it
The role is **never** passed from the frontend. `register()` always assigns `ROLE_USER` server-side. If you allowed the frontend to send a role, anyone could register themselves as admin.

---

## 7. auth-service — DTOs as Java Records

Java Records are immutable data carriers — ideal for request/response DTOs. No boilerplate setters, getters, or constructors.

### LoginRequest
```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
```

### RegisterRequest
```java
public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password
) {}
```

### LoginResponse
```java
public record LoginResponse(String token, String tokenType, long expiresIn) {

    public static LoginResponse of(String token, long expiresInMs) {
        return new LoginResponse(token, "Bearer", expiresInMs / 1000);
    }
}
```

`expiresIn` is returned in seconds (divided by 1000) because that is the standard JWT convention.

### Why POST (not GET) for login
- GET requests can be cached and logged with the URL — credentials in a query string would appear in server access logs and browser history.
- GET body may be dropped by proxies.
- GET must be safe and idempotent — login is neither.
- Always use POST for operations that transmit credentials.

---

## 8. auth-service — JWT Implementation

### JwtUtil
```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)          // role embedded in token
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

### Why role comes from JWT, not from the request header
If you read the role from a request header (`X-Role: ADMIN`), any client can fake it. The role is embedded inside the JWT, which is **signed with the server's secret key**. Any tampering invalidates the signature — so the role can be trusted.

### JJWT 0.12.6 API (important change from older versions)
| Old API | New API (0.12.6) |
|---|---|
| `Jwts.parserBuilder()` | `Jwts.parser()` |
| `.setSigningKey()` | `.verifyWith()` |
| `parseClaimsJws()` | `parseSignedClaims()` |

---

## 9. auth-service — Security Filter

`JwtAuthFilter` runs on every request. It reads the `Authorization` header, validates the JWT, and sets the authenticated user in the `SecurityContext`.

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);   // no token — pass through
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);   // invalid token — pass through
            return;
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority(role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
```

`OncePerRequestFilter` guarantees the filter runs exactly once per request (not once per servlet forward or include).

---

## 10. auth-service — Security Config

```java
@Configuration
@EnableMethodSecurity          // enables @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)        // stateless API — no CSRF needed
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

### Why CSRF is disabled
CSRF protection is for browser-based session cookies. This API uses stateless JWTs — no cookies, no sessions, so CSRF does not apply.

### `@EnableMethodSecurity`
Without this, `@PreAuthorize("hasRole('ADMIN')")` on controller methods silently does nothing. This annotation activates method-level security.

---

## 11. auth-service — Service Layer

```java
@Service
public class AuthService {

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return LoginResponse.of(token, expirationMs);
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        UserEntity user = new UserEntity(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER         // server always assigns default role
        );
        userRepository.save(user);
    }

    public void promoteToAdmin(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
    }
}
```

### How BCrypt password validation works
BCrypt adds a random salt before hashing. The same password hashed twice produces different strings. Because of this, you **cannot** query the database by password — there is nothing to match against.

The correct approach:
1. Find user by username
2. Call `passwordEncoder.matches(rawPassword, storedHash)` — BCrypt extracts the salt from the stored hash and re-hashes the raw password with it, then compares

### Bug fixed — promoteToAdmin
The original implementation created `new UserEntity(username, password, ROLE_ADMIN)` — a new entity with no ID. JPA would try to INSERT a second row, failing with a unique constraint violation on username.

**Fix:** Add `setRole()` to `UserEntity` and update the existing entity instead of creating a new one.

---

## 12. auth-service — Controller and Exception Handler

### AuthController
```
POST /auth/login                    — open to everyone (no auth required)
POST /auth/register                 — open to everyone
POST /auth/admin/promote/{username} — requires ROLE_ADMIN
```

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/promote/{username}")
    public ResponseEntity<Void> promote(@PathVariable String username) {
        authService.promoteToAdmin(username);
        return ResponseEntity.ok().build();
    }
}
```

`@Valid` triggers Bean Validation on the request body. If any `@NotBlank` or `@Size` constraint fails, Spring throws `MethodArgumentNotValidException` before the controller method body runs.

### GlobalExceptionHandler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
```

### HTTP status mapping
| Exception | HTTP Status | Scenario |
|---|---|---|
| `BadCredentialsException` | 401 Unauthorized | Wrong password or user not found |
| `IllegalArgumentException` | 409 Conflict | Username already exists / user not found for promote |
| `MethodArgumentNotValidException` | 400 Bad Request | Blank username, short password |

---

## 13. auth-service — Testing Strategy

### What to test and why

| Class | Test type | Reason |
|---|---|---|
| `AuthService` | Unit test (Mockito) | Business logic — login, register, promote |
| `UserRepository` | Integration (`@DataJpaTest`) | Custom queries need real DB verification |
| `JwtUtil` | Unit test | Non-trivial token encode/decode logic |
| `AuthController` | Slice test (`@WebMvcTest`) | HTTP layer — status codes, request validation |
| `SecurityConfig` | No | Covered by `@WebMvcTest` via `@Import` |
| `JwtAuthFilter` | No | Covered by `@WebMvcTest` via `@Import` |
| DTOs / Entity / Role | No | Records and POJOs with no custom logic |

### Test count summary
```
AuthServiceApplicationTests — 1  (context loads)
AuthControllerTest          — 9  (login/register/promote HTTP scenarios)
AuthServiceTest             — 8  (login/register/promote business logic)
UserRepositoryTest          — 5  (findByUsername, existsByUsername, unique constraint)
JwtUtilTest                 — 6  (generate, extract, valid, tampered, expired)
```

---

## 14. auth-service — Test Setup

### JwtUtilTest — unit test without Spring context
`@Value` fields cannot be injected without a Spring context, so use `ReflectionTestUtils.setField()` to inject values directly in `@BeforeEach`.

```java
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1000L);  // already expired
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}
```

### AuthServiceTest — unit test with Mockito
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
    }

    @Test
    void register_shouldSaveUserWithEncodedPasswordAndDefaultRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-password");

        authService.register(new RegisterRequest("alice", "rawpassword"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
    }
}
```

`ArgumentCaptor` captures the object that was passed to `save()` so you can assert what was actually stored.

### UserRepositoryTest — @DataJpaTest
In Spring Boot 4.0.6, `@DataJpaTest` moved to a new package:
```java
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;  // Spring Boot 4.x

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void save_shouldThrowWhenDuplicateUsernameIsInserted() {
        userRepository.save(new UserEntity("alice", "hash1", Role.ROLE_USER));

        assertThatThrownBy(() ->
            userRepository.saveAndFlush(new UserEntity("alice", "hash2", Role.ROLE_USER))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

`saveAndFlush()` forces the INSERT immediately so the constraint violation is thrown inside the test.

### AuthControllerTest — @WebMvcTest
```java
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void promoteShouldReturn200WhenCalledByAdmin() throws Exception {
        doNothing().when(authService).promoteToAdmin("targetuser");
        mockMvc.perform(post("/auth/admin/promote/targetuser"))
                .andExpect(status().isOk());
    }
}
```

**Package change in Spring Boot 4.x:**
```java
// Spring Boot 3.x
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// Spring Boot 4.x
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

---

## 15. Creating the First Admin User

There is no admin user initially. The `POST /auth/admin/promote/{username}` endpoint requires an existing admin to call it — a chicken-and-egg problem. The solution is to INSERT the first admin directly into the database.

### Generate a BCrypt hash
```bash
htpasswd -nbBC 10 admin Admin@123 | cut -d: -f2
```
Or use an online BCrypt generator.

`$2y$` and `$2a$` are both accepted by Spring Security's `BCryptPasswordEncoder`.

### Insert the admin user
```sql
INSERT INTO users (username, password, role)
VALUES ('admin', '$2y$10$aM9F6mKqlaftgv2LtK1zzefCIr/mUNtDpeLBUwiG3ejQuLj5ZIlMG', 'ROLE_ADMIN');
```

Run in MySQL:
```bash
mysql -u root -p ecommerce
```

### After first admin exists
Use the promote endpoint to make future admins — no more direct DB changes needed:
1. Login as `admin` → get JWT
2. Call `POST /auth/admin/promote/{username}` with `Authorization: Bearer <token>`

---

## 16. API Usage with curl

### Register a new user
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```
Response: `201 Created`

### Login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'
```
Response:
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Promote a user to admin (requires admin JWT)
```bash
curl -X POST http://localhost:8081/auth/admin/promote/alice \
  -H "Authorization: Bearer eyJhbGci..."
```
Response: `200 OK`

---

## 17. Common Errors and Fixes

### Error: @SpringBootTest fails — no DataSource configured
```
Failed to configure a DataSource: 'url' attribute is not specified
```
**Cause:** `@SpringBootTest` loads the full context including JPA, which requires a database. MySQL is not available in tests.

**Fix:** Add H2 as a test dependency and create `src/test/resources/application.properties` with H2 config. Spring Boot automatically uses this file during test runs instead of the main one.

---

### Error: All controller tests return 403 Forbidden
**Cause:** `@WebMvcTest` loads Spring Security with default config (all routes locked, CSRF enabled). Your custom `SecurityConfig` is not loaded by default.

**Fix:** Add `@Import(SecurityConfig.class)` to the test class. Also import `JwtAuthFilter.class` and `JwtUtil.class` so the real filter chain is active.

---

### Error: Controller tests return 200 for everything after mocking JwtAuthFilter
**Cause:** `@MockitoBean JwtAuthFilter` replaces the real filter with an empty mock. A void mock does not call `filterChain.doFilter()`, so no request ever reaches the controller and Spring Security passes everything.

**Fix:** Do NOT mock `JwtAuthFilter`. Import the real class:
```java
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtUtil.class, GlobalExceptionHandler.class})
```

---

### Error: ObjectMapper cannot be autowired in @WebMvcTest
**Cause:** In Spring Boot 4.x, `ObjectMapper` is no longer auto-configured in the `@WebMvcTest` slice context.

**Fix:** Create it directly instead of autowiring:
```java
private final ObjectMapper objectMapper = new ObjectMapper();
```

---

### Error: @DataJpaTest class not found
```
package org.springframework.boot.test.autoconfigure.orm.jpa does not exist
```
**Cause:** In Spring Boot 4.x, test slice annotations moved to module-specific packages.

**Fix:** Use the new import:
```java
// Spring Boot 4.x
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
```

---

### Error: promoteToAdmin fails with duplicate entry in production
**Cause:** The original implementation created `new UserEntity(username, password, ROLE_ADMIN)` — a new entity with no ID. JPA performs an INSERT (not UPDATE), which violates the unique constraint on username.

**Fix:** Update the existing entity's role using `setRole()` and save the same object:
```java
user.setRole(Role.ROLE_ADMIN);
userRepository.save(user);
```
Add `setRole(Role role)` to `UserEntity` to enable this.

---

### Error: Context fails to load with jwt.secret not found
```
Could not resolve placeholder 'jwt.secret' in value "${jwt.secret}"
```
**Cause:** `jwt.secret` and `jwt.expiration-ms` are present in `src/test/resources/application.properties` but not in `src/main/resources/application.properties`.

**Fix:** Ensure both properties exist in the main application.properties:
```properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration-ms=86400000
```