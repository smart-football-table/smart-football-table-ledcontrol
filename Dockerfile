# our base build image
FROM maven:3.6-jdk-8 as maven

WORKDIR /project
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package

FROM gcr.io/distroless/java:latest
COPY --from=maven /project/target/packaged/assembly-dir/lib /app/
COPY --from=maven /project/target/packaged/assembly-dir/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar", "ledcontrol.runner.SystemRunner"]
CMD []

