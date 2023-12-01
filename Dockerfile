FROM openjdk:21-oracle
LABEL authors="Dustin Behnke"
VOLUME /tmp
COPY target/*.jar imageTagging.jar
ENTRYPOINT ["java", "-jar", "/imageTagging.jar"]