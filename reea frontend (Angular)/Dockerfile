FROM docker-trusted.ecosis.csi.it/node:22.17.1-slim AS build

# here copy package.json and lockfile
WORKDIR /app
COPY --chown=node:node package.json package-lock.json /app/
RUN npm ci
# here copy and build app
COPY --chown=node:node . /app
RUN printenv
RUN ls -lat
RUN npm run build -- --base-href=/registro-amianto/
RUN echo "CHECK BUILD"

RUN cat /app/dist/reeafe/browser/index.html | grep base

# here second stage of image
FROM docker-trusted.ecosis.csi.it/alpine:3.17
LABEL it.csi.codice_progetto="reea" \
      it.csi.codice_componente="reeafe"

COPY --from=build /app/dist/reeafe/browser /data/registro-amianto

VOLUME /data
