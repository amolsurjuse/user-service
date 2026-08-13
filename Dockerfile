FROM eclipse-temurin:25.0.3_9-jre-ubi10-minimal@sha256:35f47084a4c1e34636fc8842780d5ca1e85b1b74de139723d1a541137932ddf2
WORKDIR /app
COPY --chown=1000:1000 user-service-app/target/user-service-0.0.1-SNAPSHOT.jar app.jar
USER 1000:1000
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
