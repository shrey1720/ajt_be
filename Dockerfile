# --- Stage 1: Build ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/engine-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
