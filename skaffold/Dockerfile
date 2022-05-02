FROM gcr.io/forgerock-io/icf/docker-build:1.5.20.5

COPY logback.xml /opt/openicf/lib/framework
COPY ConnectorServer.properties /opt/openicf/conf
COPY scripts /opt/openicf/scripts
COPY drivers/ /opt/openicf/lib/framework/

# root needed for file sync in dev mode
USER root
