FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="SecGuard Team"
LABEL description="SecGuard SIEM Server"

WORKDIR /app

COPY target/secguard-server-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8900

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}"]
