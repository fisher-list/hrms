# ============================================================
# Backend multi-stage build — Spring Boot 3.x + JDK 17
# ============================================================

# ---- Stage 1: Build with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy parent POM first for better layer caching
COPY backend/pom.xml ./pom.xml
COPY backend/hrms-common/pom.xml ./hrms-common/pom.xml
COPY backend/hrms-app/pom.xml ./hrms-app/pom.xml

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY backend/ ./

# Build — skip tests (they need DB + env vars)
RUN mvn package -DskipTests -B --no-transfer-progress \
    && ls -la hrms-app/target/

# ---- Stage 2: Slim JRE runtime ----
FROM eclipse-temurin:17-jre-alpine AS runtime
LABEL maintainer="hrms-team"

RUN addgroup -S hrms && adduser -S hrms -G hrms
WORKDIR /app

COPY --from=builder /build/hrms-app/target/hrms-app.jar app.jar

RUN chown hrms:hrms /app/app.jar
USER hrms

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
