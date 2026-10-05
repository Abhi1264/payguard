FROM eclipse-temurin:26-jre
WORKDIR /app
COPY services/order-service/target/service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
