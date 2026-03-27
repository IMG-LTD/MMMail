FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY backend/pom.xml backend/pom.xml
COPY backend/mmmail-common/pom.xml backend/mmmail-common/pom.xml
COPY backend/mmmail-server/pom.xml backend/mmmail-server/pom.xml

RUN mvn -f backend/pom.xml -pl mmmail-server -am dependency:go-offline

COPY backend backend

RUN mvn -f backend/pom.xml -pl mmmail-server -am -DskipTests package

FROM eclipse-temurin:21-jre

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/backend/mmmail-server/target/mmmail-server-*.jar /app/mmmail-server.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/mmmail-server.jar"]
