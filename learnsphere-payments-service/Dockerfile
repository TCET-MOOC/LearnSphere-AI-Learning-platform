# syntax=docker/dockerfile:1

# Build the payments service with the project's pinned Maven wrapper.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app \
    && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
