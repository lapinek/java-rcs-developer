FROM gcr.io/forgerock-io/rcs/docker-build:1.5.20.8-latest-postcommit

ARG TENANT_HOSTNAME
ARG ACCESS_TOKEN_URL
ARG CLIENT_ID
ARG CLIENT_SECRET

COPY logback.xml /opt/openicf/lib/framework
COPY ConnectorServer.properties /opt/openicf/conf
COPY scripts /opt/openicf/scripts
COPY drivers/ /opt/openicf/lib/framework/

ENV TENANT_HOSTNAME=$TENANT_HOSTNAME
ENV ACCESS_TOKEN_URL=$ACCESS_TOKEN_URL
ENV CLIENT_ID=$CLIENT_ID
ENV CLIENT_SECRET=$CLIENT_SECRET

# root needed for file sync in dev mode
USER root
