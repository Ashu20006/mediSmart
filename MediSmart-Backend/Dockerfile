# syntax=docker/dockerfile:1

## Build stage: resolves dependencies and packages the Spring Boot jar
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and project metadata first to leverage Docker layer caching
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Pre-fetch dependencies to speed up subsequent builds
RUN ./mvnw -q -B dependency:go-offline

# Copy source and build the application
COPY src src
RUN ./mvnw -q -B -DskipTests package

## Runtime stage: lightweight JRE image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Render injects PORT; default to 8080 for local runs
ENV PORT=8080
EXPOSE 8080

# Copy the fat jar from the builder stage
COPY --from=builder /app/target/*.jar /app/app.jar

# Use sh to allow env var expansion for PORT
ENTRYPOINT ["sh", "-c", "exec java -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=75 -Dserver.port=$PORT -jar /app/app.jar"]
