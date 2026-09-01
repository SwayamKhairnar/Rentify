# Multi-stage Dockerfile for Spring Boot Monolith
# Stage 1: Build JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B -T 1C

# Stage 2: Lightweight JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /build/target/rentify-backend-1.0.0.jar app.jar

EXPOSE 4000
ENV SERVER_PORT=4000
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
