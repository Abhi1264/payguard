FROM eclipse-temurin:26-jre
WORKDIR /app
COPY services/payment-service/target/service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
