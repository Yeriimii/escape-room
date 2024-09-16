FROM openjdk:17

WORKDIR /app

CMD ["./gradlew", "build", "-x", "test"]

COPY build/libs/*.jar escape-app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=dev", "escape-app.jar"]
