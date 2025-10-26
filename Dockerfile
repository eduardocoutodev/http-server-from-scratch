FROM gradle:8.11-jdk23 AS build

WORKDIR /app

COPY settings.gradle.kts .
COPY gradle.properties .
COPY build.gradle.kts* .

COPY app/build.gradle.kts app/
COPY app/src app/src

RUN gradle :app:build --no-daemon -x test

FROM eclipse-temurin:23-jre-alpine
LABEL author = "eduardocoutodev"

WORKDIR /app

COPY --from=build /app/app/build/libs/*.jar app.jar

EXPOSE 444

ENTRYPOINT ["java", "-jar", "app.jar"]