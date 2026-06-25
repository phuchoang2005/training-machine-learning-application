# syntax=docker/dockerfile:1

# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# The backend launches *sibling* training containers and inspects them through the
# host Docker daemon (DockerTrainingRunner / JobReconcilerService / HealthController
# shell out to `docker run|inspect|info`), and clones GitHub sources with `git`.
# So the image needs the docker CLI + git; curl is used by the compose healthcheck.
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
RUN apt-get update \
    && apt-get install -y --no-install-recommends git curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/ai-training-backend-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
