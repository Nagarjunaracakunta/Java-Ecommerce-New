# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy all pom files first — Docker caches this layer until a pom changes
COPY pom.xml .
COPY auth-service/pom.xml    auth-service/
COPY product-service/pom.xml product-service/
COPY api-gateway/pom.xml     api-gateway/

# Install parent pom to local repo (non-recursive — pom packaging only)
RUN mvn install -N -q

# Pre-download auth-service dependencies (cached until pom changes)
RUN mvn dependency:go-offline -pl auth-service -q

# Copy source and build
COPY auth-service/src auth-service/src
RUN mvn package -pl auth-service -DskipTests -q

# Stage 2 — Runtime (JRE only — smaller image)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
