# --- Stage 1: build ---
# Using a full JDK image here only for the build
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

# Copy just the Maven wrapper and pom.xml first, then download dependencies,
# before copying the source. Docker caches each layer by its inputs - as
# long as pom.xml doesn't change, this dependency-download layer is reused
# on every rebuild instead of re-downloading the internet every time you
# change a single Java file.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# Now copy the actual source and build. Only this layer re-runs when
# application code changes.
COPY src src
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: runtime ---
# JRE only, not JDK
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Run as a non-root user. Without this, the process runs as root inside
# the container by default - unnecessary privilege for a service that
# only needs to listen on a port and make outbound HTTP/Kafka calls
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Lets Docker/Kubernetes know the container is actually ready to serve
# traffic, not just that the process is running - the JVM can be up
# before the Spring context has finished initializing.
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]