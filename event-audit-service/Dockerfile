FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY bank-account-service/pom.xml bank-account-service/pom.xml
COPY event-audit-service/pom.xml event-audit-service/pom.xml
COPY event-audit-service/src event-audit-service/src

RUN mvn -pl event-audit-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/event-audit-service/target/event-audit-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
