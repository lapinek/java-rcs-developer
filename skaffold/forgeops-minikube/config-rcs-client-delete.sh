#!/bin/sh

# execute from this folder
source "$(dirname $0)/.env"

curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request DELETE \
"$TENANT_ORIGIN/openidm/config/provisioner.openicf.connectorinfoprovider" -v