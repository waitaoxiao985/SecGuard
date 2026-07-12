FROM eclipse-temurin:25-jre-alpine

LABEL maintainer="SecGuard Team"
LABEL description="SecGuard SIEM Agent"

WORKDIR /app

COPY target/secguard-agent-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8901

ENV JAVA_OPTS="-Xms128m -Xmx256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
