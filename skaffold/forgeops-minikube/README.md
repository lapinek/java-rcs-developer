# Remote Connector Server (RCS) and ForgeOps in Minikube

The following covers some _additional_ details for using the [RCS Skaffold Example](../../README.md) with the [Cloud Developer's Kit (CDK)](https://backstage.forgerock.com/docs/forgeops/7.1/cdk/setup-cdk.html), both running in Minikube.

> The URLs used below assume the `default` namespace in Minikube. Note, deploying both the Platform and the RCS in the same cluster does not represent _exactly_ the pattern a _Remote_ Connector Server is used in production.
>
> In this example, however, the Platform resources are referenced via external URLs, which should sufficiently _simulate_ an actual deployment.

* ## OAuth 2.0 Client

    You could (and probably should) create and use your own, RCS-designated client.

    For the new client, you will need to run the following script in ForgeRock Identity Management (IDM) administrator browser console:

    ```javascript
    var clientId = '<your-client-id>';
    var auth = await $.get('/openidm/config/authentication');

    var clientIdExists = auth.rsFilter.staticUserMapping.find((mapping) => {
        return mapping.subject === clientId;
    });

    if (clientIdExists) {
        console.log(`No changes; ${clientId} exists:`, auth);
        return;
    }

    auth.rsFilter.staticUserMapping.push({
        "subject": clientId,
        "localUser": "internal/user/idm-provisioning",
        "roles": [
            "internal/role/platform-provisioning"
        ]
    });

    auth = await $.ajax({
        "method": "PUT",
        "data": JSON.stringify(auth),
        "url" : "/openidm/config/authentication",
        "headers": {
            "Content-type": "application/json",
            "If-Match": "*"
        }
    });

    console.log(`Added ${clientId}:`, auth);
    ```

    This will add your client as a subject to IDM's static user mapping for the RCS to identify as.

    > As of this writing, the static user mapping in CDK seems to come from [authentication.json](https://stash.forgerock.org/projects/OPENIDM/repos/openidm/browse/openidm-docker/openidm-docker-idm-cdk/src/main/resources/conf/authentication.json#38-55) in IDM image, and is populated with two subjects:
    >
    > ```json
    > {
    >     "rsFilter" : {
    >         . . .
    >         "staticUserMapping" : [
    >             {
    >                 "subject" : "amadmin",
    >                 "localUser" : "internal/user/openidm-admin",
    >                 "roles" : [
    >                     "internal/role/openidm-authorized",
    >                     "internal/role/openidm-admin"
    >                 ]
    >             },
    >             {
    >                 "subject" : "idm-provisioning",
    >                 "localUser" : "internal/user/idm-provisioning",
    >                 "roles" : [
    >                     "internal/role/platform-provisioning"
    >                 ]
    >             }
    >         ]
    >     }
    > }
    > ```
    >
    > If you want to use the included `idm-provisioning` OAuth 2.0 client, pending clarification in https://forgerock.slack.com/archives/C9GPHT607/p1652111787831719?thread_ts=1651876371.803939&cid=C9GPHT607, change its authentication method to `client_secret_post`.
    >
    > If you use `idm-provisioning` in ForgeOps, you can obtain the client's secret with the following command:
    >
    > ```sh
    > $ kubectl get secret amster-env-secrets -o json | jq .data.IDM_PROVISIONING_CLIENT_SECRET -r | base64 -d
    > ```

* ## `ConnectorServer.properties`

    Use the FQDN you had chosen during ForgeOps installation.

    Point the connector server trusted store to the file that contains your self-signed TLS certificate for ForgeOps. More on this later.

    Example `ConnectorServer.properties`:
    ```java
    connectorserver.url=wss://default.iam.example.com:443/openicf

    connectorserver.tokenEndpoint=https://default.iam.example.com/am/oauth2/realms/root/access_token
    connectorserver.clientId=<your-client-id>
    connectorserver.clientSecret=<your-client-secret>
    connectorserver.scope=fr:idm:*
    connectorserver.loggerClass=org.forgerock.openicf.common.logging.slf4j.SLF4JLog

    connectorserver.trustStoreFile=security/keyStore.pkcs12
    connectorserver.trustStoreType=PKCS12
    connectorserver.trustStorePass=changeit
    connectorserver.keyStoreFile=security/keyStore.pkcs12
    connectorserver.keyStoreType=PKCS12
    connectorserver.keyStorePass=changeit
    connectorserver.keyPass=changeit
    ```

* ## Hostname Resolution

    Add a hosts file entry to your RCS.

    Example `rcs.yaml`:

    ```yaml
    apiVersion: apps/v1
    kind: StatefulSet
    metadata:
      name: rcs
    spec:
      replicas: 2
      serviceName: rcs
      selector:
        matchLabels:
          app: rcs
      template:
        metadata:
          labels:
            app: rcs
        spec:
          containers:
          - image:  rcs
            name: rcs
            command:
            - bash
            - -c
            - echo $(HOST_IP) $(HOST_NAME) >> /etc/hosts;
              export OPENICF_OPTS=-Dconnectorserver.connectorServerName=$HOSTNAME && /opt/openicf/bin/docker-entrypoint.sh;
            resources:
              requests:
                memory: "500Mi"
            env:
            - name: HOST_IP
              value: 192.168.64.6
            - name: HOST_NAME
              value: default.iam.example.com
    ```

    Note the command to run in the container and the use of environment variables:

    ```yaml
    # . . .
            - echo $(HOST_IP) $(HOST_NAME) >> /etc/hosts;
    # . . .
            env:
            - name: HOST_IP
              value: 192.168.64.6
            - name: HOST_NAME
              value: default.iam.example.com
    ```

    Update the environment variables values with your ForgeOps-specific ones, the Minikube ip and the FQDN.

    _IF_ your RCS is running on the same host as ForgeOps (which is the case when you run both on Minikube), setting an environment variable and using it in a command allows for obtaining the HOST_IP value _dynamically_, from the pod status data:

    ```yaml
    # . . .
        env:
        - name: HOST_IP
          valueFrom:
            fieldRef:
              fieldPath: status.hostIP
        - name: HOST_NAME
          value: default.iam.example.com
    ```

    Otherwise, for hardcoded values, you could resolve the host by [Adding additional entries with hostAliases](https://kubernetes.io/docs/tasks/network/customize-hosts-file-for-pods/#adding-additional-entries-with-hostaliases).

* ## TLS Certificate

    You might need to import the root certificate authority into your RCS client trust store, especially if you are using a self-signed TLS certificate with your ForgeOps deployment.

    ### Installing TLS Certificate Example

    You could use [Certificate Generated by the mkcert Utility](https://backstage.forgerock.com/docs/forgeops/7.1/cdk/minikube/setup/certificate.html#certificate_generated_by_the_mkcert_utility).

    You might need to perform two additional steps for this certificate to work in ForgeOps:

    1. Comment out the certificate manager annotation in ForgeOps, as it could influence your custom certificate:

        `remove-cert-manager-annotation.patch`
        ```sh
        diff --git a/kustomize/base/ingress/ingress.yaml b/kustomize/base/ingress/ingress.yaml
        index 0c4024d2c..d29163e7c 100644
        --- a/kustomize/base/ingress/ingress.yaml
        +++ b/kustomize/base/ingress/ingress.yaml
        @@ -9,7 +9,7 @@ metadata:
             nginx.ingress.kubernetes.io/ssl-redirect: "true"
             # CORS is now set in the AM and IDM configurations
             nginx.ingress.kubernetes.io/enable-cors: "false"
        -    cert-manager.io/cluster-issuer: $(CERT_ISSUER)
        +    # cert-manager.io/cluster-issuer: $(CERT_ISSUER)
             nginx.ingress.kubernetes.io/body-size: "64m"
             nginx.ingress.kubernetes.io/send-timeout: "600"
             nginx.ingress.kubernetes.io/proxy-body-size: "64m"
        @@ -132,7 +132,7 @@ metadata:
           annotations:
             nginx.ingress.kubernetes.io/ssl-redirect: "true"
             nginx.ingress.kubernetes.io/rewrite-target: "/$2"
        -    cert-manager.io/cluster-issuer: $(CERT_ISSUER)
        +    # cert-manager.io/cluster-issuer: $(CERT_ISSUER)

         spec:
           ingressClassName: nginx
        ```

        As an alternative, you could try running this command in your cluster:

        ```sh
        $ kubectl annotate ingress forgerock cert-manager.io/cluster-issuer-
        ```

    1. Update your TLS secret in ForgeOps:

        ```sh
        $ kubectl create secret tls sslcert --cert /path/to/_wildcard.iam.example.com.pem --key /path/to/_wildcard.iam.example.com-key.pem --save-config --dry-run=client -o yaml | kubectl apply -f -
        ```

    ### Importing the Root Certificate

    If you do use the `mkcert` tool, you can extract your root certificate as described in [Installing the CA on other systems](https://github.com/FiloSottile/mkcert#installing-the-ca-on-other-systems).

    Once you have your root certificate, you can add it to the trusted store in the RCS image.

    Example `Dockerfile`:

    ```docker
    FROM gcr.io/forgerock-io/icf/docker-build:1.5.20.5

    COPY logback.xml /opt/openicf/lib/framework
    COPY ConnectorServer.properties /opt/openicf/conf
    COPY scripts /opt/openicf/scripts
    COPY rootCA.pem /opt/openicf/security/rootCA.pem

    RUN echo $(keytool -keystore /opt/openicf/security/keyStore.pkcs12 -import -trustcacerts -storetype pkcs12 -file /opt/openicf/security/rootCA.pem -alias forgeops-ca -storepass changeit -noprompt)

    # root needed for file sync in dev mode
    USER root
    ```

    > Optionally, you could execute into the container and check if the import was successful:
    >
    > ```sh
    > $ keytool -keystore /opt/openicf/security/keyStore.pkcs12 -storepass changeit -list -v
    > ```
    > &nbsp;

    More general information on this subject could be found in:
    * [FAQ: SSL certificates and secured connections in IDM](https://backstage.forgerock.com/knowledge/kb/article/a61462878)
    * [Secure the Connection to the Connector Server With SSL](https://backstage.forgerock.com/docs/idm/7/connector-reference/configure-rcs-ssl.html)


    * ## Deploying in Minikube

    Use minkube context:

    ```sh
    $ kubectl config use-context minikube
    ```

    Use Minikube's Docker engine:

    ```sh
    $ source <(minikube docker-env)
    ```

    Stop Skaffold from pushing Docker images to a remote Docker registry:

    ```sh
    $ skaffold config set --kube-context minikube local-cluster true
    ```

    Related:
    * [Skaffold Environment Management > Local Cluster](https://skaffold.dev/docs/environment/local-cluster/)
    * [CDK Envrionment Setup > Minikube's Docker Engine](https://backstage.forgerock.com/docs/forgeops/7.1/legacy/cdk/minikube/setup/docker.html)


    Deploy:

    ```sh
    $ skaffold dev
    ```
