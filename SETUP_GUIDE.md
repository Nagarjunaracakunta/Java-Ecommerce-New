# Java Ecommerce — Complete Setup Guide
# Checkstyle · JaCoCo · Docker · SonarQube · Jenkins · GitHub Webhook

---

## Table of Contents
1. [Project Stack](#1-project-stack)
2. [Checkstyle Setup](#2-checkstyle-setup)
3. [JaCoCo Code Coverage](#3-jacoco-code-coverage)
4. [SonarQube Properties in pom.xml](#4-sonarqube-properties-in-pomxml)
5. [Docker — Build and Run the App](#5-docker--build-and-run-the-app)
6. [Docker Compose — App + SonarQube + Jenkins](#6-docker-compose--app--sonarqube--jenkins)
7. [Jenkins Setup](#7-jenkins-setup)
8. [Jenkinsfile Pipeline](#8-jenkinsfile-pipeline)
9. [SonarQube Integration](#9-sonarqube-integration)
10. [GitHub Webhook — Auto Trigger Jenkins on Push](#10-github-webhook--auto-trigger-jenkins-on-push)
11. [Common Errors and Fixes](#11-common-errors-and-fixes)
12. [Quick Reference Commands](#12-quick-reference-commands)

---

## 1. Project Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.6 | Framework |
| Maven | 3.9 | Build tool |
| JaCoCo | 0.8.12 | Code coverage |
| Checkstyle | 3.6.0 | Code style (Google checks) |
| SonarQube | lts-community | Code quality analysis |
| Jenkins | lts-jdk21 | CI/CD pipeline |
| Docker | 28+ | Containerisation |
| ngrok | latest | Expose local Jenkins to GitHub |

---

## 2. Checkstyle Setup

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
```

### Key points
- Runs automatically during `mvn verify`
- Uses Google style rules (`google_checks.xml`)
- Only fails the build if a rule has `severity="error"` (warnings don't fail the build)
- Does NOT affect `mvn compile` — only `verify` phase and above

---

## 3. JaCoCo Code Coverage

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

## 4. SonarQube Properties in pom.xml

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

## 5. Docker — Build and Run the App

### Dockerfile (multi-stage build)
```dockerfile
# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### .dockerignore
```
target/
.git/
.idea/
*.iml
.mvn/
mvnw
mvnw.cmd
```

### Build and run commands
```bash
# Build image
docker build -t java-ecommerce:latest .

# Run container
docker run -d --name java-ecommerce -p 8080:8080 java-ecommerce:latest

# View logs
docker logs java-ecommerce

# Stop and remove
docker stop java-ecommerce && docker rm java-ecommerce
```

### Key concept
The Docker image is only for **running** the application.
Jenkins validates the **source code** — it does not use the app image at all.

---

## 6. Docker Compose — App + SonarQube + Jenkins

### docker-compose.yml
```yaml
services:
  sonarqube:
    image: sonarqube:lts-community
    container_name: sonarqube
    ports:
      - "9000:9000"
    environment:
      - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs
      - sonarqube_extensions:/opt/sonarqube/extensions
    networks:
      - ecommerce-network

  app:
    image: java-ecommerce:latest
    container_name: java-ecommerce
    ports:
      - "8080:8080"
    networks:
      - ecommerce-network

  jenkins:
    build: ./jenkins
    container_name: jenkins
    ports:
      - "8090:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
    networks:
      - ecommerce-network
    depends_on:
      - sonarqube

networks:
  ecommerce-network:
    driver: bridge

volumes:
  sonarqube_data:
  sonarqube_logs:
  sonarqube_extensions:
  jenkins_home:
```

### Container roles
| Container | Port | Purpose |
|---|---|---|
| `sonarqube` | 9000 | Code quality server |
| `java-ecommerce` (app) | 8080 | Running the application |
| `jenkins` | 8090 | CI/CD pipeline runner |

### Docker Compose commands
```bash
# Start all containers
docker compose up -d

# Start specific container
docker compose up -d jenkins

# Stop all (data preserved)
docker compose down

# Stop all and DELETE all data (volumes)
docker compose down -v     # WARNING: wipes everything

# Recreate a specific container (pick up config changes)
docker compose up -d --force-recreate jenkins

# View logs
docker logs sonarqube
docker logs jenkins
```

### Data persistence
All configuration is stored in named volumes and survives restarts:
```bash
docker volume ls
# java-ecommerce-new_jenkins_home
# java-ecommerce-new_sonarqube_data
# java-ecommerce-new_sonarqube_logs
# java-ecommerce-new_sonarqube_extensions
```

---

## 7. Jenkins Setup

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
docker run --rm -v java-ecommerce-new_jenkins_home:/var/jenkins_home alpine \
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

## 8. Jenkinsfile Pipeline

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
                junit 'target/surefire-reports/*.xml'
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

### Pipeline stages explained
| Stage | Command | What it validates |
|---|---|---|
| Checkout | `checkout scm` | Pulls latest code from GitHub |
| Build | `mvn clean compile` | No compilation errors |
| Test & Coverage | `mvn verify` | Tests pass + JaCoCo ≥ 80% |
| SonarQube | `mvn sonar:sonar` | Code quality pushed to SonarQube |

### Configure pipeline job in Jenkins
1. **New Item** → enter name `java-ecommerce` → select **Pipeline** → OK
2. Under **Build Triggers** → check **GitHub hook trigger for GITScm polling**
3. Under **Pipeline** → Definition: **Pipeline script from SCM**
   - SCM: `Git`
   - Repository URL: `https://github.com/<your-username>/<repo>.git`
   - Credentials: your GitHub credentials
   - Branch: `*/main` (or your branch)
   - Script Path: `Jenkinsfile`
4. Click **Save**
5. Click **Build Now** once manually to register the `githubPush()` trigger

---

## 9. SonarQube Integration

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

## 10. GitHub Webhook — Auto Trigger Jenkins on Push

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
      ├── Build
      ├── Test & Coverage (JaCoCo check)
      └── SonarQube Analysis → http://localhost:9000
```

---

## 11. Common Errors and Fixes

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
**Fix:** You ran `mvn checkstyle:` without specifying the goal. Run `mvn checkstyle:check` (with `:check`).

---

### Error: SonarQube not authorized
```
Not authorized. Please provide a user token in sonar.login
```
**Fix:** The token was wrong or missing. Use property name `sonar.login` (not `sonar.token`) and make sure the actual token value is set (not the placeholder).

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
**Cause:** `junit` step was in the `post` block which runs outside the `dir()` context.
**Fix:** Move `junit` inside the `dir` block:
```groovy
dir('/var/jenkins_home/project') {
    sh 'mvn verify'
    junit 'target/surefire-reports/*.xml'
}
```

---

### Jenkins login — forgot password
If you can't log in:
```bash
docker stop jenkins
docker run --rm -v java-ecommerce-new_jenkins_home:/var/jenkins_home alpine \
  sed -i 's/<useSecurity>true<\/useSecurity>/<useSecurity>false<\/useSecurity>/' \
  /var/jenkins_home/config.xml
docker start jenkins
```
Then go to `http://localhost:8090` (no password) → Manage Jenkins → Users → reset password → re-enable security.

---

## 12. Quick Reference Commands

### Maven
```bash
mvn clean compile          # compile only
mvn test                   # run tests
mvn verify                 # compile + test + coverage check + checkstyle
mvn checkstyle:check       # run checkstyle only
mvn sonar:sonar            # run sonar analysis
mvn package -DskipTests    # build JAR without tests
```

### Docker
```bash
docker build -t java-ecommerce:latest .          # build app image
docker run -d -p 8080:8080 java-ecommerce:latest # run app
docker logs <container-name>                      # view logs
docker ps                                         # list running containers
docker ps -a                                      # list all containers
docker images                                     # list images
docker volume ls                                  # list volumes
```

### Docker Compose
```bash
docker compose up -d                              # start all
docker compose down                               # stop all (data safe)
docker compose down -v                            # stop all + delete data
docker compose up -d --force-recreate jenkins     # recreate jenkins
docker compose restart sonarqube                  # restart sonarqube
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
| App | http://localhost:8080 | user / password |
| SonarQube | http://localhost:9000 | admin / admin |
| Jenkins | http://localhost:8090 | your admin user |