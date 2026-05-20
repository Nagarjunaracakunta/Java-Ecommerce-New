# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY auth-service/pom.xml    auth-service/
COPY product-service/pom.xml product-service/
COPY api-gateway/pom.xml     api-gateway/
COPY cart-service/pom.xml    cart-service/
COPY order-service/pom.xml   order-service/
COPY payment-service/pom.xml  payment-service/
COPY notification-service/pom.xml notification-service/

RUN mvn install -N -q
RUN mvn dependency:go-offline -pl cart-service -q

COPY cart-service/src cart-service/src
RUN mvn package -pl cart-service -DskipTests -q

# Stage 2 — Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/cart-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]