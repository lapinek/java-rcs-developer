#!/bin/sh

# execute from this folder
source "$(dirname $0)/.env"

curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request GET \
"$TENANT_ORIGIN/openidm/config" -v