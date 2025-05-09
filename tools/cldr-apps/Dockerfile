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

FROM icr.io/appcafe/open-liberty:kernel-slim-java17-openj9-ubi-minimal AS server
# copy just the server file so we get config
COPY --from=setup /opt/ol/wlp/usr/servers/cldr/server.xml /opt/ol/wlp/usr/servers/defaultServer/server.xml
USER root
RUN chown default /config/server.xml
RUN mkdir -p /srv/st/config  /srv/st/src && chown -R default /srv/st
USER default
# fix local configuration
COPY docker/bootstrap-docker.properties /opt/ol/wlp/usr/servers/defaultServer/bootstrap.properties
COPY docker/server-docker.env /opt/ol/wlp/usr/servers/defaultServer/server.env
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
