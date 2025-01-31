FROM gradle:8.10.2-jdk21 AS building
WORKDIR /application
COPY . /application
RUN gradle dependencies --no-daemon
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /application
COPY --from=building /application/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]