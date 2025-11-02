# Multi-stage Dockerfile for building and testing
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
RUN mvn clean package -DskipTests

FROM maven:3.9-eclipse-temurin-21 AS test
WORKDIR /app
COPY --from=builder /app/pom.xml .
COPY --from=builder /root/.m2/repository /root/.m2/repository
COPY src src
# Run all tests including integration tests with Testcontainers
# For Linux/CI: Build with BuildKit and Docker socket mount:
#   DOCKER_BUILDKIT=1 docker build --target test --build-arg DOCKER_HOST=unix:///var/run/docker.sock -t treevault-backend-test .
# For Windows: Run tests directly with Maven (Docker socket accessible via named pipe):
#   mvn clean test
# Or use WSL2 where socket is at /var/run/docker.sock
ARG DOCKER_HOST
ENV DOCKER_HOST=${DOCKER_HOST}
RUN mvn clean test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
