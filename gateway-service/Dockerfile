# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :gateway-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]