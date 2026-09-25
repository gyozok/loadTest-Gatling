# --- Stage 1: build ---
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY order-producer/pom.xml order-producer/pom.xml
COPY order-consumer/pom.xml order-consumer/pom.xml
RUN ./mvnw -pl order-consumer -am dependency:go-offline -B

COPY order-consumer/src order-consumer/src
RUN ./mvnw -pl order-consumer -am clean package -DskipTests -B

# --- Stage 2: runtime ---
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/order-consumer/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
