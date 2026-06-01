FROM maven:3-eclipse-temurin-17-alpine

# TODO: copy pom.xml and predownload
WORKDIR /workarea
COPY . /workarea
COPY surveydriver-docker.properties /workarea/surveydriver.properties
CMD ["mvn", "-B", "test"]
