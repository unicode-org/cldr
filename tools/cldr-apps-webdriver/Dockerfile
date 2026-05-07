FROM maven:3-eclipse-temurin-17-alpine

WORKDIR /workarea
# COPY . /workarea
COPY surveydriver-docker.properties /workarea/surveydriver.properties
COPY target/cldr-apps-webdriver.jar /workarea/cldr-apps-webdriver.jar
CMD ["java", "-jar", "cldr-apps-webdriver.jar"]
