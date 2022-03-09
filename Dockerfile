FROM gcr.io/forgerock-io/icf/docker-build:1.5.20.6-de83e489f424762522352876c9e1fa9d51cb34b8

COPY logback.xml /opt/openicf/lib/framework
COPY ConnectorServer.properties /opt/openicf/conf
COPY scripts /opt/openicf/scripts

# root needed for file sync in dev mode
USER root
