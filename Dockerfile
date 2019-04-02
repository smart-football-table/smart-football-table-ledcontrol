# our base build image
FROM maven:3.6-jdk-8 as maven

# copy the project files
COPY ./pom.xml ./pom.xml
COPY ./src ./src

RUN mvn package -Djar.finalName=myapp
RUN mvn package dependency:copy-dependencies -DoutputDirectory=/app/lib

FROM gcr.io/distroless/java:latest

COPY --from=maven /app/ /app/
COPY --from=maven /target/myapp.jar /app/

ENTRYPOINT ["java", "-cp", "/app/myapp.jar:/app/lib/*", "ledcontrol.runner.SystemRunner"]
CMD []

