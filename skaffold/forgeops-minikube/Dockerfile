FROM gcr.io/forgerock-io/icf/docker-build:1.5.20.5

COPY logback.xml /opt/openicf/lib/framework
COPY ConnectorServer.properties /opt/openicf/conf
COPY scripts /opt/openicf/scripts
COPY rootCA.pem /opt/openicf/security/rootCA.pem

RUN echo $(keytool -keystore /opt/openicf/security/keyStore.pkcs12 -import -trustcacerts -storetype pkcs12 -file /opt/openicf/security/rootCA.pem -alias forgeops-ca -storepass changeit -noprompt)

# root needed for file sync in dev mode
USER root
