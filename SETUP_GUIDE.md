# Java Ecommerce — Complete Setup Guide
# Microservices · Docker Compose · ELK Observability · SonarQube · Jenkins · GitHub Webhook

---

## Table of Contents
1. [Project Stack](#1-project-stack)
2. [Microservices Overview](#2-microservices-overview)
3. [Checkstyle Setup](#3-checkstyle-setup)
4. [JaCoCo Code Coverage](#4-jacoco-code-coverage)
5. [SonarQube Properties in pom.xml](#5-sonarqube-properties-in-pomxml)
6. [Dockerfile — Per-Service Multi-Stage Build](#6-dockerfile--per-service-multi-stage-build)
7. [Docker Compose — Full Stack](#7-docker-compose--full-stack)
8. [ELK Stack — Observability](#8-elk-stack--observability)
9. [Jenkins Setup](#9-jenkins-setup)
10. [Jenkinsfile Pipeline](#10-jenkinsfile-pipeline)
11. [SonarQube Integration](#11-sonarqube-integration)
12. [GitHub Webhook — Auto Trigger Jenkins on Push](#12-github-webhook--auto-trigger-jenkins-on-push)
13. [Common Errors and Fixes](#13-common-errors-and-fixes)
14. [Quick Reference Commands](#14-quick-reference-commands)

---

## 1. Project Stack

### Backend
| Tool | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.6 | Framework |
| Maven | 3.9 | Build tool (multi-module) |
| JaCoCo | 0.8.12 | Code coverage |
| Checkstyle | 3.6.0 | Code style (Google checks) |

### Frontend
| Tool | Version | Purpose |
|---|---|---|
| Angular | 16.2.x | SPA framework |
| Angular Material | 16.2.x | UI component library |
| Node.js | 18 LTS | Build runtime |
| nginx | 1.27-alpine | Static file server (in Docker) |

### Data Stores
| Store | Version | Used by |
|---|---|---|
| MySQL | 8.0 | auth-service, product-service |
| PostgreSQL | 16-alpine | order-service, payment-service |
| Redis | 7-alpine | cart-service |
| MongoDB | 7.0 | notification-service |
| Apache Kafka | 3.7.0 (KRaft) | order-service → payment-service → notification-service |

### Infrastructure
| Tool | Version | Purpose |
|---|---|---|
| Docker | 28+ | Containerisation |
| SonarQube | lts-community | Code quality analysis |
| Jenkins | lts-jdk21 | CI/CD pipeline |
| Elasticsearch | 8.11.0 | Log storage and search |
| Logstash | 8.11.0 | Log ingestion (TCP → Elasticsearch) |
| Kibana | 8.11.0 | Log visualisation dashboard |
| ngrok | latest | Expose local Jenkins to GitHub |

---

## 2. Microservices Overview

```
Browser (port 4200)
      │
      ▼
ecommerce-frontend (nginx, port 80→4200)
      │ HTTP
      ▼
api-gateway (port 8080) ── JWT validation ──► auth-service (port 8081)
      │                                               │ MySQL
      ├──► product-service (port 8082) ── MySQL
      │
      ├──► cart-service (port 8083) ── Redis
      │
      ├──► order-service (port 8084) ── PostgreSQL
      │           │ Kafka: order.created
      │           ▼
      │    payment-service (port 8085) ── PostgreSQL
      │           │ Kafka: payment.success / payment.failed
      │           ▼
      │    notification-service (port 8087) ── MongoDB
      │
      └── (all services send logs to Logstash:5000)

ELK Stack:
  Logstash (port 5000 TCP) → Elasticsearch (port 9200) → Kibana (port 5601)
```

### Port reference
| Service | Container port | Host port |
|---|---|---|
| api-gateway | 8080 | 8080 |
| auth-service | 8081 | 8081 |
| product-service | 8082 | 8082 |
| cart-service | 8083 | 8083 |
| order-service | 8084 | 8084 |
| payment-service | 8085 | 8085 |
| notification-service | 8087 | 8087 |
| ecommerce-frontend | 80 | 4200 |
| MySQL | 3306 | 3307 |
| PostgreSQL | 5432 | 5433 |
| MongoDB | 27017 | 27017 |
| Redis | 6379 | 6379 |
| Kafka | 9092 | 9092 |
| Elasticsearch | 9200 | 9200 |
| Kibana | 5601 | 5601 |
| SonarQube | 9000 | 9000 |
| Jenkins | 8080 | 8090 |

---

## 3. Checkstyle Setup

### pom.xml plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>
        <failOnViolation>true</failOnViolation>
        <consoleOutput>true</consoleOutput>
    </configuration>
    <executions>
        <execution>
            <id>checkstyle</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Run checkstyle manually
```bash
mvn checkstyle:check
# or for a specific module:
mvn checkstyle:check -pl auth-service
```

### Key points
- Runs automatically during `mvn verify`
- Uses Google style rules (`google_checks.xml`)
- Only fails the build if a rule has `severity="error"` (warnings don't fail the build)
- Does NOT affect `mvn compile` — only `verify` phase and above

---

## 4. JaCoCo Code Coverage

### pom.xml plugin (version is mandatory — checkstyle enforces it)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <!-- Instruments bytecode before tests run -->
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <!-- Generates HTML/XML report after tests -->
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <!-- Enforces minimum coverage threshold -->
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Key points
| Behaviour | Detail |
|---|---|
| Minimum coverage | 80% line coverage |
| Fails build if below threshold | Yes, during `mvn verify` |
| Report location | `target/site/jacoco/index.html` |
| Does NOT affect compile | Correct — only `verify` phase |

---

## 5. SonarQube Properties in pom.xml

Add inside `<properties>`:
```xml
<sonar.host.url>http://localhost:9000</sonar.host.url>
<sonar.projectKey>java-ecommerce</sonar.projectKey>
<sonar.projectName>Java Ecommerce New</sonar.projectName>
<sonar.login>YOUR_SONAR_TOKEN</sonar.login>
<sonar.coverage.jacoco.xmlReportPaths>
    ${project.build.directory}/site/jacoco/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
```

### Run analysis manually
```bash
mvn sonar:sonar
```

> **Security:** Never commit `sonar.login` token to Git. Use environment variable instead:
> ```bash
> mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN
> ```

---

## 6. Dockerfile — Per-Service Multi-Stage Build

Each microservice has its own `Dockerfile` at the project root level (not inside the service folder). The build context is always the **project root** so Maven can access the parent `pom.xml` and all sibling module poms.

### Pattern (example: auth-service/Dockerfile)
```dockerfile
# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy all pom files first — Docker caches this layer until a pom changes
COPY pom.xml .
COPY auth-service/pom.xml    auth-service/
COPY product-service/pom.xml product-service/
COPY api-gateway/pom.xml     api-gateway/
COPY cart-service/pom.xml    cart-service/
COPY order-service/pom.xml   order-service/
COPY payment-service/pom.xml  payment-service/
COPY notification-service/pom.xml notification-service/

# Install parent pom to local repo (non-recursive — pom packaging only)
RUN mvn install -N -q

# Pre-download dependencies (cached until pom changes)
RUN mvn dependency:go-offline -pl auth-service -q

# Copy source and build
COPY auth-service/src auth-service/src
RUN mvn package -pl auth-service -DskipTests -q

# Stage 2 — Runtime (JRE only — smaller image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Why this pattern?
- **Two-stage build**: The builder stage has the full JDK + Maven; the runtime stage has only a JRE → smaller final image
- **Pom files copied first**: Docker layer caching means dependency downloads are skipped unless a `pom.xml` changes
- **`-pl <module>`**: Maven's reactor flag builds only the specified module, pulling parent pom from local repo
- **Context is project root**: Each service's Dockerfile references `COPY auth-service/src` — this only works if `docker build` is run from the project root (which `docker compose` handles automatically)

### Frontend Dockerfile (ecommerce-frontend/Dockerfile)
```dockerfile
# Stage 1: Build Angular app
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
RUN npx ng build --configuration production

# Stage 2: Serve with nginx
FROM nginx:1.27-alpine
COPY --from=builder /app/dist/ecommerce-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

> **Angular 16 build output:** The build outputs to `dist/ecommerce-frontend/` (no `/browser/` subfolder — that subfolder was introduced in Angular 17+).

---

## 7. Docker Compose — Full Stack

The `docker-compose.yml` defines the complete infrastructure. All services share `ecommerce-network`.

### Start everything
```bash
# From project root — builds all service images and starts containers
docker compose up -d

# Watch startup logs (Ctrl+C exits tail, containers keep running)
docker compose logs -f

# Start only infrastructure (databases + Kafka) without app services
docker compose up -d mysql postgres redis mongodb kafka
```

### Service startup order (dependency chain)
```
MySQL / PostgreSQL / Redis / MongoDB / Kafka  (infrastructure)
        │
        ▼
auth-service, product-service  (depend on MySQL)
        │
        ▼
api-gateway  (depends on auth-service, product-service)
cart-service  (depends on Redis, product-service)
        │
        ▼
order-service  (depends on PostgreSQL, cart-service, Kafka)
payment-service  (depends on PostgreSQL, Kafka)
notification-service  (depends on MongoDB, Kafka)
        │
        ▼
ecommerce-frontend  (depends on api-gateway)
        │
Elasticsearch → Logstash (receives logs from all services)
             → Kibana (reads from Elasticsearch)
```

Health checks ensure each service only starts after its dependency is truly ready (not just running).

### Key environment variables
All services receive `LOGSTASH_HOST: logstash` — this tells `logback-spring.xml` where to ship logs.

| Variable | Services | Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | auth, product | `jdbc:mysql://mysql:3306/ecommerce` |
| `SPRING_DATASOURCE_URL` | order, payment | `jdbc:postgresql://postgres:5432/orderdb` |
| `SPRING_MONGODB_URI` | notification | `mongodb://mongodb:27017/notificationdb` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | notification | `kafka:9092` |
| `JWT_SECRET` | all backend services | shared HMAC-SHA key |
| `LOGSTASH_HOST` | all backend services | `logstash` |

> **Note:** PostgreSQL runs two databases: `orderdb` (default) and `paymentdb` (created by `postgres/init.sql`). The init script runs automatically on first container start.

### Docker Compose commands
```bash
# Start all containers
docker compose up -d

# Stop all (data preserved in volumes)
docker compose down

# Stop all and DELETE all data (volumes)
docker compose down -v     # WARNING: wipes all databases and volumes

# Rebuild a specific service image and restart it
docker compose up -d --build auth-service

# Recreate a specific container without rebuilding (picks up config changes)
docker compose up -d --force-recreate jenkins

# View logs for a specific service
docker compose logs -f api-gateway
docker compose logs -f notification-service

# View all running containers and their health
docker compose ps
```

### Named volumes (data persistence)
```bash
docker volume ls
# Volumes survive docker compose down (but NOT docker compose down -v):
# ecommerce-new_mysql-data
# ecommerce-new_postgres-data
# ecommerce-new_mongodb-data
# ecommerce-new_redis-data
# ecommerce-new_elasticsearch-data
# ecommerce-new_sonarqube_data / _logs / _extensions
# ecommerce-new_jenkins_home
```

### Container roles quick reference
| Container | Host Port | Purpose |
|---|---|---|
| `ecommerce-gateway` | 8080 | API gateway — single entry point for all clients |
| `ecommerce-auth` | 8081 | JWT auth: register, login |
| `ecommerce-products` | 8082 | Product catalog CRUD |
| `ecommerce-cart` | 8083 | Redis-backed shopping cart |
| `ecommerce-orders` | 8084 | Order lifecycle management |
| `ecommerce-payments` | 8085 | Payment processing |
| `ecommerce-notifications` | 8087 | Notification feed (MongoDB) |
| `ecommerce-frontend` | 4200 | Angular 16 SPA served by nginx |
| `ecommerce-mysql` | 3307 | MySQL for auth + products |
| `ecommerce-postgres` | 5433 | PostgreSQL for orders + payments |
| `ecommerce-redis` | 6379 | Redis for cart storage |
| `ecommerce-mongodb` | 27017 | MongoDB for notifications |
| `ecommerce-kafka` | 9092 | Kafka event bus (KRaft mode, no ZooKeeper) |
| `ecommerce-elasticsearch` | 9200 | Log storage |
| `ecommerce-logstash` | 5000 (TCP) | Log ingestion |
| `ecommerce-kibana` | 5601 | Log visualisation |
| `sonarqube` | 9000 | Code quality |
| `jenkins` | 8090 | CI/CD |

---

## 8. ELK Stack — Observability

The ELK (Elasticsearch, Logstash, Kibana) stack collects structured JSON logs from all microservices.

### How it works

```
Spring Boot service
  └── logback-spring.xml
        └── LogstashTcpSocketAppender → Logstash:5000 (TCP, JSON)
                                              │
                                              ▼
                                       Logstash pipeline
                                         (parse + timestamp)
                                              │
                                              ▼
                                       Elasticsearch
                                         index: ecommerce-logs-{service}-{date}
                                              │
                                              ▼
                                           Kibana
                                         (http://localhost:5601)
```

### logback-spring.xml (each service)

Each service has a `src/main/resources/logback-spring.xml`:
```xml
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>
    <springProperty scope="context" name="LOGSTASH_HOST" source="logstash.host" defaultValue="localhost"/>

    <!-- Console appender for local dev -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Logstash appender — ships structured JSON logs over TCP -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${LOGSTASH_HOST}:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service_name":"${APP_NAME}"}</customFields>
        </encoder>
        <reconnectionDelay>5 seconds</reconnectionDelay>
        <keepAliveDuration>5 minutes</keepAliveDuration>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

### Logstash pipeline (`logstash/pipeline/logstash.conf`)
```ruby
input {
  tcp {
    port => 5000
    codec => json_lines
  }
}

filter {
  date {
    match => ["@timestamp", "ISO8601"]
    target => "@timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "ecommerce-logs-%{[service_name]}-%{+YYYY.MM.dd}"
    manage_template => false
  }
}
```

Each service's logs go into a separate daily index (e.g., `ecommerce-logs-auth-service-2026.05.20`).

### Kibana setup (first time)

1. Open `http://localhost:5601`
2. Go to **Stack Management → Index Patterns → Create index pattern**
3. Name: `ecommerce-logs-*` → timestamp field: `@timestamp` → Create
4. Go to **Discover** → select `ecommerce-logs-*` → search logs across all services

### Useful Kibana queries

```
# All ERROR logs across all services
level: "ERROR"

# Logs from a specific service
service_name: "auth-service"

# Errors in order-service in the last 15 minutes
service_name: "order-service" AND level: "ERROR"

# Logs containing a specific message
message: "Payment"
```

### Startup time note
Elasticsearch takes ~60 seconds to be ready after `docker compose up`. Logstash waits for Elasticsearch to be healthy before starting. If logs don't appear in Kibana immediately, wait 2–3 minutes after the full stack is up.

---

## 9. Jenkins Setup

### jenkins/Dockerfile
```dockerfile
FROM jenkins/jenkins:lts-jdk21
USER root
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
USER jenkins
```

### First-time setup
1. Open `http://localhost:8090`
2. Get initial admin password:
   ```bash
   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Click **Install suggested plugins**
4. Create admin user

### Reset Jenkins password (if locked out)
```bash
# Stop Jenkins
docker stop jenkins

# Disable security in config
docker run --rm -v ecommerce-new_jenkins_home:/var/jenkins_home alpine \
  sed -i 's/<useSecurity>true<\/useSecurity>/<useSecurity>false<\/useSecurity>/' \
  /var/jenkins_home/config.xml

# Start Jenkins (no login required)
docker start jenkins
```
Then go to **Manage Jenkins → Users** → reset password → re-enable security.

### Fix Maven .m2 permissions error
If you see `Could not create local repository at /var/jenkins_home/.m2/repository`:
```bash
docker exec -u root jenkins mkdir -p /var/jenkins_home/.m2/repository
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/.m2
```

---

## 10. Jenkinsfile Pipeline

```groovy
pipeline {
    agent any

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -q'
            }
        }

        stage('Test & Coverage') {
            steps {
                sh 'mvn verify'
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                sh 'mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
```

> **Multi-module note:** `junit '**/target/surefire-reports/*.xml'` uses a glob that matches test reports from all Maven submodules. The `mvn verify` command runs against the parent pom by default and builds all modules.

### Pipeline stages explained
| Stage | Command | What it validates |
|---|---|---|
| Checkout | `checkout scm` | Pulls latest code from GitHub |
| Build | `mvn clean compile` | No compilation errors across all modules |
| Test & Coverage | `mvn verify` | Tests pass + JaCoCo ≥ 80% per module |
| SonarQube | `mvn sonar:sonar` | Code quality pushed to SonarQube |

### Configure pipeline job in Jenkins
1. **New Item** → enter name `java-ecommerce` → select **Pipeline** → OK
2. Under **Build Triggers** → check **GitHub hook trigger for GITScm polling**
3. Under **Pipeline** → Definition: **Pipeline script from SCM**
   - SCM: `Git`
   - Repository URL: `https://github.com/Nagarjunaracakunta/Java-Ecommerce-New.git`
   - Credentials: your GitHub credentials
   - Branch: `*/feature/commandlineargs-slf4j` (or your target branch)
   - Script Path: `Jenkinsfile`
4. Click **Save**
5. Click **Build Now** once manually to register the `githubPush()` trigger

---

## 11. SonarQube Integration

### First-time SonarQube setup
1. Open `http://localhost:9000`
2. Login: `admin` / `admin` (change password on first login)
3. Generate token:
   - Top right avatar → **My Account** → **Security** tab
   - Enter token name → **Generate** → copy the token (starts with `squ_...`)
4. Add token to `pom.xml` as `<sonar.login>YOUR_TOKEN</sonar.login>`

### Run analysis
```bash
mvn sonar:sonar
```

### View results
- Dashboard: `http://localhost:9000/dashboard?id=java-ecommerce`
- The project only appears after the first analysis run

### SonarQube URL from within Jenkins (Docker network)
When Jenkins runs sonar analysis, it uses the Docker network hostname:
```
http://sonarqube:9000
```
not `http://localhost:9000` — because both containers are on `ecommerce-network`.

---

## 12. GitHub Webhook — Auto Trigger Jenkins on Push

### Install ngrok
```bash
brew install ngrok
ngrok config add-authtoken YOUR_NGROK_AUTHTOKEN
ngrok http 8090
```
Copy the forwarding URL, e.g. `https://abc123.ngrok-free.app`

### Set Jenkins URL
Go to **Manage Jenkins** → **System** → **Jenkins URL**:
```
https://abc123.ngrok-free.app
```

### Add webhook in GitHub
1. GitHub repo → **Settings** → **Webhooks** → **Add webhook**
2. Payload URL: `https://abc123.ngrok-free.app/github-webhook/`  ← trailing slash is mandatory
3. Content type: `application/json`
4. Event: **Just the push event**
5. Click **Add webhook**

### Verify webhook is working
- ngrok terminal should show: `POST /github-webhook/ 200 OK`
- GitHub → Settings → Webhooks → Recent Deliveries → green tick

### Complete flow
```
git push origin main
      │
      ▼
GitHub sends webhook → ngrok → Jenkins (localhost:8090)
      │
      ▼
Jenkins pulls code from GitHub → runs pipeline
      │
      ├── Build (all modules)
      ├── Test & Coverage (JaCoCo check per module)
      └── SonarQube Analysis → http://localhost:9000
```

---

## 13. Common Errors and Fixes

### Error: Service fails to start — dependency not ready

**Symptom:** A service exits immediately with a connection error to MySQL/Kafka/etc.

**Cause:** Docker Compose's `depends_on` with `condition: service_healthy` waits for the health check to pass, but the service might still fail if the health check window hasn't elapsed.

**Fix:** Services have `restart: on-failure` — they restart automatically. Wait 30–60 seconds for all infrastructure to be healthy:
```bash
docker compose ps   # check STATUS column — should show "healthy" or "running"
docker compose logs -f auth-service   # watch startup logs
```

---

### Error: Logstash not receiving logs

**Symptom:** No logs appear in Kibana despite services running.

**Cause:** Logstash starts only after Elasticsearch is healthy, which takes ~60 seconds. Services connecting before Logstash is ready use `reconnectionDelay: 5s` to retry.

**Fix:** Wait 2–3 minutes after `docker compose up` before checking Kibana. Verify with:
```bash
docker compose ps logstash      # should be "running"
docker compose logs logstash    # check for "Pipelines running"
```

---

### Error: Kafka consumer not receiving messages

**Symptom:** Order placed but status stays PENDING; no notifications arrive.

**Cause:** Kafka in KRaft mode takes ~40 seconds to be ready. Services connecting early may fail.

**Fix:** Kafka has a health check (`condition: service_healthy`). If the issue persists:
```bash
docker compose logs kafka            # check for STARTED
docker compose restart order-service payment-service notification-service
```

---

### Error: JaCoCo version missing
```
'build.plugins.plugin.version' for org.jacoco:jacoco-maven-plugin is missing
```
**Fix:** Add `<version>0.8.12</version>` to the jacoco plugin in pom.xml.

---

### Error: Checkstyle goal not found
```
Could not find goal '' in plugin maven-checkstyle-plugin:3.6.0
```
**Fix:** Run `mvn checkstyle:check` (with `:check` — not just `mvn checkstyle:`).

---

### Error: SonarQube not authorized
```
Not authorized. Please provide a user token in sonar.login
```
**Fix:** The token was wrong or missing. Use property name `sonar.login` (not `sonar.token`) and ensure the actual token value is set (not the placeholder).

---

### Error: Jenkins workspace access denied
```
java.nio.file.AccessDeniedException: /var/project@tmp
```
**Fix:** The mount point `/var/project` is inside `/var` which is owned by root. Move mount to a path Jenkins owns, e.g. `/var/jenkins_home/project`.

---

### Error: Maven cannot create .m2 repository
```
Could not create local repository at /var/jenkins_home/.m2/repository
```
**Fix:**
```bash
docker exec -u root jenkins mkdir -p /var/jenkins_home/.m2/repository
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/.m2
```

---

### Error: Webhook returns 302 (pipeline not triggering)
**Cause:** GitHub webhook URL is missing the trailing slash.
**Fix:** Change `https://xxxx.ngrok-free.app/github-webhook` to `https://xxxx.ngrok-free.app/github-webhook/`

---

### Error: Webhook returns 200 but pipeline still not triggering
**Cause:** The `githubPush()` trigger in Jenkinsfile only registers after the first manual build.
**Fix:** Go to Jenkins → click **Build Now** once manually. After that, all pushes auto-trigger.

---

### Error: No test report files were found
```
hudson.AbortException: No test report files were found. Configuration error?
```
**Fix:** Use a glob pattern that covers all Maven submodules:
```groovy
junit '**/target/surefire-reports/*.xml'
```

---

### Error: npm install fails with peer dependency errors
**Cause:** Angular 16 has strict peer version requirements.
**Fix:** Use `--legacy-peer-deps`:
```bash
npm install --legacy-peer-deps
```
The frontend Dockerfile already uses `npm ci --legacy-peer-deps` for the Docker build.

---

### Jenkins login — forgot password
```bash
docker stop jenkins
docker run --rm -v ecommerce-new_jenkins_home:/var/jenkins_home alpine \
  sed -i 's/<useSecurity>true<\/useSecurity>/<useSecurity>false<\/useSecurity>/' \
  /var/jenkins_home/config.xml
docker start jenkins
```
Then go to `http://localhost:8090` (no password) → Manage Jenkins → Users → reset password → re-enable security.

---

## 14. Quick Reference Commands

### Maven (run from project root)
```bash
mvn clean compile              # compile all modules
mvn test                       # run tests (all modules)
mvn verify                     # compile + test + coverage + checkstyle (all modules)
mvn verify -pl auth-service    # verify a single module
mvn checkstyle:check           # checkstyle only
mvn sonar:sonar                # sonar analysis
mvn package -DskipTests        # build all JARs without tests
```

### Docker
```bash
docker build -t java-ecommerce:latest .          # build app image
docker logs <container-name>                      # view logs
docker ps                                         # list running containers
docker ps -a                                      # list all containers
docker images                                     # list images
docker volume ls                                  # list volumes
docker exec -it <container> sh                   # shell into a container
```

### Docker Compose
```bash
docker compose up -d                              # start all
docker compose up -d --build                      # rebuild + start all
docker compose down                               # stop all (data safe)
docker compose down -v                            # stop all + delete data
docker compose up -d --build auth-service         # rebuild + restart one service
docker compose up -d --force-recreate jenkins     # recreate without rebuild
docker compose restart sonarqube                  # restart sonarqube
docker compose logs -f api-gateway                # tail logs for one service
docker compose ps                                 # status of all containers
```

### Jenkins
```bash
# Get initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Fix Maven permissions
docker exec -u root jenkins chown -R jenkins:jenkins /var/jenkins_home/.m2
```

### ngrok
```bash
ngrok config add-authtoken YOUR_TOKEN    # one-time setup
ngrok http 8090                          # expose Jenkins
```

### URLs
| Service | URL | Credentials |
|---|---|---|
| Frontend | http://localhost:4200 | register a new user |
| API Gateway | http://localhost:8080 | — |
| Kibana (logs) | http://localhost:5601 | no auth (security disabled) |
| SonarQube | http://localhost:9000 | admin / admin |
| Jenkins | http://localhost:8090 | your admin user |
| Elasticsearch | http://localhost:9200 | — |