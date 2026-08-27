FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package \
    && find target -maxdepth 1 -type f -name "*.jar" ! -name "*.jar.original" \
       -exec cp {} /workspace/app.jar \; \
    && test -f /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
RUN mkdir -p /app/files && chown -R app:app /app

USER app

EXPOSE 8820

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
