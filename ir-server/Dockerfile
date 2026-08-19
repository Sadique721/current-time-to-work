# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/ocs-1.0.0.jar app.jar
EXPOSE 8060
ENTRYPOINT ["java", "-jar", "app.jar"]
