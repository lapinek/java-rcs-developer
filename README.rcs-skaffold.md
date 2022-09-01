# Deploying Java Remote Connector Server and Developing Scripted Connectors in Kubernetes with Docker and Skaffold

An RCS  can be deployed as a Docker container within a Kubernetes cluster. Within that setting, you can scale the RCS cluster, monitor logs, and develop new connectors.

## <a id="contents" name="contents"></a>Contents

* [Overview](#project-overview)
* [RCS OAuth 2.0 Client Application](#project-rcs-oauth2-client)
* [Setting Up Environment](#project-environment)
* [Preparing Kubernetes Cluster](#project-preparing-cluster)
* [Running RCS Using Skaffold](#project-running)
    * [Prerequisites](#project-running-prerequisites)
    * [Configuring RCS](#project-running-configuring-rcs)
        * [Connector Server Properties](#project-running-configuring-rcs-properties)
        * [Logs Level](#project-running-configuring-rcs-logs)
    * [Running RCS](#project-running-running-rcs)
    * [Registering Connector](#project-running-registering-connector)
    * [Using Skaffold in Development Mode](#developing-skaffold-development-mode)
* [DISCLAIMER](#project-disclaimer)

## <a id="project-overview" name="project-overview"></a>Overview

[Back to Contents](#contents)

<!-- An RCS might preserve and rely on its state; for example, it could implement [StatefulConfiguration Interface](https://backstage.forgerock.com/docs/idcloud-idm/latest/_attachments/apidocs/org/identityconnectors/framework/spi/StatefulConfiguration.html) and persist some objects, like HTTP pool or tokens. -->

The example project is a Java RCS Kubernetes deployment managed with [Skaffold](https://skaffold.dev/). The Skaffold configuration can be found in [rcs/identity-cloud/skaffold.yaml](rcs/identity-cloud/skaffold.yaml).

The example RCS manifest used in this project can be found in [rcs/identity-cloud/rcs.yaml](rcs/identity-cloud/rcs.yaml).

The RCS image referenced in the configuration will be built according to the content of a Dockerfile, an example of which is provided at [rcs/identity-cloud/Dockerfile](rcs/identity-cloud/Dockerfile).

In the Dockerfile, you can observe that, all connectors' scripts (roots) will be copied at once from [rcs/identity-cloud/scripts](rcs/identity-cloud/scripts) to the corresponding location under `/opt/openicf/scripts` in the RCS Docker container.

Similarly, all the drivers any of the connectors might require (such as database drivers) will be copied from under [rcs/identity-cloud/drivers/](rcs/identity-cloud/drivers) to the RCS' ` /opt/openicf/lib/framework/` location.

Example connector configurations, which will reference the script roots, will be placed in individual folders under [connectors](connectors).

## <a id="project-rcs-oauth2-client" name="project-rcs-oauth2-client"></a>RCS OAuth 2.0 Client Application

[Back to Contents](#contents)

In Identity Cloud, you are provided with `RCSClient` OAuth 2.0 client application, for which you'll need to [reset the client secret](https://backstage.forgerock.com/docs/idcloud/latest/identities/sync-identities.html#1_reset_the_client_secret) and save it for your RCS client configuration.

> You could also create your own OAuth 2.0 client, but you will need to add it as a subject to IDM static user mapping for the RCS to identify as.
>
> See [rcs/forgeops-minikube/README.md](rcs/forgeops-minikube/README.md) for an example of how this can be done over REST, at the `/openidm/config/authentication` endpoint, in an environment where there is no designated RCS client.
>
> Note that, an RCS OAuth 2.0 Client application needs to have the `fr:idm.*` value in Scopes.

With this setup, RCS can get sufficient authorization with the `Client Credentials` Grant Type in order to establish connection with the Identity Cloud tenant.

In order to manage your RCS and its connectors registration in Identity Cloud over REST, you will need an access token issued to a ForgeRock Identity Management (IDM) administrator. You could use a separate client for obtaining such authorization or, for development purposes, you could add the `Resource Owner Password Credentials` Grant Type to the existing `RCSClient` application. The latter approach is assumed in this example project.

## <a id="project-environment" name="project-environment"></a>Setting Up Environment

[Back to Contents](#contents)

It is easier describe an RCS deployment in a reproducible way via REST interactions where the RCS specifics could be provided dynamically, as variables.

> In addition, the Platform UI [will not currently accept hyphens for Connector Server names](https://bugster.forgerock.org/jira/browse/IAM-2130). At the same time, an RCS instance might be associated with data; hence, this example project will present an implementation based on a [StatefulSet](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/), which [requires hyphens in its pods' hostnames](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#stable-network-id). A hostname, in turn, can serve as a dynamic reference to a connector server in a Kubernetes deployment, and this will be employed in this deployment example.

An RCS instance-specific information might include:
* Identity Cloud tenant and realm-specific URIs.
* Credentials, such as OAuth 2.0 client name and secret and a resource owner login and password.
* Docker repository location.

In this example, we will use environment variables for project-specific settings so that:
* Sensitive information can be kept in ignored file(s) and thus not accidentally committed into a repository.
    > Note that, any secret your supply to the RCS container will be accessible by the RCS image users. Currently, there is no known (to me) way of securely providing a secret to an RCS instance.
    >
    > At least, your code base could be free of sensitive information.
* You could easily switch between environments if they require different credentials and/or other settings that cannot be captured otherwise—for example, in Skaffold profiles.

<!-- TODO:
    * See if there is a better way of handling secrets in RCS.
    * See if there is a better way of handling secrets in a Kubernetes deployment.
        * https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables
        * Employ a customizing/templating technology?
-->

[./.env.example](/rcs/identity-cloud/.env.example)
```
SKAFFOLD_DEFAULT_REPO=gcr.io/engineering-devops/<your-docker-repository-name>
TENANT_HOSTNAME=<your-tenant-name>.forgeblocks.com
TENANT_ORIGIN=https://$TENANT_HOSTNAME
ACCESS_TOKEN_URL="$TENANT_ORIGIN/am/oauth2/realms/root/realms/alpha/access_token"
CLIENT_ID=RCSClient
CLIENT_SECRET=<your-RCSClient-secret>
USER_USERNAME=idm-admin
USER_PASSWORD="X&...\`...?ydY"

AUTHORIZATION_HEADER_VALUE="Bearer "$( \
    echo $( \
        curl -s --location --request POST "$ACCESS_TOKEN_URL" \
        --header "Authorization: Basic $(printf  "$CLIENT_ID:$CLIENT_SECRET" | base64)" \
        --header "Content-Type: application/x-www-form-urlencoded" \
        --data-urlencode "grant_type=password" \
        --data-urlencode "scope=fr:idm:*" \
        --data-urlencode "username=${USER_USERNAME}" \
        --data-urlencode "password=${USER_PASSWORD}" \
    ) | sed -e 's/^.*"access_token":"\([^"]*\)".*$/\1/' \
)

echo $AUTHORIZATION_HEADER_VALUE
```

You can evaluate a `.env` file in the following manner:

```sh
$ set -a; source path/to/your/.env; set +a;
```

This will export the key-pair combinations from the file into environment variables that will become available for the shell and its child processes. Note that, in the example `.env` file, we also get authorization header, which can be used in consequent requests to the IDM REST API. Echoing the authorization header will confirm whether the settings you provided in the `.env` file are valid.

You can also source the `.env` file inside a script. For example:

`send-an-authorized-request-to-tenant.sh`
```sh
#!/bin/sh

# execute from this folder and provide relative path to the .env file
source "$(dirname $0)/.env"

curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
. . .
"$TENANT_ORIGIN/openidm/endpoint" -v
```

> Don't forget to apply execute permissions to the script file:
>
> ```sh
> $ chmod +x ./send-an-authorized-request-to-tenant.sh
> ```
>
> Then:
>
> ```sh
> $ ./send-an-authorized-request-to-tenant.sh
> ```

## <a id="project-preparing-cluster" name="project-preparing-cluster"></a>Preparing Kubernetes Cluster

[Back to Contents](#contents)

Navigate to [rcs/identity-cloud](rcs/identity-cloud).

[Register your Remote Connector Server (RCS) in client mode](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/remote-connector.html#configure-rcs) in your tenant using the provided [provisioner configuration example](/rcs/identity-cloud/provisioner.openicf.connectorinfoprovider.json). The static configuration in this file assumes two replicas of a StatefulSet named `rcs`.

You can use the settings and credentials saved in the environment variables when making a REST request with [cURL](https://curl.se/docs/manpage.html). For example:

```sh
$ set -a; source ./.env; set +a;
```

```sh
$ curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request PUT \
--data "$(cat ./provisioner.openicf.connectorinfoprovider.json)" \
"$TENANT_ORIGIN/openidm/config/provisioner.openicf.connectorinfoprovider" -v
```

Or, you could use a script:

[./shell-scripts/register-rcs.sh](rcs/identity-cloud/shell-scripts/register-rcs.sh)
```sh
#!/bin/sh

# execute from this folder
source "$(dirname $0)/.env"

curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request PUT \
--data "$(cat "$(dirname $0)/provisioner.openicf.connectorinfoprovider.json")" \
"$TENANT_ORIGIN/openidm/config/provisioner.openicf.connectorinfoprovider" -v
```

> The environment variables here and in the following examples are used as placeholders. When you are authorized as an IDM admin in a browser, you can copy the entire authorization header or its value from the browser console.
>
> As an alternative to using cURL, you can make a network request from an authorized browser console; in doing so, you will need to supply the raw RCS registration content instead of referencing a file.
>
> <details>
> <summary><strong>Browser JavaScript Example</strong></summary>
>
> ```javascript
> /**
>  * @todo Sign in your tenant at: https://$TENANT_NAME.forgeblocks.com/platform
>  */
>
> var settings = {
>     "url": "/openidm/config/provisioner.openicf.connectorinfoprovider",
>     "method": "PUT",
>     "timeout": 0,
>     "headers": {
>         "Accept-API-Version": "resource=1.0",
>         "Content-Type": "application/json",
>         "If-None-Match": "*"
>     },
>     "data": JSON.stringify({
>         "_id": "provisioner.openicf.connectorinfoprovider",
>         "connectorsLocation": "connectors",
>         "remoteConnectorClients": [
>             {
>                 "name": "rcs-0",
>                 "enabled": true,
>                 "useSSL": true
>             },
>             {
>                 "name": "rcs-1",
>                 "enabled": true,
>                 "useSSL": true
>             }
>         ],
>         "remoteConnectorServers": [],
>         "remoteConnectorClientsGroups": [
>             {
>                 "algorithm": "roundrobin",
>                 "name": "rcs",
>                 "serversList": [
>                     {
>                         "name": "rcs-0"
>                     },
>                     {
>                         "name": "rcs-1"
>                     }
>                 ]
>             }
>         ],
>         "remoteConnectorServersGroups": []
>     })
> };
>
> $.ajax(settings).done(function (response) {
>   console.log(response);
> });
> ```
>
> </details>
>

<br/>

This will register two RCS "client" instances (`rcs-0` and `rcs-1`) as well as a client group (`rcs`) that refers to them.

> You can further manage your RCS registration, like any other `/openidm/config/object`, as described in the [Identity Cloud IDM > Server Configuration](https://backstage.forgerock.com/docs/idcloud-idm/latest/rest-api-reference/endpoints/rest-server-config.html) docs.
>
> For example, you can delete it:
>
> ```sh
> $ curl \
> --header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
> --header "Accept-API-Version: resource=1.0" \
> --header "Content-Type: application/json" \
> --request DELETE \
> "$TENANT_ORIGIN/openidm/config/provisioner.openicf.connectorinfoprovider" -v
> ```


## <a id="project-running" name="project-running"></a>Running RCS Using Skaffold

[Back to Contents](#contents)

### <a id="project-running-prerequisites" name="project-running-prerequisites"></a>Running RCS Using Skaffold > Prerequisites

[Back to Contents](#contents)

 - [Skaffold](https://skaffold.dev/) is a tool produced by Google to automate various Kubernetes deployment related tasks. Be sure to install the appropriate version for your operating system.
 - [Docker](https://www.docker.com/) is used by skaffold to build your custom RCS image; be sure you have that installed as well.
 - [Kubernetes](https://kubernetes.io/) must be running somewhere in the proximity of the resources you would like to connect to; as such, be sure you have [kubectl](https://kubernetes.io/docs/reference/kubectl/) installed and configured to use your Kubernetes service.

 Finally, be sure to [configure Docker so that you have appropriate privileges](https://docs.docker.com/engine/reference/commandline/login/) to `docker push` into an image registry, which your Kubernetes cluster can pull from.

### <a id="project-running-configuring-rcs" name="project-running-configuring-rcs"></a>Running RCS Using Skaffold > Configuring RCS

[Back to Contents](#contents)

#### <a id="project-running-configuring-rcs-properties" name="project-running-configuring-rcs-properties"></a>Running RCS Using Skaffold > Configuring RCS > Connector Server Properties

[Back to Contents](#contents)

If you followed steps in [Setting Up Environment](#project-environment), all the properties will be set during the project startup. Read the rest of this section to see how it is done in this example, and how you could change it.

<!-- Using the provided [ConnectorServer.properties.example](/rcs/identity-cloud/ConnectorServer.properties.example) as a template, create your own `ConnectorServer.properties` file with the appropriate references to your tenant; the only details you should have to change to match your environment are:
* `connectorserver.url`
* `connectorserver.tokenEndpoint`
* `connectorserver.clientSecret`. -->

The RCS client-mode configuration properties are described in Identity Cloud Docs at [Remote connector configuration](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/remote-connector.html) > RCS Properties.

You need to specify the following:

* `connectorserver.connectorServerName`
* `connectorserver.url`
* `connectorserver.tokenEndpoint`
* `connectorserver.clientSecret`
* `connectorserver.scope`
* `connectorserver.loggerClass`

These properties could be saved in a `ConnectorServer.properties` file and copied to your RCS installation into the `conf` folder.

Some properties that share the same static value between all server replicas _and_ the value could be kept in the RCS configuration code can stay in the `ConnectorServer.properties` file. For example:

[./ConnectorServer.properties](rcs/identity-cloud/ConnectorServer.properties)

```
connectorserver.scope=fr:idm:*
connectorserver.loggerClass=org.forgerock.openicf.common.logging.slf4j.SLF4JLog
```

However, as of this writing, property substitution in a `ConnectorServer.properties` file is not supported; and thus, none of the properties could be populated dynamically.

One way of making RCS configuration dynamic is to supply them in the RCS Java Virtual Machine (JVM) options populated with resources available in the container. In your local development setup, you can specify JVM options in an RCS Kubernetes manifest, in the command to be run in the RCS containers; there, you can use the container environment variables as the dynamic values.

PROS:
* For one, this will allow to provide unique values for the required `connectorserver.connectorServerName` property in your RCS replicas. For example, you can reference available in that environment pod's hostname, which is unique in a StatefulSet. Following the simple convention for [Stable Network ID](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/#stable-network-id) for a StatefulSet pod, you will then know what to register as the Connector Server names in Identity Cloud; for example, `rcs-0`, `rcs-1`, . . . , `rcs-n`.

* Secondly, you can exclude the sensitive values from your `ConnectorServer.properties`, which could be a part of the shared deployment code.

For example:

[./rcs.yaml](rcs/identity-cloud/rcs.yaml)
```yaml
. . .
      containers:
      - image:  rcs
        name: rcs
        command: ['bash', '-c']
        args:
        - export OPENICF_OPTS="-Dconnectorserver.connectorServerName=$HOSTNAME
          -Dconnectorserver.url=wss://$TENANT_HOSTNAME/openicf/0
          -Dconnectorserver.tokenEndpoint=$ACCESS_TOKEN_URL
          -Dconnectorserver.clientId=$CLIENT_ID
          -Dconnectorserver.clientSecret=$CLIENT_SECRET"
          && /opt/openicf/bin/docker-entrypoint.sh;
. . .
```

> Using the `D` prefix will _[D]efine_ corresponding system properties in the RCS JVM.
>
> The `OPENICF_OPTS` content will be appended by the ICF framework to the `JAVA_OPTS` environment variable for the RCS JVM.

The environment variables used to populate the connector server properties _could_ come from [Docker build arguments](https://docs.docker.com/engine/reference/builder/#arg).

> You could also employ Kubernetes' [ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/) and [Secrets](https://kubernetes.io/docs/concepts/configuration/secret/), and [use them as environment variables](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables) in the pods (along with one of the Kubernetes customization techniques for switching between environments). This, however, might require a bit more maintenance compared to a single `.env` file, and still won't hide any secrets from the RCS image consumers.
>
> Skaffold currently does not support [setting container environment variables via Docker run arguments](https://docs.docker.com/engine/reference/commandline/run/#set-environment-variables--e---env---env-file).

For example:

[./Dockerfile](rcs/identity-cloud/Dockerfile)
```Dockerfile
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
```

> Note, that anyone with access to the Docker image will be able to see the build-time variables (with the [docker history](https://docs.docker.com/engine/reference/commandline/history/) command), check the environment variables set in the container, and read the content of the copied into the container `Connector.properties` file.

The [Docker build arguments can be provided in your Skaffold configuration](https://skaffold.dev/docs/references/yaml/#build-artifacts-docker-buildArgs):

[skaffold.yaml](rcs/identity-cloud/skaffold.yaml)
```yaml
. . .
    docker:
      dockerfile: Dockerfile
      buildArgs:
        TENANT_HOSTNAME: "{{ .TENANT_HOSTNAME }}"
        ACCESS_TOKEN_URL: "{{ .ACCESS_TOKEN_URL }}"
        CLIENT_ID: "{{ .CLIENT_ID }}"
        CLIENT_SECRET: "{{ .CLIENT_SECRET }}"
. . .
```

Using the [supported by Skaffold](https://skaffold.dev/docs/environment/templating/) Go language template syntax, you can refer to environment variables in your local development shell; and thus, provide all dynamic/sensitive content from a single `.env` file or any other convenient way of managing local development environment.

#### <a id="project-running-configuring-rcs-logs" name="project-running-configuring-rcs-logs"></a>Running RCS Using Skaffold > Configuring RCS > Logs Level

[Back to Contents](#contents)

If you would like to adjust the detail level of the logs produced by the RCS, you can do so by editing [./logback.xml](/rcs/identity-cloud/logback.xml) as described in [Identity Cloud IDM > Connector Development > Connector Troubleshooting](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/troubleshooting.html).

### <a id="project-running-running-rcs" name="project-running-running-rcs"></a>Running RCS Using Skaffold > Running RCS

[Back to Contents](#contents)

The key command to deploy RCS into your cluster is this:

```sh
# @example
# skaffold run --default-repo gcr.io/engineering-devops/olaf

$ skaffold run --default-repo gcr.io/your-docker-registry
```

Instead of providing your registry location in the `--default-repo` argument, you can [set a default image repository in Skaffold's global config](https://skaffold.dev/docs/environment/image-registries/), or specify one in `$SKAFFOLD_DEFAULT_REPO` environment variable (as shown in the [.env.example](./rcs/identity-cloud/.env.example) file).

> If you want to stream logs from your `skaffold run` command, add the `--tail` flag:
> ```sh
> $ skaffold run --default-repo gcr.io/your-docker-registry --tail
> ```
> You can stop tailing logs with `Ctrl-C`

This will pull the core RCS docker image, overlay your customizations into a new docker image, push that new image into your registry, and then deploy the RCS service into your Kubernetes cluster with that image reference included.

You can see the running pods with this command:

```sh
$ kubectl get po --selector "app=rcs"

NAME    READY   STATUS    RESTARTS   AGE
rcs-0   1/1     Running   0          2m57s
rcs-1   1/1     Running   0          2m55s
```

Sign in your platform admin UI and look under Identities > Connect—both registered servers should be listed and connected.

### <a id="project-running-registering-connector" name="project-running-registering-connector"></a>Running RCS Using Skaffold > Registering Connector

[Back to Contents](#contents)

Once you have confirmed that your RCS instances are properly connected to your platform, you can register a connector.

Navigate to a connector configuration folder; for example:

```sh
$ cd ../../connectors/pseudo
```

You can register the provided [sample pseudo connector](./connectors/pseudo/provisioner.openicf-pseudo.json) using the [REST API](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST) like so:

```sh
$ set -a; source path/to/your/.env; set +a;
```

```sh
$ curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request PUT \
--data "$(cat ./provisioner.openicf-pseudo.json)" \
"$TENANT_ORIGIN/openidm/config/provisioner.openicf/pseudo" -v
```

Or, with a script; for example:

[./shell-scripts/register.identity-cloud.sh](connectors/pseudo/shell-scripts/register.identity-cloud.sh)
```sh
#!/bin/sh

# execute from this folder
source "$(dirname $0)/../../../rcs/identity-cloud/.env"

curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
--header "Accept-API-Version: resource=1.0" \
--header "Content-Type: application/json" \
--request PUT \
--data "$(cat "$(dirname $0)/../provisioner.openicf-postgres.json")" \
"$TENANT_ORIGIN/openidm/config/provisioner.openicf/postgres" -i
```

You can now use this connector in your platform, like so:

```sh
$ curl \
--header "Authorization: $AUTHORIZATION_HEADER_VALUE" \
"$TENANT_ORIGIN/openidm/system/pseudo/__TEST__?_queryFilter=true"

{"result":[{"_id":"UID00", . . . }, . . . ],"resultCount":10,"pagedResultsCookie":null,"totalPagedResultsPolicy":"NONE","totalPagedResults":-1,"remainingPagedResults":-1}
```

And this is how you remove the RCS pods after having started them as described above:

```sh
$ skaffold delete
```

### <a id="developing-skaffold-development-mode" name="developing-skaffold-development-mode"></a>Running RCS Using Skaffold > Using Skaffold in Development Mode

[Back to Contents](#contents)

Instead of `skaffold run`, you can use the other mode of operation that skaffold provides: [skaffold dev](https://skaffold.dev/docs/references/cli/#skaffold-dev). Using this, skaffold can monitor your filesystem for changes within the [scripts](/rcs/identity-cloud//scripts/) folder and automatically copy them into your running RCS instances. It will also remain attached to the containers in order to show you the logs from each. This is very helpful as you develop [groovy-based scripted connectors](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/groovy.html).

You can try this out very easily; start by running skaffold dev:
```sh
# @example
# skaffold dev --default-repo gcr.io/engineering-devops/olaf

$ skaffold dev --default-repo gcr.io/your-docker-registry
```

As with the `run` command, you can [set a default image registry in Skaffold global config](https://skaffold.dev/docs/environment/image-registries/), or specify it in `SKAFFOLD_DEFAULT_REPO` environment variable.


You will see the various log messages produced in your skaffold terminal. You can edit the files within your [./scripts/pseudo/](/rcs/identity-cloud/scripts/pseudo/) folder and see the results of your changes immediately applied automatically.

You can exit the development mode and remove the pods with `Ctrl-C`.

If you lost your terminal session, your can still remove your application and pods with the `skaffold delete` command.

***

## <a id="project-disclaimer" name="project-disclaimer"></a>DISCLAIMER

[Back to Contents](#contents)

The sample code described herein is provided on an "as is" basis, without warranty of any kind,
to the fullest extent permitted by law. ForgeRock does not warrant or guarantee the individual success
developers may have in implementing the sample code on their development platforms or in production
configurations. ForgeRock does not warrant, guarantee or make any representations regarding the use, results
of use, accuracy, timeliness or completeness of any data or information relating to the sample code.
ForgeRock disclaims all warranties, expressed or implied, and in particular, disclaims all warranties of
merchantability, and warranties related to the code, or any service or software related thereto.
ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising
out of any action taken by you or others related to the sample code.
