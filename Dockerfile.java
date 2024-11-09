FROM eclipse-temurin:17.0.13_11-jre-jammy AS builder

WORKDIR /build
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:17.0.13_11-jre-jammy

WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar

# Create directory for logs
RUN mkdir logs && chmod 777 logs

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]