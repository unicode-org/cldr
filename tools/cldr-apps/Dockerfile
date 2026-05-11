FROM alpine:latest AS setup
COPY target/cldr-apps.zip /tmp
RUN mkdir -p /opt/ol
WORKDIR /opt/ol
# we don't want the bootstrap.properties or server.env from the workspace
RUN unzip /tmp/cldr-apps.zip -x "*/bootstrap.properties" "*/server.env"
WORKDIR /tmp
RUN unzip -j cldr-apps.zip wlp/usr/servers/cldr/apps/cldr-apps.war
RUN unzip -j cldr-apps.war 'WEB-INF/lib/cldr-code-*'
RUN mv -v cldr-code-*-SNAPSHOT.jar cldr-code.jar

FROM icr.io/appcafe/open-liberty:26.0.0.4-kernel-slim-java17-openj9-ubi AS server
# copy just the server file so we get config
COPY --from=setup /opt/ol/wlp/usr/servers/cldr/server.xml /opt/ol/wlp/usr/servers/defaultServer/server.xml
USER root
RUN mkdir /home/default
RUN chown default /config/server.xml /home/default
RUN mkdir -p /srv/st/config  /srv/st/src && chown -R default /srv/st
# fix local configuration
COPY docker/bootstrap-docker.properties /opt/ol/wlp/usr/servers/defaultServer/bootstrap.properties
COPY docker/server-docker.env /opt/ol/wlp/usr/servers/defaultServer/server.env
## CLDR-19464 workaround maven issue:
COPY docker/featureUtility.properties /opt/ol/wlp/etc/featureUtility.properties
### - check settings
RUN featureUtility viewSettings
# RUN mkdir -p /home/default/.m2/repository/io/
# COPY docker/io/ /home/default/.m2/repository/io/
### - verify (this is what features.sh ought to do)
# RUN featureUtility viewSettings && featureUtility installServerFeatures --acceptLicense defaultServer --verbose
# setup features needed
RUN features.sh
# setup configuration
RUN configure.sh
# add this so it mirrors prod
COPY docker/setup-config.sh /srv/st/
ARG CLDR_PHASE=SUBMIT
ENV CLDR_PHASE=${CLDR_PHASE}
WORKDIR /srv/st/
# Copy everything else
COPY --from=setup /opt/ol/wlp/usr/servers/cldr/ /opt/ol/wlp/usr/servers/defaultServer/
COPY --from=setup /tmp/cldr-code.jar /tmp/cldr-code.jar
RUN /srv/st/setup-config.sh
