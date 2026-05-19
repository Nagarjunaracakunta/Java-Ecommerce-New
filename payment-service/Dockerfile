# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY auth-service/pom.xml     auth-service/
COPY product-service/pom.xml  product-service/
COPY api-gateway/pom.xml      api-gateway/
COPY cart-service/pom.xml     cart-service/
COPY order-service/pom.xml    order-service/
COPY payment-service/pom.xml  payment-service/

RUN mvn install -N -q
RUN mvn dependency:go-offline -pl payment-service -q

COPY payment-service/src payment-service/src
RUN mvn package -pl payment-service -DskipTests -q

# Stage 2 — Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/payment-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]