# ---------- Builder Stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Import corporate CA certificates so Maven can reach repositories through
# TLS-inspecting proxies. Fails the build if a cert cannot be imported so the
# truststore is never silently left incomplete.
COPY certs/ /usr/local/share/ca-certificates/statement-certs/
RUN set -eu; \
    for cert in /usr/local/share/ca-certificates/statement-certs/*.pem; do \
        alias="$(basename "$cert" .pem)"; \
        echo "Importing CA: $alias"; \
        keytool -importcert -noprompt -trustcacerts \
            -alias "$alias" \
            -file "$cert" \
            -cacerts -storepass changeit; \
    done

# Copy only POM first (layer caching)
COPY pom.xml .

# Pre-download dependencies
RUN mvn -q dependency:go-offline

# Copy source
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar built in previous stage
COPY --from=build /app/target/app.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
