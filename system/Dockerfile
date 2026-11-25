
FROM maven:3.9.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:22-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -jar app.jar"]
