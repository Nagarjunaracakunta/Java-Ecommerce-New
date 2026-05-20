# EcommerceHub — Quick Reference

A microservices food-delivery platform built with Spring Boot 4, Angular 16, Kafka, and the ELK stack.

---

## Guides in this repo

| Guide | What it covers |
|---|---|
| [SETUP_GUIDE.md](SETUP_GUIDE.md) | Docker Compose, ELK stack, Jenkins, SonarQube, GitHub webhook |
| [SERVICES_GUIDE.md](SERVICES_GUIDE.md) | Every microservice — architecture, code patterns, API usage |
| [ANGULAR_GUIDE.md](ANGULAR_GUIDE.md) | Angular 16 frontend — components, guards, interceptors, routing |
| [OBSERVABILITY_GUIDE.md](OBSERVABILITY_GUIDE.md) | Kibana dashboards, log queries, ELK setup |

---

## Quick Start

```bash
# Start the full stack (all services + databases + ELK + Jenkins + SonarQube)
docker compose up -d

# Watch logs as services come up
docker compose logs -f

# Open the app
open http://localhost:4200
```

Services take ~60–90 seconds to be healthy (Elasticsearch is the slowest).

---

## Service URLs

| Service | URL | Notes |
|---|---|---|
| Frontend | http://localhost:4200 | Angular 16 SPA |
| API Gateway | http://localhost:8080 | Single entry point for all API calls |
| Kibana | http://localhost:5601 | Log visualisation |
| SonarQube | http://localhost:9000 | Code quality (admin / admin) |
| Jenkins | http://localhost:8090 | CI/CD pipeline |

---

## Common Commands

### Docker Compose
```bash
docker compose up -d                        # start everything
docker compose up -d --build                # rebuild images + start
docker compose down                         # stop (data safe)
docker compose down -v                      # stop + wipe all volumes
docker compose logs -f <service>            # tail logs for one service
docker compose ps                           # status of all containers
docker compose up -d --build auth-service   # rebuild one service
docker compose restart notification-service # restart one service
```

### Maven
```bash
mvn clean compile              # compile all modules
mvn test                       # run all tests
mvn verify                     # compile + test + coverage (JaCoCo ≥ 80%) + checkstyle
mvn verify -pl auth-service    # verify a single module
mvn checkstyle:check           # run checkstyle only
mvn sonar:sonar                # push analysis to SonarQube
mvn package -DskipTests        # build all JARs without running tests
```

### Docker
```bash
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker exec -it ecommerce-mysql mysql -u root -p    # MySQL shell
docker exec -it ecommerce-redis redis-cli           # Redis shell
docker logs <container-name>                        # view logs
```

### Jenkins
```bash
# Get initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Fix Maven .m2 permissions
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/.m2
```

---

## Test Accounts

Register via the app (`/register`) or curl:

```bash
# Register a user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

### Create the first admin (direct DB insert)
```bash
# Generate BCrypt hash for your password
htpasswd -nbBC 10 admin Admin@123 | cut -d: -f2

# Insert into MySQL
docker exec -it ecommerce-mysql mysql -u root -ppassword ecommerce \
  -e "INSERT INTO users (username, password, role) VALUES ('admin', '<bcrypt-hash>', 'ROLE_ADMIN');"
```

---

## SonarQube Analysis

```bash
# Generate a token: http://localhost:9000 → My Account → Security → Generate
mvn sonar:sonar -Dsonar.login=<YOUR_TOKEN>

# Or set in pom.xml properties (do NOT commit the token — use env var)
mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN
```

---

## Reference Documentation

- [Spring Boot 4.0.6](https://docs.spring.io/spring-boot/4.0.6/reference/)
- [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.6/reference/data/sql.html)
- [Spring Security](https://docs.spring.io/spring-boot/4.0.6/reference/web/spring-security.html)
- [Spring WebFlux](https://docs.spring.io/spring-boot/4.0.6/reference/web/reactive.html)
- [Apache Kafka with Spring](https://docs.spring.io/spring-kafka/reference/)
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
- [Angular 16](https://v16.angular.io/docs)
- [Angular Material 16](https://v16.material.angular.io/)
- [JJWT 0.12.x](https://github.com/jwtk/jjwt)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Maven Multi-Module](https://maven.apache.org/guides/mini/guide-multiple-modules.html)