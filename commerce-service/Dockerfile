# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :commerce-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/commerce-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]