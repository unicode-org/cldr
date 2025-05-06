FROM node:slim
WORKDIR /home/node/app
# fewer deps, so we don't pull in puppeteer/etc
COPY ./js/ /home/node/app/
# we do an npm i to make sure these two are here,
# we don't need the whole load of packages.
RUN npm i
ENV CLDR_VAP=admin_vap
ENV SURVEYTOOL_URL=http://cldr-apps:9080
