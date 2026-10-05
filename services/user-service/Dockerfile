FROM eclipse-temurin:26-jre
WORKDIR /app
COPY services/user-service/target/service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
