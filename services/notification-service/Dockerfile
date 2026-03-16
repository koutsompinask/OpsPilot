FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
ARG JAR_FILE=services/notification-service/build/libs/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
