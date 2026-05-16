FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

WORKDIR /app

COPY backend/pom.xml backend/pom.xml
COPY backend/mmmail-billing/pom.xml backend/mmmail-billing/pom.xml
COPY backend/mmmail-common/pom.xml backend/mmmail-common/pom.xml
COPY backend/mmmail-drive/pom.xml backend/mmmail-drive/pom.xml
COPY backend/mmmail-foundation/pom.xml backend/mmmail-foundation/pom.xml
COPY backend/mmmail-identity/pom.xml backend/mmmail-identity/pom.xml
COPY backend/mmmail-labs/pom.xml backend/mmmail-labs/pom.xml
COPY backend/mmmail-mail/pom.xml backend/mmmail-mail/pom.xml
COPY backend/mmmail-org-governance/pom.xml backend/mmmail-org-governance/pom.xml
COPY backend/mmmail-pass/pom.xml backend/mmmail-pass/pom.xml
COPY backend/mmmail-platform/pom.xml backend/mmmail-platform/pom.xml
COPY backend/mmmail-server/pom.xml backend/mmmail-server/pom.xml
COPY backend/mmmail-workspace/pom.xml backend/mmmail-workspace/pom.xml

RUN mvn -f backend/pom.xml -pl mmmail-server -am dependency:go-offline

COPY backend backend

RUN mvn -f backend/pom.xml -pl mmmail-server -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd --system mmmail \
  && useradd --system --gid mmmail --home-dir /app --shell /usr/sbin/nologin mmmail \
  && mkdir -p /app /var/lib/mmmail/drive \
  && chown -R mmmail:mmmail /app /var/lib/mmmail

WORKDIR /app

COPY --from=build /app/backend/mmmail-server/target/mmmail-server-*.jar /app/mmmail-server.jar

USER mmmail

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/mmmail-server.jar"]
