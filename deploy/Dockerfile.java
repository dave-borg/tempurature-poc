FROM eclipse-temurin:17.0.13_11-jre-jammy

WORKDIR /app
COPY target/tempurature-poc-0.0.2.jar app.jar

# Create directory for logs
RUN mkdir logs && chmod 777 logs

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]