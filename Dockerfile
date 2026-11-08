FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/data \
    && chown -R app:app /app

COPY --from=build /src/target/interview-platform.jar /app/interview-platform.jar
COPY --chown=app:app data/questions.json /app/data/questions.json

USER app
EXPOSE 8091

ENV SERVER_PORT=8091 \
    INTERVIEW_DATA_FILE=/app/data/questions.json

ENTRYPOINT ["java", "-jar", "/app/interview-platform.jar", "--server.address=0.0.0.0"]
