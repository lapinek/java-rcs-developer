# Developing Scripted Connectors for Java Remote Connector Server (RCS)

In a managed environment that [ForgeRock Identity Cloud](https://backstage.forgerock.com/docs/idcloud/latest/home.html) (Identity Cloud) presents, [syncing identities](https://backstage.forgerock.com/docs/idcloud/latest/identities/sync-identities.html) via a remote server provides necessary flexibility in integrating your [ForgeRock Identity Platform](https://backstage.forgerock.com/docs/platform) (Platform) with external systems.

Scripted implementations extend this flexibility further and almost indefinitely.

The following content aims to overlay existing ever-evolving docs with additional structure and details that will hopefully help one understand better the RCS setup and using it to its maximum capabilities.

While primary focus and many references in this article are pointed to Identity Cloud, much of the lower level considerations should be universally applicable to other current RCS scripted solutions.

## <a id="contents" name="contents"></a>Contents

* [Choosing IDE](#developing-ide)
* [Interacting with RCS via IDM's REST](#developing-idm-rest)
* [Debugging Scripts](#developing-debugging-scripts)
    * [Try and Catch](#developing-debugging-scripts-try-catch)
    * [Custom Logs](#developing-debugging-scripts-custom-logs)
    * [Attaching Debugger to Kubernetes Deployment](#developing-debugging-scripts-debugger)
* [Connector Configuration](#developing-connector-configuration)
    * ["configurationProperties"](#developing-connector-configuration-configuration-properties)
        * ["customConfiguration" and "customSensitiveConfiguration"](#developing-connector-configuration-configuration-properties-custom-configuration)
    * ["systemActions"](#developing-connector-configuration-system-actions)
        * [Defining System Action](#developing-connector-configuration-system-actions-definition)
            * ["scriptId"](#developing-connector-configuration-system-actions-definition-script-id)
            * ["actions"](#developing-connector-configuration-system-actions-definition-actions)
                * ["systemType"](#developing-connector-configuration-system-actions-definition-actions-system-type)
                * ["actionType"](#developing-connector-configuration-system-actions-definition-actions-action-type)
                * ["actionSource" _or_ "actionFile"](#developing-connector-configuration-system-actions-definition-actions-action-source-or-file)
        * [Invoking via REST](#developing-connector-configuration-system-actions-rest)
            * [Parts of the Request](#developing-connector-configuration-system-actions-rest-parts)
                * [/openidm/system/\<connector-name\> (connection endpoint)](#developing-connector-configuration-system-actions-rest-parts-path)
                * [?_action=script (execute script)](#developing-connector-configuration-system-actions-rest-parts-action)
                * [&scriptId=\<script_id\> (identifier for system action to execute and return from)](#developing-connector-configuration-system-actions-rest-parts-script-id)
                * [&arg1=value1&arg2=value2 . . . (script arguments)](#developing-connector-configuration-system-actions-rest-parts-request-params)
                * [request body (script arguments)](#developing-connector-configuration-system-actions-rest-parts-request-body)
                * [&scriptExecuteMode=resource ("run on resource")](#developing-connector-configuration-system-actions-rest-parts-execute-mode)
            * ["run on resource" vs "run on connector"](#developing-connector-configuration-system-actions-rest-execute-modes)
        * [Invoking from IDM Script](#developing-connector-configuration-system-actions-idm-script)
            * [Syntax](#developing-connector-configuration-system-actions-idm-script-syntax)
            * [Examples](#developing-connector-configuration-system-actions-idm-script-examples)
                * ["run on connector"](#developing-connector-configuration-system-actions-idm-script-examples-on-connector)
                * ["run on resource"](#developing-connector-configuration-system-actions-idm-script-examples-on-resource)
        * [Support in Connectors](#developing-connector-configuration-system-actions-support)
* [Example Connectors](#example-connectors)
    * [Scripted SQL Connector](#example-connectors-scripted-sql)

## <a id="developing-ide" name="developing-ide"></a>Choosing IDE

[Back to Contents](#contents)

For a Java RCS, you will write scripts in [the Apache Groovy programming language](https://groovy-lang.org/) (Groovy). Consult the [IDE integration support for Groovy](https://groovy-lang.org/ides.html) when you choose your IDE for RCS script development.

In general, you can get a better support for Groovy in a Java-specialized IDE, like [IntelliJ IDEA](https://www.jetbrains.com/idea/) (IntelliJ).

In a non-Java or in a polyglottal IDE, you might be able to effectively maintain your RCS scripts, but Groovy-related features may not be readily available or have limited functionality and support (in comparison to IntelliJ).

> For example, as of this writing, no Groovy debugger extension is available for Visual Code Studio—a very popular code editor. This means that, if you want to do remote debugging and attach a debugger to your RCS process, you will have to use something like IntelliJ.

## <a id="developing-idm-rest" name="developing-idm-rest"></a>Interacting with RCS via IDM's REST

A remote connector is a [system object](https://backstage.forgerock.com/docs/idcloud-idm/latest/objects-guide/appendix-system-objects.html), and as such, you can interact with it via [IDM's REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/rest-api-reference/endpoints/rest-system-objects.html).

You will need to authorize your requests to IDM's REST as an IDM administrator.

In Identity Cloud, this means providing an OAuth 2.0 bearer token in the `Authorization` header of your request. The token needs to be obtained with a client mapped to an IDM subject associated with the admin role.

The easiest way of accomplishing this type of authorization is signing in to the IDM admin UI, and using the browser console for making HTTP request with `jQuery`. Internally, `jQuery` uses `XMLHttpRequest` (XHR), and such requests are automatically authorized by the IDM admin UI.

For example (where `[ . . . ]` denotes omission from the original content):

`IDM Admin Browser Console`

```javascript
(async function () {
    var data = await $.ajax('/openidm/system?_action=availableConnectors', {
        method: 'POST'
    });

    console.log(JSON.stringify(data, null, 4));
}());
```

In the output, this will produce similar to the following:

```json
{
    "connectorRef": [
        {
            "connectorHostRef": "rcs",
            "displayName": "SSH Connector",
            "bundleVersion": "1.5.20.12-SNAPSHOT",
            "systemType": "provisioner.openicf",
            "bundleName": "org.forgerock.openicf.connectors.ssh-connector",
            "connectorName": "org.forgerock.openicf.connectors.ssh.SSHConnector"
        },
        [ . . . ]
        {
            "connectorHostRef": "rcs",
            "displayName": "CSV File Connector",
            "bundleVersion": "1.5.20.12-SNAPSHOT",
            "systemType": "provisioner.openicf",
            "bundleName": "org.forgerock.openicf.connectors.csvfile-connector",
            "connectorName": "org.forgerock.openicf.csvfile.CSVFileConnector"
        }
    ]
}
```

Alternatively, you could use an IDE like [Postman](https://www.postman.com/) for crafting your requests to IDM's REST.

You can also obtain your access token separately and use with [cURL](https://curl.se/). For example, an equivalent to the aforementioned request made with cURL will look similar to the following:

```sh
curl 'https://openam-dx-kl03.forgeblocks.com/openidm/system?_action=availableConnectors' \
-X POST \
-H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiI1Sk9EejJwNVFIS08wUFNZTWlEL1lXT3Zkc0U9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI4NWRmM2JiMC04MGRkLTRkMTAtYWE3MC1jZWY5MWExZWY5NTciLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiZmZmMDljNjctZGVjOS00OGIyLWIxNWMtNjljZjI0OTk4OTA1LTEyNDI2MCIsInN1Ym5hbWUiOiI4NWRmM2JiMC04MGRkLTRkMTAtYWE3MC1jZWY5MWExZWY5NTciLCJpc3MiOiJodHRwczovL29wZW5hbS1keC1rbDAzLmZvcmdlYmxvY2tzLmNvbTo0NDMvYW0vb2F1dGgyIiwidG9rZW5OYW1lIjoiYWNjZXNzX3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFudElkIjoiU1lETzYweGQ4eG5YYnAyYzNBYkJpS0F3MmVnIiwiYXVkIjoiaWRtQWRtaW5DbGllbnQiLCJuYmYiOjE2NzMwNDM5OTEsImdyYW50X3R5cGUiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzY29wZSI6WyJmcjppZG06KiJdLCJhdXRoX3RpbWUiOjE2NzMwNDM1MTIsInJlYWxtIjoiLyIsImV4cCI6MTY3MzA0NzU5MSwiaWF0IjoxNjczMDQzOTkxLCJleHBpcmVzX2luIjozNjAwLCJqdGkiOiJTN0kwZThMR2poVWp1NkZ0N3NqSWNDdnotQ2sifQ.iE5n1lZav7ITdedWXGeGtAG6jULLzxSlhQwyfQz4yBR6LOsrjcRk9sa-ULuTiWET51mRGsyTzbSpjhxk0FkN3AjWCqgDaltRoI4x2P3j-Q-jhmGschTiVksI0XNDiWkEbqhTj-cYSO0SazbTNKD3r5kkhhU3II7dgvX3dcBIzLXydqDwbALPzCHJAoeWO1Q-Sf7bt6EMZmCMy2g9Nf3lzVa3q9RH3j65iyyMmPvJLu-SUnwQZr2JAJS_0a9cTB2UL_bVo2tC9WNkdCoQ_952Bdzv-_txWkDl-6paHDUeeZegq2CK4t4ldh3RYdbFZVoMlrPM32fHWxaT0T2MSWMLNA' \
-H 'Content-Length: 0'
```

Browser console, however, can serve well as an IDE for functional programming in JavaScript. It will provide an interactive playground with code highlighting, autocompletion, and error checking. It will add necessary headers to your requests. And, in the case of IDM admin UI, it will have jQuery preloaded and its requests authorized. Finally, it will provide convenient output that you can interact with.

We will use this technique in the examples below.

## <a id="developing-debugging-scripts" name="developing-debugging-scripts"></a>Debugging Scripts

[Back to Contents](#contents)

### <a id="developing-debugging-scripts-try-catch" name="developing-debugging-scripts-try-catch"></a>Debugging Scripts > Try and Catch

[Back to Contents](#contents)

If an unhandled error occurs in your RCS scripts, depending on the script, it may result in a malformed or blank screen in IDM admin UI, an unnecessarily detailed error message sent to the client side, and no debug information in the RCS logs.

Therefore, you should wrap your code with a `try/catch` block, send custom error messages to the logs output, and, potentially, throw a custom exception.

For example:

`SearchScript.groovy`

```groovy
try {
    def message = operation + ' script'

    [ . . . ]
} catch (e) {
    log.error 'EXCEPTION: ' + e.message
    throw new UnsupportedOperationException('Error occurred during ' + operation + ' operation')
}
```

> [UnsupportedOperationException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/UnsupportedOperationException.html) is a Java exception, which, among some other most commonly used Java classes, is [automatically provided in Groovy scripts](https://groovy-lang.org/structure.html#_default_imports).

> To convert an object to a String using the plus operator, you have to start your expression with a String, and use the String concatenation operator: `def message = 'script ' + operation`. >
>
> An object's `.plus()` method invoked by the `+` operator might not be taking a String as its argument. Hence, for the `operation` object, this particular expression in the `try` block will throw an exception.

When you look for a connector data in the IDM admin UI under CONFIGURE > CONNECTORS > _connector name_ > Data, it will call the search script, and the browser response will contain:

```json
{"code":404,"reason":"Not Found","message":"Error occurred during SEARCH operation"}
```

For a search script, the browser response will be reflected in the UI as well:

<img alt="Error Message in IDM admin UI in a Connector Screen reading Error occurred during SEARCH operation." src="README_files/rcs.connector.search-script.handled-exception.png" width="1024">

At the same time, in the RCS logs, you will find a more detailed message with additional information (added manually and/or by the `Log` class) that could help with your debugging:

`RCS logs`

```
[rcs] Jun 22, 2022 2:35:11 AM ERROR SearchScript: EXCEPTION: No signature of method: org.forgerock.openicf.connectors.groovy.OperationType.plus() is applicable for argument types: (String) values: [ script]%0APossible solutions: values(), split(groovy.lang.Closure), is(java.lang.Object), use([Ljava.lang.Object;), wait(), name()
```

### <a id="developing-debugging-scripts-custom-logs" name="developing-debugging-scripts-custom-logs"></a>Debugging Scripts > Custom Logs

[Back to Contents](#contents)

You can use methods of the [Log](https://backstage.forgerock.com/docs/idcloud-idm/latest/_attachments/apidocs/org/identityconnectors/comprintlnmon/logging/Log.html) class to output custom logs from your connector scripts by passing in a String containing your debugging content.

For example:

`TestScript.groovy`

```groovy
log.info 'This is ' + operation + ' script'
```

`RCS logs`

```
[rcs] Jul 20, 2022 12:41:12 AM INFO  TestScript: This is TEST script
```

Using methods of the `Log` class might require some extra processing applied to the content you are trying to output:

* To output an object information without referencing its individual properties you may need to convert it to a String first. Otherwise, you could get a wordy error.

    For (an error) example:

    `TestScript.groovy`

    ```groovy
    try {
        log.info operation
    } catch (e) {
        log.error e.message
    }
    ```

    `RCS logs`

    ```
    [rcs] Jul 20, 2022 11:43:45 PM ERROR TestScript: No signature of method: org.identityconnectors.common.logging.Log.info() is applicable for argument types: (org.forgerock.openicf.connectors.groovy.OperationType) values: [TEST]%0APossible solutions: info(java.lang.String, [Ljava.lang.Object;), isInfo(), info(java.lang.Throwable, java.lang.String, [Ljava.lang.Object;), isOk(), find(), any()
    ```

    You can convert an object to string by using its `.toString()` method or by prepending your log with a String:

    For example:

   `TestScript.groovy`

    ```groovy
    try {
        log.info 'Operation: ' + operation
    } catch (e) {
        log.error e.message
    }
    ```

    `RCS logs`

    ```
    [rcs] Jul 20, 2022 11:47:24 PM INFO  TestScript: Operation: TEST
    ```

* If you try to output raw JSON describing an object, its curly braces will be interpreted as formatting syntax (by internally used [MessageFormat](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/MessageFormat.html)). This might produce an error if the content of the curly braces is not a number.

    For (an error) example:

    `TestScript.groovy`

    ```groovy
    try {
        log.info '{"key": "value"}'
    } catch (e) {
        log.error e.message
    }
    ```

    `RCS logs`

    ```
    [rcs] Oct 19, 2022 06:55:08 PM ERROR TestScript: cant parse argument number: "key": "value"
    ```

    To mitigate this issue, you should parse the JSON first _and_ convert the resulting object to a String.

    For example:

     `TestScript.groovy`

    ```groovy
    import groovy.json.JsonSlurper

    try {
        log.info 'JSON object: ' + (new JsonSlurper().parseText('{"key": "value"}'))
    } catch (e) {
        log.error e.message
    }
    ```

    `RCS logs`

    ```
    [rcs] Oct 19, 2022 06:56:41 PM INFO  TestScript: JSON object: [key:value]
    ```

Methods of the `Log` class add additional information to the output: a timestamp, the log level, and the source reference. You should use `Log` for debugging output that is to stay in the code and be used in test and production.

During the development phase, however, for a quick and temporary output, you could use the [println](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/PrintStream.html) method. `System.out.println` will automatically apply `.toString()` method available in all Java objects to the content it outputs. This will allow to print out content of different types of variables without additional processing. It will be up to you to provide any extra info in the output.

> Much of Java functionality is [imported in Groovy by default](https://groovy-lang.org/structure.html#_default_imports), including the `java.lang.*` package where `println` comes from. Hence, you don't need to use the full `System.out.println` statement.

For example:

`TestScript.groovy`

```groovy
try {
    println operation
} catch (e) {
    log.error e.message
}
```

`RCS logs`

```
[rcs] TEST
```

### <a id="developing-debugging-scripts-debugger" name="developing-debugging-scripts-debugger"></a>Debugging Scripts > Attaching Debugger to Kubernetes Deployment

[Back to Contents](#contents)

Attaching a debugger to your RCS process will allow to pause a connector execution at different points in your code and inspect the current state of your connector scripts. Doing so can help to locate and eliminate programming errors.

RCS can be deployed in a [Docker](https://www.docker.com/) container within a [Kubernetes](https://kubernetes.io/) cluster.

Java RCS deployed in a Docker container will run in a remote Java Virtual Machine (JVM). In order to attach a debugger to this process from your local development setup, you will need to perform the following steps:

1. Start your RCS JVM with the [Java Debug Wire Protocol (JDWP)](https://docs.oracle.com/en/java/javase/11/docs/specs/jpda/conninv.html#oracle-vm-invocation-options) options.

    You can specify the JDWP options in a few alternative ways:

    * Engage the ICF defaults.

        You could rely on the default JDWP options defined for the RCS Docker container. You can do it by supplying the expected `jpda` argument to the ICF's [Docker ENTRYPOINT](https://docs.docker.com/engine/reference/builder/#entrypoint) script:

        `/opt/openicf/bin/docker-entrypoint.sh`:

        ```sh
        [ . . . ]
        if [ "$1" = "jpda" ] ; then
        if [ -z "$JPDA_TRANSPORT" ]; then
            JPDA_TRANSPORT="dt_socket"
        fi
        if [ -z "$JPDA_ADDRESS" ]; then
            JPDA_ADDRESS="5005"
        fi
        if [ -z "$JPDA_SUSPEND" ]; then
            JPDA_SUSPEND="n"
        fi
        if [ -z "$JPDA_OPTS" ]; then
            JPDA_OPTS="-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
        fi
        OPENICF_OPTS="$OPENICF_OPTS $JPDA_OPTS"
        shift
        fi
        [ . . . ]
        ```

        > JDWP is a part of Java Platform Debugger Architecture; hence, the JPDA abbreviation used in the ICF code.

        In a Kubernetes manifest for your RCS, the `jpda` argument can be added to the command that calls the Docker ENTRYPOINT script.

        For example:

        `rcs.yaml`

        ```sh
        [ . . . ]
        command: ['bash', '-c']
        args:
        - export OPENICF_OPTS="-Dconnectorserver.connectorServerName=$HOSTNAME [ . . . ]"
          && /opt/openicf/bin/docker-entrypoint.sh jpda;
        [ . . . ]
        ```

    * Provide custom JDWP options at RCS launch.

        Alternatively, you can include your (custom) JDWP options in the `OPENICF_OPTS` environment variable defined in your Kubernetes manifest.

        For example:

        `rcs.yaml`

        ```sh
        [ . . . ]
        command: ['bash', '-c']
        args:
        - export OPENICF_OPTS="-Dconnectorserver.connectorServerName=$HOSTNAME [ . . . ]
          -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n"
          && /opt/openicf/bin/docker-entrypoint.sh;
        [ . . . ]
        ```

    * Provide (custom) JDWP options at runtime.

        You can dynamically apply an environment variable to your RCS containers by using [kubectl set env](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#set) command. With this command, you can update `JAVA_OPTS` for the JVM running in the containers.

        For example:

        `Terminal`

        ```sh
        $ kubectl set env statefulsets/rcs -c rcs JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n"
        ```

        > If you use [Skaffold](https://skaffold.dev/docs/), updating `JAVA_OPTS` will restart your StatefulSet/Deployment.

        You can check the updated environment with the `--list` option.

        For example:

        `Terminal`

        ```sh
        $ kubectl set env statefulsets/rcs -c rcs --list=true
        ```

        ```
        # StatefulSet rcs, container rcs
        JAVA_OPTS= -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n
        ```

        You can remove the variable and its effects with the (negative) `JAVA_OPTS-` option.

        For example:

        `Terminal`

        ```sh
        $ kubectl set env statefulsets/rcs -c rcs JAVA_OPTS-
        ```

    In all cases, the JDWP address option is the remote JVM's TCP/IP port, to which your local debugger will eventually connect. It will be a _local to the RCS instance_ port, but to distinguish it from the port on the debugger machine, we will call it "remote".

    > Optionally, you can include the host information in the address option, an IP or the `localhost` designation, to limit where the debugger connection could be made from; for example: `address=127.0.0.1:5005`.
    >
    > If you omit the host identifier in the Java Development Kit (JDK) 9 and above, the connection will be limited to `localhost`. In the older versions of JDK, if no host is specified, a connection would be allowed from any IP. To achieve the same behavior in JDK 9+, you can use a wildcard as the host value; for example, `address=*:5005`. It is considered the best practice, however, to limit connections to a specific IP.
    >
    > In the case of attaching a debugger to RCS, leaving the host information out, and thus limiting the debugger connection to localhost, is the easiest option.

2. Allow your local debugger to communicate with the RCS process via the remote debugging port specified in the JDWP options.

    Your RCS deployment and its debugging port are unlikely to be exposed externally. This means, you will need to let your debugger access the remote process by [forwarding connections made to a local port on your machine to a remote port on the RCS pods in your Kubernetes cluster](https://kubernetes.io/docs/tasks/access-application-cluster/port-forward-access-application-cluster/#forward-a-local-port-to-a-port-on-the-pod).

    Here, the local port is the one you will use in your debugger configuration; the remote port is the one that you specified in the JDWP `address` option. In the following command, the "local" port is on the left and the "remote" one is on the right:

    `Terminal`

    ```sh
    $ kubectl port-forward statefulsets/rcs 5005:5005

    Forwarding from 127.0.0.1:5005 -> 5005
    Forwarding from [::1]:5005 -> 5005
    ```

3. Configure your debugger for remote debugging and start a debugging session.

    IntelliJ is a popular IDE that has rich and refined support for Java and Groovy; and thus, it is probably going to be your best option for developing Groovy scripts for RCS. Below, find an example of how you can configure IntelliJ for remote debugging and attach its debugger to your RCS process:

    1. Create a new IntelliJ project.

        For example, you can use `File > New > Project from Existing Sources...` and point it to the folder that contains your project files, such as README, configuration, etc., and the actual scripts; then, the folder content could be accessed and maintained under `Project > Project Files`. Do not import any sources at this point; you will add the scripts you need to debug as a module in the next step.

        Open the project.

    1. Add a new module with `File > New Module from Existing Sources...` and point it to your connector's scripts location.

        If/when you have more than one connector in your RCS, mark only the connector-specific scripts as the source files in the Import Module dialog. Creating a separate module from the existing scripts for each connector will let you reference the module in a debugging configuration and thus limit its scope to the scripts for a particular connector.

        > Otherwise, if you included files with the same name for more than one connector in a module, and set a breakpoint in one of the namesake scripts, the debugger could open a file with the same name for a different connector—the first script file with this name that was was found in the module sources.

        The module files will serve as the [sources of your (RCS) application](https://www.jetbrains.com/help/idea/attaching-to-local-process.html#prerequisites), which is one of the prerequisites for attaching a debugger in IntelliJ.

        For example, two modules registered for a project might appear under the Project Files in the following manner:

        <img alt="Two modules registered in a project and appearing under Project Files: groovy and postgres. The latter is expanded to show existing source files and their location." src="README_files/intellij.project-files.modules.png" width="428">

    1. Select `Run` > `Edit Configurations...`
    1. Select `Add New Configuration` (`+`), then select `Remote JVM Debug` from the list of predefined configuration templates.
    1. In the `Configuration` tab, provide values (or verify the defaults) for the following settings:
        1. `Name`: _your-rcs-connector-debugging-configuration-name_
        1. `Debugger mode`: Attach to remote JVM
        1. `Host`: localhost

            The host to which the debugger will connect. Choose localhost because we, actually, attempt to debug locally (that is, the debugger runs locally and connects to a local port, and then it is forwarded to a remote port in the Kubernetes cluster); you could also use `127.0.0.1` or `::1` as the Host value.

        1. `Port`: 5005

            The local port the debugger will connect to, from which we forward connections to the remote port (with `kubectl port-forward [ . . . ]`).

        1. `Command line arguments for remote JVM (for JDK 9 or later)`: JDK 9 or later

            This input is to provide a template for your JDWP options according to your previous choices, and you will see the following:

            `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`

            Note, however, that:

            * The `*` prefix  in `JDK 9+` means that the connection will be allowed on the remote host from any IP.

                Leaving the host information out and having just the port specified will limit connections to the localhost only, which is the safest option that will work in this case.

                > Removing the wildcard or replacing it with a specific identifier (for example, an IP or `localhost`) is considered the best practice. In reality, however, it is unlikely that any IP/port will be made public on your RCS; hence, limiting debugging connections to your JVM might be a minor consideration in this case.

            * The remote JVM port is populated with the same number as your local debugger port, for it assumes that the two ports, local and remote, are the same.

                > _If_ the remote debugging port in your RCS were different from the local one, you could still use this input for getting your JDWP options template, and simply update the port with the actual remote port that you will use for debugging.
                >
                > Naturally, in such case, you would also need to port-forward your local connections to _that_ port with the `kubectl port-forward [ . . . ]` command.
                >
                > For example, if your remote JVM port is `5006`, your _actual_ JDWP options could look like the following:
                >
                > `Terminal`
                >
                > ```sh
                > $ kubectl set env statefulsets/rcs -c rcs JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"
                > ```
                >
                > and your port-forwarding command would be:
                >
                > `Terminal`
                >
                > ```
                > kubectl port-forward statefulsets/rcs 5005:5006
                > ```

        1. `Use module classpath`: _your-rcs-connector-module-name_

            Here, you reference the module created from your existing connector's scripts—so that the debugger looks for the breakpoints set only in those files.

        1. The end result may look similar to this:

            <img alt="Run/Debug Configurations Window in IntelliJ" src="README_files/intellij.debug-configuration.png" width="1024" />

            Select `Apply` or `OK`.

    1. Start debugging.

        For example, you can select the bug button in the upper right:

        <img alt="Debug Button Highlighted in IntelliJ Tool Bar" src="README_files/intellij.debug-run.png" width="512" />

    1. Add breakpoints.

        If everything is set up correctly, you should be able to see the breakpoints [verified](https://www.jetbrains.com/help/idea/using-breakpoints.html#breakpoint-icons) and employed when you are using your connector.

        For example:

        <img alt="IntelliJ Window with Active Debug Panel" src="README_files/intellij.debug-running.png" width="1024" />

    1. If you want to to use debugger with another scripted connector in the same IntelliJ project:

        * Add a new module via `File > New Module from Existing Sources...`, and point it to the other connector's scripts.

        * Under `Run > Edit Configurations...`, add a new remote JVM debugging configuration, and select the new module in the `Use module classpath:` input.

        * Select the new debug configuration before you start your debug session.

            For example:

            <img alt="Opened Select Debug Configuration Dialog in IntelliJ Tool Bar" src="README_files/intellij.debug-run-select-configuration.png" width="382" />

    For additional details, consult the IntelliJ docs on [setting debugging environment](https://www.jetbrains.com/help/idea/creating-and-editing-run-debug-configurations.html) and [debugging](https://www.jetbrains.com/help/idea/debugging-code.html#general-procedure).

This should help understand the process of attaching a debugger to your RCS instance running in a Kubernetes cluster. Change it according to your specific requirements.

##  <a id="developing-connector-configuration" name="developing-connector-configuration"></a>Connector Configuration

[Back to Contents](#contents)

The docs outline general steps of [Configuring connectors over REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST). Configuration properties for different remote connector types (that are available in Identity Cloud) can be found under [Connector reference](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/preface.html) > Remote Connectors.

This section will elaborate on some additional details, not currently presented in the docs.

###  <a id="developing-connector-configuration-configuration-properties" name="developing-connector-configuration-configuration-properties"></a>Connector Configuration > "configurationProperties"

[Back to Contents](#contents)

The "configurationProperties" key in connector configuration contains settings that are specific to the target system.

####  <a id="developing-connector-configuration-configuration-properties-custom-configuration" name="developing-connector-configuration-configuration-properties-custom-configuration"></a>Connector Configuration > "configurationProperties" > "customConfiguration" and "customSensitiveConfiguration"

[Back to Contents](#contents)

The docs provide an [example of using customConfiguration and customSensitiveConfiguration](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/kerberos.html#ssh-kerberos-config):

```json
"customConfiguration" : "kadmin { cmd = '/usr/sbin/kadmin.local'; user = 'openidm/admin'; default_realm = 'EXAMPLE.COM' }",
"customSensitiveConfiguration" : "kadmin { password = 'Passw0rd' }",
```

The variables set in `custom(Sensitive)Configuration` will become configuration properties in the script.

It might not be entirely clear, though, how (and why) this Groovy-like syntax works, and how custom configuration options set this way could be used in a script.

The content provided in "customConfiguration" will be evaluated with [a matching parse method of the groovy.util.ConfigSlurper class](https://docs.groovy-lang.org/latest/html/gapi/groovy/util/ConfigSlurper.html#method_summary).

> For reference: [OpenICF > connectors > [ . . . ] > ScriptedConfiguration.java](https://stash.forgerock.org/projects/OPENICF/repos/connectors/browse/groovy-common/src/main/java/org/forgerock/openicf/connectors/groovy/ScriptedConfiguration.java#563-569,576-584).

In particular, the [parse(String script)](https://docs.groovy-lang.org/latest/html/gapi/groovy/util/ConfigSlurper.html#parse(java.lang.String)) method accepts a special script that will set variables using variable assignment or a [Closure](https://groovy-lang.org/closures.html) syntax:

* Assigning variables:

    ```json
    "customConfiguration": "key1 = 'value1';"
    ```

* Defining closures:

    ```json
    "customConfiguration": "key1 { key2 = 'value2'; };"
    ```

If you use a closure for setting "customConfiguration", the word preceding the closure becomes a property that contains a map with keys set by the closure code. Using closure syntax may reduce the redundant clutter if a map is referenced multiple times. It will also allow for some processing before a variable is assigned. But you can use variable assignment or dot notation for creating a map as well.

For example, you could have the following configuration for your connector:

`provisioner.openicf-<connector-name>.json`

```json
{
    "connectorRef": {
        "connectorHostRef": "rcs",
        "bundleVersion": "1.5.20.6-SNAPSHOT",
        "bundleName": "org.forgerock.openicf.connectors.groovy-connector",
        "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector"
    },
    "configurationProperties": {
        "customConfiguration": "key1 = 'value1'; key2 = 'value2'; map1 { key1 = 'value3'; key2 = 'value4'; }; map2.key1 = 'value5'; map2.key2 = 'value6'; map3 = [ key1: 'value7', key2: 'value8' ];",
        [ . . . ]
    }
    [ . . . ]
}
```

Note that multi-line statements are not supported in JSON. This means, you must put all "customConfiguration" in one line, and separate multiple Groovy statements with semicolons.

> You _could_ use new lines for separating Groovy statements as well:
>
> ```json
> [ . . . ]
> "customConfiguration": "key1 = 'value1'\n key2 = 'value2'\n map1 { key1 = 'value3'\n key2 = 'value4'\n }\n map2.key1 = 'value5'\n map2.key2 = 'value6'\n map3 = [ key1: 'value7', key2: 'value8' ]\n",
> [ . . . ]
>```

The parsed configuration will be used to populate the scripts' `configuration.propertyBag` binding.

For example, with the aforementioned connector configuration in place, the output of `propertyBag` properties could look like the following:

`TestScript.groovy`
```groovy
    println configuration.propertyBag.key1
    println configuration.propertyBag.key2
    println configuration.propertyBag.map1.key1
    println configuration.propertyBag.map1.key2
    println configuration.propertyBag.map2.key1
    println configuration.propertyBag.map2.key2
    println configuration.propertyBag.map3.key1
    println configuration.propertyBag.map3.key2
```

`RCS logs`

```
[rcs] value1
[rcs] value2
[rcs] value3
[rcs] value4
[rcs] value5
[rcs] value6
[rcs] value7
[rcs] value8
```

If you don't want your connector configuration to be exposed in clear text in IDM, you can also supply the `propertyBag` content via the connector's "customSensitiveConfiguration" configuration property. The information defined with the "customSensitiveConfiguration" key will be encrypted on IDM side; its content will become a [GuardedString](https://docs.oracle.com/en/middleware/idm/identity-governance/12.2.1.3/omicf/index.html?org/identityconnectors/common/security/GuardedString.html) (that is, encrypted string), and it will continue to be encrypted in transport to the RCS.

The properties defined in "customSensitiveConfiguration" will overwrite the same keys provided in "customConfiguration".

For example:

`provisioner.openicf-<connector-name>.json`

```json
{
    [ . . . ]
    "configurationProperties": {
        "customConfiguration": "key1 = 'value1'; key2 = 'value2'; map1 { key1 = 'value3'; key2 = 'value4'; }; map2.key1 = 'value5'; map2.key2 = 'value6'; map3 = [ key1: 'value7', key2: 'value8' ];",
        "customSensitiveConfiguration": "key1 = 'sensitive-value1'; map1 { key1 = 'sensitive-value3'; }; map2.key1 = 'sensitive-value5'; map3 = [ key1: 'sensitive-value7' ];",
        [ . . . ]
    }
    [ . . . ]
}
```

`TestScript.groovy`
```groovy
    println configuration.propertyBag.key1
    println configuration.propertyBag.key2
    println configuration.propertyBag.map1.key1
    println configuration.propertyBag.map1.key2
    println configuration.propertyBag.map2.key1
    println configuration.propertyBag.map2.key2
    println configuration.propertyBag.map3.key1
    println configuration.propertyBag.map3.key2
```

`RCS logs`

```
[rcs] sensitive-value1
[rcs] value2
[rcs] sensitive-value3
[rcs] value4
[rcs] sensitive-value5
[rcs] value6
[rcs] sensitive-value7
[rcs] value8
```

As with many things processed on IDM side, you can use [property value substitution](https://backstage.forgerock.com/docs/idm/7.2/setup-guide/using-property-substitution.html) in the "custom(Sensitive)Configuration" values. In addition to the IDM variables, in Identity Cloud, you can reference [Environment-Specific Variables and Secrets (ESVs)](https://qa.forgerock.com/docs/docs-pr/idcloud/antora/594/idcloud/latest/tenants/esvs.html) in a connector configuration.

For example:

`provisioner.openicf-<connector-name>.json`

```json
{
    [ . . . ]
    "configurationProperties": {
        "customConfiguration": "oauth2 { provider = 'https://&{fqdn}/'; client_id = 'client-id' }",
        "customSensitiveConfiguration": "oauth2 { client_secret = '&{esv.my.secret}' }",
        [ . . . ]
    }
    [ . . . ]
}
```

`TestScript.groovy`

```groovy
println configuration.propertyBag
```

`RCS logs`

```
[rcs] [oauth2:[client_secret:esv-my-secret  value, provider:https://openam-dx-kl02.forgeblocks.com/, client_id:client-id]]
```

> Unfortunately, [one has to be told what _IDM_ variables are](https://backstage.forgerock.com/docs/idm/7.2/setup-guide/using-property-substitution.html#expression-evaluation) in Identity Cloud. You cannot see it for yourself.

###  <a id="developing-connector-configuration-system-actions" name="developing-connector-configuration-system-actions"></a>Connector Configuration > "systemActions"

[Back to Contents](#contents)

Since remote connector is a [system object](https://backstage.forgerock.com/docs/idcloud-idm/latest/objects-guide/appendix-system-objects.html), you can initiate a scripted action on it. You can define your action under the "systemActions" key in the connector configuration.

> Here, connector configuration is the final JSON sent to the `/openidm/config/provisioner.openicf/<connector-name>` endpoint to register your connector in IDM, as described in [Configure connectors over REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST).

Running a remote script may serve as the means of making a change to or getting a response from the remote system without authorizing to that system or changing its firewall rules.

A scripted action on a remote connector could also be used to change the connector behavior. The script will have access to the libraries available to the connector and to the connector-specific information.

####  <a id="developing-connector-configuration-system-actions-definition" name="developing-connector-configuration-system-actions-definition"></a>Connector Configuration > "systemActions" > Defining System Action

[Back to Contents](#contents)

First, to outline its general structure, example of a system action definition with two individual actions with one returning script bindings and another one solving a math problem:

`provisioner.openicf-<connector-name>.json`
```json
{
    "connectorRef": {
        "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
        [ . . . ]
    },
    [ . . . ]
    "systemActions": [
        {
            "scriptId" : "script-1",
            "actions" : [
                {
                    "systemType" : ".*ScriptedConnector",
                    "actionType" : "groovy",
                    "actionSource" : "binding.variables.toString();"
                },
                {
                    "systemType" : ".*ScriptedConnector",
                    "actionType" : "groovy",
                    "actionSource" : "4 * 4"
                }
            ]
        },
        [ . . . ]
    ]
}
```

Each system action is defined with the following keys:

* <a id="developing-connector-configuration-system-actions-definition-script-id" name="developing-connector-configuration-system-actions-definition-script-id"></a>"scriptId"

    [Back to Contents](#contents)

    The ID you will use in your request to invoke this system action.

* <a id="developing-connector-configuration-system-actions-definition-actions" name="developing-connector-configuration-system-actions-definition-actions"></a>"actions"

    [Back to Contents](#contents)

    For each script ID, you can specify one or more action in an array of action definitions.

    Each action definition consists of the following keys:

    * <a id="developing-connector-configuration-system-actions-definition-actions-system-type" name="developing-connector-configuration-system-actions-definition-actions-system-type"></a>"systemType"

        [Back to Contents](#contents)

        Reference to the connector type for which this action was written. System actions will be performed in the context of the connector type, for which scripting environment will be built. You can populate this key with a regular expression matching the connector type.

        You GET the connector type in "connectorRef.connectorName" in the core connector configuration JSON received from the `/openidm/system?_action=availableConnectors` endpoint.

        If "systemType" does not match the connector type, the action will be ignored when the corresponding system action "scriptId" is requested.

        For example, consider the aforementioned example:

        *  The system type for both actions, ".*ScriptedConnector", matches the connector name, "org.forgerock.openicf.connectors.groovy.ScriptedConnector".

        * Therefore, when "script-1" is requested, both actions will return results:

        ```json
        {
            "actions": [
                {
                    "result": "[arg1:Arg1, param1:Param1, operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1674597149435-ad859d99bf71003de8ae-19623/0/1}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@213027ec, log:org.identityconnectors.common.logging.Log@41b1ffbf]"
                },
                {
                    "result": 4
                }
            ]
        }
        ```

        If you define the same actions for a different connector type, you will need to match it with the "systemType" value:

        ```json
        {
            "connectorRef": {
                "connectorName": "org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConnector",
                [ . . . ]
            },
            [ . . . ]
            "systemActions": [
                {
                    "scriptId" : "script-1",
                    "actions" : [
                        {
                            "systemType" : ".*ScriptedRESTConnector",
                            "actionType" : "groovy",
                            "actionSource" : "binding.variables.toString();"
                        },
                        {
                            "systemType" : ".*ScriptedConnector",
                            "actionType" : "groovy",
                            "actionSource" : "4 * 4"
                        }
                    ]
                },
                [ . . . ]
            ]
        }
        ```

        If you request this system action, identified by "script-1" id, only the matching action will be executed and return result:

        ```json
        {
            "actions": [
                {
                    "result": "[arg1:Arg1, param1:Param1, operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1674597562382-5618dcd582b30aa5ae0d-20316/0/1}, configuration:org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration@27a387dd, connection:org.apache.http.impl.client.InternalHttpClient@21979403, customizedConnection:InnerRESTClient@106ef511, log:org.identityconnectors.common.logging.Log@6124b177]"
                }
            ]
        }
        ```

        Note also that for this connector type, `ScriptedRESTConnector`, there are two additional bindings available for the action script: `connection` and `customizedConnection`.

    * <a id="developing-connector-configuration-system-actions-definition-actions-action-type" name="developing-connector-configuration-system-actions-definition-actions-action-type"></a>"actionType"

        [Back to Contents](#contents)

        A language reference, indicating which language the system action script is written and should be interpreted in.

        For a Java RCS, the action type is always "groovy".

    * <a id="developing-connector-configuration-system-actions-definition-actions-action-source-or-file" name="developing-connector-configuration-system-actions-definition-actions-action-source-or-file"></a>"actionSource" _or_ "actionFile"

        [Back to Contents](#contents)

        The script content can be provided inline, as the value of "actionSource", _or_ in "actionFile" as a relative path reference to a script on the IDM host:

        * "actionFile"

            The reference to a script file stored in the IDM installation folder. In Identity Cloud, you do not have access to the IDM file system.

            This means that currently in Identity Cloud you can customize your system action with the "actionSource" key content and/or by passing in parameters in your system action request in the (REST) request body or as the additional params in an IDM script.

        * "actionSource"

            The actual script content sent to RCS to be executed.

            Because JSON does not support multiline statements, separate your statements in "actionSource" with semicolons or new lines.

            For example:

            ```json
            [ . . . ]
            {
                [ . . . ]
                "actionSource" : "println 'actionSource bindings: '; println binding.variables;"
            },
            {
                [ . . . ]
                "actionSource" : "println 'actionSource bindings: ' \nprintln binding.variables"
            }
            [ . . . ]
            ```

            > You can use a formatting tool where your script content is converted into a JSON-acceptable format; for example, https://www.freeformatter.com/json-escape.html
            >
            > You can consult https://www.json.org/json-en.html on the acceptable JSON syntax.

You can use either of the two ways to invoke a scripted system action on a remote connector:
* [Invoking via REST](#developing-connector-configuration-system-actions-rest)
* [Invoking from IDM Script](#developing-connector-configuration-system-actions-idm-script)

####  <a id="developing-connector-configuration-system-actions-rest" name="developing-connector-configuration-system-actions-rest"></a>Connector Configuration > "systemActions" > Invoking via REST

[Back to Contents](#contents)

You can [run a script on a remote connector](https://backstage.forgerock.com/docs/idcloud-idm/latest/rest-api-reference/endpoints/rest-system-objects.html#script-system-object) by sending following POST request to IDM's REST API:

`/openidm/system/<connector-name>`?`_action=script`&`scriptId=<script_id>`[&`arg1=value1`&`arg2=value2` . . . ]\[&`scriptExecuteMode=resource`]

#####  <a id="developing-connector-configuration-system-actions-rest-parts" name="developing-connector-configuration-system-actions-rest-parts"></a>Connector Configuration > "systemActions" > Invoking via REST > Parts of the Request:

[Back to Contents](#contents)

* <a id="developing-connector-configuration-system-actions-rest-parts-path" name="developing-connector-configuration-system-actions-rest-parts-path"></a>`/openidm/system/<connector-name>`

    [Back to Contents](#contents)

    Path to the IDM's endpoint, at which your remote connection is registered.

    As an example, `/openidm/system/groovy` path in your system action request will correspond to a remote connection registered at `/openidm/config/provisioner.openicf/groovy`, as described in the final step of the [Configure connectors over REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST) doc.

* <a id="developing-connector-configuration-system-actions-rest-parts-action" name="developing-connector-configuration-system-actions-rest-parts-action"></a>`?_action=script`

    [Back to Contents](#contents)

    When executing a script on a remote connector, the `_action` parameter value is always to be `script`.

* <a id="developing-connector-configuration-system-actions-rest-parts-script-id" name="developing-connector-configuration-system-actions-rest-parts-script-id"></a>`&scriptId=<script_id>`

    [Back to Contents](#contents)

    The identifier of the system action you are trying to invoke, which is saved in your connector configuration JSON under the "systemActions.scriptId" key.

    For example, consider the following system action definition:

    `provisioner.openicf-<connector-name>.json`
    ```json
    {
        "connectorRef": {
            "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
            [ . . . ]
        },
        [ . . . ]
        "systemActions": [
            {
                "scriptId" : "script-1",
                "actions" : [
                    {
                        "systemType" : ".*ScriptedConnector",
                        "actionType" : "groovy",
                        "actionSource" : "println 'actionSource bindings: '; println binding.variables;"
                    }
                ]
            },
            [ . . . ]
        ]
    }
    ```

    `IDM Admin Browser Console`
    ```javascript
    (async function () {
        var data = await $.ajax('/openidm/system/groovy?_action=script&scriptId=script-1', {
            method: 'POST'
        });

        console.log(JSON.stringify(data, null, 4));
    }());
    ```

    Because this particular "actionSource" script does not return anything, the response data will bear an empty result from the only action defined and executed for this script ID:

    `Browser Network Response`

    ```json
    {
        "actions": [
            {
                "result": null
            }
        ]
    }
    ```

    > In Groovy, you don't have to explicitly return anything from a script; the result of the last statement is returned automatically.
    >
    > However, in the provided example, the last statement is [println](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/PrintWriter.html#println(java.lang.String)), which returns nothing; hence, the `null` result in the response.

    To return something in the response, end your script with a statement returning "serializable" value. A value is serializable if it can be converted into a valid [JSON text](https://www.rfc-editor.org/rfc/rfc7159.html).

    In a Java RCS, instances of the following classes (and the corresponding primitives) are serializable and could be returned from your script:

    * [Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html) (and `boolean`)
    * [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html) (and `int`)
    * [Long](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html) (and `long`)
    * [Float](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Float.html) (and `float`)
    * [Double](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Double.html) (and `double`)
    * [BigDecimal](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/math/BigDecimal.html)
    * [BigInteger](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/math/BigInteger.html)
    * [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)
    * [Character](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Character.html) (and `char`)
    * [Byte](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Byte.html) (and `byte` and `byte[]`)
    * [URI](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/URI.html)
    * [File](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/File.html)
    * [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)
    * [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)
    * [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html) (but not [SortedMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/SortedMap.html))
    * [Map.Entry](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.Entry.html)
    * [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html) (but not [SortedSet](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/SortedSet.html))
    * [Locale](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Locale.html)
    * [GuardedString](https://docs.oracle.com/en/middleware/idm/identity-governance/12.2.1.3/omicf/index.html?org/identityconnectors/common/security/GuardedString.html)
    * [GuardedByteArray](https://docs.oracle.com/en/middleware/idm/identity-governance/12.2.1.3/omicf/index.html?org/identityconnectors/common/security/GuardedByteArray.html)

    > There are also a number of built-in ICF-specific Handlers and Mappings that can consume and return serialized values.

    Generally, if you try to return a non-serializable value from your script you will receive an error message in the response similar to the following:

    ```json
    {
        "actions": [
            {
                "error": "No serializer for class: class java.util.HashMap$Node"
            }
        ]
    }
    ```

    or

    ```json
    {
        "actions": [
            {
                "error": "No serializer for class: class groovy.lang.Script"
            }
        ]
    }
    ```

    As an example of a serializable value, you could return a list (as the result of an arbitrary operation in remotely executed script):

    `provisioner.openicf-<connector-name>.json`
    ```json
    {
        "connectorRef": {
            "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
            [ . . . ]
        },
        [ . . . ]
        "systemActions": [
            {
                "scriptId" : "script-1",
                "actions" : [
                    {
                        "systemType" : ".*ScriptedConnector",
                        "actionType" : "groovy",
                        "actionSource" : "println 'actionSource bindings: '; println binding.variables; [1, 2, 3];"
                    }
                ]
            },
            [ . . . ]
        ]
    }
    ```

    In the response, you will now see an array of results—one "result" for each action defined for the system action:

    `Browser Network Response`

    ```json
    {
        "actions": [
            {
                "result": [
                    1,
                    2,
                    3
                ]
            }
        ]
    }
    ```

    At the same time, this "actionSource" script will output its variable bindings in the RCS logs:

    ```
    [rcs] actionSource bindings:
    [rcs] [operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1659985544219-55e3d75b5a1adc2a72f9-134922/0/4}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@73133988, log:org.identityconnectors.common.logging.Log@2cba672e]
    ```

    Note that the `operation` binding value reveals the [script on connector operation](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/operations/operation-script-on-connector.html) environment, which is the default mode for a remote script execution.

    In this mode,  the script specified in "actionSource" (or "actionFile") is executed in a context built for a [Run on connector script](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/scripts/script-run-on-connector.html), with the corresponding variable bindings.

    You can read about common RCS script bindings in [Variables available to all Groovy scripts](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/groovy-operations.html#groovy-script-variables), and find more specific information in the sections designated to a particular script operation.

* <a id="developing-connector-configuration-system-actions-rest-parts-request-params" name="developing-connector-configuration-system-actions-rest-parts-request-params"></a>`&arg1=value1&arg2=value2 . . . `

    [Back to Contents](#contents)

    Besides `scriptId`, you can pass additional arbitrary arguments in the query string. These will become variables in the scripting context.

    It might be more efficient, however, to define you script parameters in the request body.

* <a id="developing-connector-configuration-system-actions-rest-parts-request-body" name="developing-connector-configuration-system-actions-rest-parts-request-body"></a>`request body`

    [Back to Contents](#contents)

    The key/value pairs provided as JSON in the request body will be available to the remotely executed script as variables.

    For example, you could execute the following request:

    `IDM Admin Browser Console`

    ```javascript
    (async function () {
        var data = await $.ajax('/openidm/system/groovy?_action=script&scriptId=script-1', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            data: JSON.stringify({
                arg1: 'value1'
            })
        });

        console.log(JSON.stringify(data, null, 4));
    }());
    ```

    In the RCS logs, in the bindings available to your action script, you will now see `arg1`, which was a key in your request body JSON:

    ```
    [rcs] actionSource bindings:
    [rcs] [arg1:value1, operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1659985960800-d72d565f715c26629c97-65231/0/4}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@73133988, log:org.identityconnectors.common.logging.Log@2cba672e]
    ```

    Different types of values in the request body JSON will be presented in the script as the following types:

    * `object` > [java.util.HashMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/HashMap.html)
    * `number` > [java.lang.Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html) or [java.lang.Double](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Double.html)
    * `array` > [java.util.ArrayList](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ArrayList.html)
    * `null` > [org.codehaus.groovy.runtime.NullObject](https://docs.groovy-lang.org/docs/latest/html/api/org/codehaus/groovy/runtime/NullObject.html)
    * `boolean` > [java.lang.Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)
    * `string` >  [java.lang.String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)
    * `undefined` > Will NOT be present.

    For example, providing this request body:

    ```js
    data: JSON.stringify({
        arg1: 'value1',
        arg2: {
            arg3: 1,
            arg4: 1.1,
            arg5: [
                'value3',
                'value4'
            ],
            arg6: true,
            arg7: null,
            arg8: undefined
        }
    })
    ```

    will yield the following output in the RCS logs:

    ```
    [arg2:[arg3:1, arg5:[value3, value4], arg4:1.1, arg7:null, arg6:true], arg1:value1, operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1661235801641-96bc95f49bbf8e27d3e4-54089/0/4}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@26f4d226, log:org.identityconnectors.common.logging.Log@1726fedb]
    ```


* <a id="developing-connector-configuration-system-actions-rest-parts-execute-mode" name="developing-connector-configuration-system-actions-rest-parts-execute-mode"></a>`&scriptExecuteMode=resource` (optional and IMPORTANT)

    [Back to Contents](#contents)

    The absence or presence of this parameter in a system action request will determine one of the two execution modes for the scripted system action:

    * By default, _without_ this optional parameter populated with this particular value, the script you specify in "actionSource" or "actionFile" script will ["run on connector"](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/scripts/script-run-on-connector.html).

        In this mode, the script you specify in "actionSource" or "actionFile" will be sent to the RCS, where your scripted connector package is deployed. The ICF framework will execute the script in the connector type-specific context, with all the variable bindings available in the script.

        This has been the mode illustrated in all previous examples.

    * Including `&scriptExecuteMode=resource` in a system action request will cause the remote script to ["run on resource"](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/scripts/script-run-on-resource.html).

        In this mode, a script hosted on the RCS will be executed. The content provided either in "actionSource" or via "actionFile" will be available as the `scriptText` variable to the hosted script.

        Exactly which script hosted on RCS is going to be executed is specified in the connector configuration under the "scriptOnResourceScriptFileName" key.

        For example:

        `provisioner.openicf-<connector-name>.json`
        ```json
        {
            "connectorRef": {
                "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
                [ . . . ]
            },
            "configurationProperties": {
                "scriptExtensions": [
                    "groovy"
                ],
                "scriptRoots": [
                    "/opt/openicf/scripts/<connector-name>"
                ],
                "scriptOnResourceScriptFileName": "ScriptOnResourceScript.groovy",
                [ . . . ]
            },
            [ . . . ]
            "systemActions": [
                {
                    "scriptId" : "script-1",
                    "actions" : [
                        {
                            "systemType" : ".*ScriptedConnector",
                            "actionType" : "groovy",
                            "actionSource" : "println 'actionSource bindings: '; println binding.variables; [1, 2, 3];"
                        }
                    ]
                },
                [ . . . ]
            ]
        }
        ```

        Then, in the script referenced in "scriptOnResourceScriptFileName", you can get the "actionSource" (or "actionFile") script content from the `scriptText` binding.

        For example:

        `ScriptOnResourceScript.groovy`

        ```groovy
        [ . . . ]
        println 'ScriptOnResourceScript.groovy bindings: ' + binding.variables
        ```

        `IDM Admin Browser Console`

        ```javascript
        (async function () {
            var data = await $.ajax('/openidm/system/groovy?_action=script&scriptId=script-1&scriptExecuteMode=resource', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                data: JSON.stringify({
                    arg1: 'value1'
                })
            });

            console.log(JSON.stringify(data, null, 4));
        }());
        ```

        `RCS logs`

        ```
        [rcs] ScriptOnResourceScript.groovy bindings: [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1659989514919-55e3d75b5a1adc2a72f9-142363/0/5}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@21fa1b88, log:org.identityconnectors.common.logging.Log@3f3e5b1c]
        ```

        Note that:

        * The `operation` type is "RUNSCRIPTONRESOURCE", which corresponds to the [script on resource operation](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/operations/operation-script-on-resource.html).

        * The script you provided in "actionSource" is available to `ScriptOnResourceScript.groovy` as the `scriptText` binding.

        * The request data is saved in the `scriptArguments` binding (which is an unmodifiable map).

        * The `scriptText` content is for a Groovy environment, as indicated in the `scriptLanguage` binding.

        Note also that, this particular `ScriptOnResourceScript.groovy` does not return anything; and thus, in the browser response you will see no results:

        `Browser Network Response`

        ```json
        {
            "actions": [
                {
                    "result": null
                }
            ]
        }
        ```

        The script you see in the `scriptText` variable is NOT executed automatically by RCS. `ScriptOnResourceScript.groovy` can ignore this binding or use its text value in any desirable way. For example, it could evaluate it as a script, and return its result:

        `ScriptOnResourceScript.groovy`

        ```groovy
        [ . . . ]

        new GroovyShell().evaluate(scriptText)
        ```

        This time, in addition to the (same) bindings printed out from `ScriptOnResourceScript.groovy`, the script provided in "actionSource" will execute as well and log out its available variables:

        `RCS logs`

        ```
        [rcs] ScriptOnResourceScript.groovy bindings: [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1661542196240-3c9381d89956a1ca3441-148731/0/4}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@308df108, log:org.identityconnectors.common.logging.Log@12972750]

        [ . . . ]

        [rcs] actionSource bindings:
        [rcs] [:]
        ```

        The empty map representing "actionSource" bindings means that no outside-defined variable bindings have been made available to the "actionSource" script. This is because no arguments were passed to the [groovy.lang.GroovyShell](https://docs.groovy-lang.org/latest/html/api/groovy/lang/GroovyShell.html) constructor, which created the instance that evaluated the "actionSource" script content.

        You can supply the "actionSource" script with the very same bindings defined for `ScriptOnResourceScript.groovy` by passing them in the `groovy.lang.GroovyShell` constructor:

        `ScriptOnResourceScript.groovy`

        ```groovy
        [ . . . ]

        new GroovyShell(binding).evaluate(scriptText)
        ```

        `RCS logs`

        ```
        [rcs] ScriptOnResourceScript.groovy bindings: [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1661543987519-3c9381d89956a1ca3441-151976/0/5}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@7a551e98, log:org.identityconnectors.common.logging.Log@26179ea7]

        [ . . . ]

        [rcs] actionSource bindings:
        [rcs] [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1661543987519-3c9381d89956a1ca3441-151976/0/5}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@7a551e98, log:org.identityconnectors.common.logging.Log@26179ea7]
        ```

        Alternatively, you could explicitly provide "actionSource"-specific bindings:

        `ScriptOnResourceScript.groovy`

        ```groovy
        [ . . . ]

        // Do some processing.
        def result = 1 + 1;

        Binding binding = new Binding();
        binding.setVariable('args', scriptArguments)
        binding.setVariable('result', result)

        new GroovyShell(binding).evaluate(scriptText)
        ```

        Now, the action script bindings will be limited to the variables explicitly set in `ScriptOnResourceScript.groovy`:

        `RCS logs`

        ```
        [rcs] ScriptOnResourceScript.groovy bindings: [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1661545941051-690832f475140dd87466-16728/0/4}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@308df108, log:org.identityconnectors.common.logging.Log@12972750]

        [ . . . ]

        [rcs] actionSource bindings:
        [rcs] [args:[arg1:value1], result:2]
        ```

        In the last few examples, `ScriptOnResourceScript.groovy` ends with the `GroovyShell` call; and thus, the browser response will contain the results of evaluating the "actionSource" script:

        `Browser Network Response`

        ```json
        {
            "actions": [
                {
                    "result": [
                        1,
                        2,
                        3
                    ]
                }
            ]
        }
        ```

        You can customize the script "on resource" behavior in the following ways:

        * With the additional parameters (passed in the request body) available to the script as the `scriptArguments` binding.

        * Via the "actionSource" content (and/or in a self-managed environment, via the "actionFile" reference) for each defined and performed action in a system action.

        The script content in the latter case could be any text. It could be evaluated by the script "on resource", or used in a conditional statement, or applied in any other similar way. It could also be a reference to a local (to RCS) and completely separate script file.

        For example, let's add the following system action with the "script-2" ID to the connector configuration:

        `provisioner.openicf-<connector-name>.json`
        ```json
        {
            "connectorRef": {
                "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
                [ . . . ]
            },
            "configurationProperties": {
                "scriptExtensions": [
                    "groovy"
                ],
                "scriptRoots": [
                    "/opt/openicf/scripts/<connector-name>"
                ],
                "scriptOnResourceScriptFileName": "ScriptOnResourceScript.groovy",
                [ . . . ]
            },
            [ . . . ]
            "systemActions": [
                {
                    "scriptId" : "script-1",
                    "actions" : [
                        {
                            "systemType" : ".*ScriptedConnector",
                            "actionType" : "groovy",
                            "actionSource" : "println 'actionSource bindings: '; println binding.variables; [1, 2, 3];"
                        }
                    ]
                },
                {
                    "scriptId" : "script-2",
                    "actions" : [
                        {
                            "systemType" : ".*ScriptedConnector",
                            "actionType" : "groovy",
                            "actionSource" : "/opt/openicf/scripts/groovy/ScriptOnResourceScript.script-2.action-0.groovy"
                        },
                        {
                            "systemType" : ".*ScriptedConnector",
                            "actionType" : "groovy",
                            "actionSource" : "/opt/openicf/scripts/groovy/ScriptOnResourceScript.script-2.action-1.groovy"
                        }
                    ]
                },
                [ . . . ]
            ]
        }
        ```

        The system action defined under the "script-2" ID has two actions, each containing a path to a local file in its "actionSource".

        Each of the referenced files will have a very simple Groovy script returning an object—a list and a map respectively:

        `ScriptOnResourceScript.script-2.action-0.groovy`

        ```groovy
        [1, 2]
        ```

        `ScriptOnResourceScript.script-2.action-1.groovy`

        ```groovy
        [ a: 'a', b: 'b']
        ```

        Now, the `ScriptOnResourceScript.groovy` script can evaluate the content of each action file—_one_ action at the time:

        `ScriptOnResourceScript.groovy`

        ```groovy
        [ . . . ]

        try {
            def scriptFileText = new File(scriptText).text
            new GroovyShell().evaluate(scriptFileText)
        } catch (e) {
            log.error e.message
        }
        ```

        Change your browser console JavaScript to request the system action identified as "script-2":

        `IDM Admin Browser Console`

        ```javascript
        (async function () {
            var data = await $.ajax('/openidm/system/groovy?_action=script&scriptId=script-2&scriptExecuteMode=resource', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                data: JSON.stringify({
                    arg1: 'value1'
                })
            });

            console.log(JSON.stringify(data, null, 4));
        }());
        ```

        In the response, you should now see the data returned for each action:

        ```json
        {
            "actions": [
                {
                    "result": [
                        1,
                        2
                    ]
                },
                {
                    "result": {
                        "a": "a",
                        "b": "b"
                    }
                }
            ]
        }
        ```

        In this way, "actionSource" could be a great tool for referencing local scripts that you can maintain as separate files under your RCS installation.

        > Note that in such case, the content of "actionSource" is NOT a script. If you attempt to evaluate it directly, you will get an error.
        >
        > For example, if you try to request the very same system action to "run on connector" (notice no `&scriptExecuteMode=resource` parameter):
        >
        > `IDM Admin Browser Console`
        >
        > ```javascript
        > (async function () {
        >     var data = await $.ajax('/openidm/system/groovy?_action=script&scriptId=script-2', {
        >         method: 'POST',
        >         headers: {
        >             'Content-Type': 'application/json'
        >         },
        >         data: JSON.stringify({
        >             arg1: 'value1'
        >         })
        >     });
        >
        >     console.log(JSON.stringify(data, null, 4));
        > }());
        > ```
        >
        > RCS will try to evaluate the "actionSource" content as a script, and you will see the following errors reported in the browser response:
        >
        > ```json
        > {
        >     "actions": [
        >         {
        >             "error": "No such property: openicf for class: Script1661393119839"
        >         },
        >         {
        >             "error": "No such property: openicf for class: Script1661393119971"
        >         }
        >     ]
        > }
        > ```

#####  <a id="developing-connector-configuration-system-actions-rest-execute-modes" name="developing-connector-configuration-system-actions-rest-execute-modes"></a>Connector Configuration > "systemActions" > Invoking via REST > "run on resource" vs "run on connector"

[Back to Contents](#contents)

Whether the script you send to RCS is run "on connector" or "on resource", it is executed in the connector environment, on the connector server, where the connector package is deployed.

But the way your script content is used is different:

* For a "run on connector" operation, the ICF framework executes the provided script, and the script has direct access to the connector type-specific context.

* For a "run on resource" operation, the "actionFile" or "actionSource" script content is given as the `scriptText` variable to a script already hosted on RCS. The hosted-on-RCS script will be executed by ICF, and will perform its actions in the connector context. Optionally, the hosted script could evaluate the provided script text, or use it in any other way, or simply ignore the `scriptText` binding.

In a managed environment such as the Identity Cloud, you cannot host a custom script file and reference it in "actionFile". Your system action-specific script content is limited to "actionSource". Arguably, in this case, a more practical approach to perform a custom system action of any considerable complexity is executing a "run on resource" script, which will enable you to maintain it in a separate file.

The "run on resource" script execution mode could also be more efficient than the "run on connector" one because:

* The hosted script is loaded, compiled, and cached once, when the connector is activated, while the "actionSource" or "actionFile" script is evaluated every time a "run on connector" action is called.

* The hosted script doesn't have to be transmitted from IDM to RCS, while the "actionSource" or "actionFile" content does.

####  <a id="developing-connector-configuration-system-actions-idm-script" name="developing-connector-configuration-system-actions-idm-script"></a>Connector Configuration > "systemActions" > Invoking from IDM Script

[Back to Contents](#contents)

You can invoke a system action from an [IDM script](https://backstage.forgerock.com/docs/idcloud-idm/latest/scripting-guide/scripting-func-ref.html) by calling `openidm.action(resource, actionName, content, params, fields)` for `Actions supported on system resources (system/*)`.

#####  <a id="developing-connector-configuration-system-actions-idm-script-syntax" name="developing-connector-configuration-system-actions-idm-script-syntax"></a>Connector Configuration > "systemActions" > Invoking from IDM Script > Syntax

[Back to Contents](#contents)

Except for the actual syntax, all the information used for invoking a system action via IDM's REST applies here. The `openidm.action(resource, actionName, content, params, fields)` arguments map to the parts of a system action REST request in the following way:

* `resource` corresponds to the [/openidm/system/\<connector-name\>](#developing-connector-configuration-system-actions-rest-parts-path) part of the path.

* `actionName` corresponds to the [_action=script](#developing-connector-configuration-system-actions-rest-parts-action) ("execute script" action) URL parameter and is always populated with 'script'.

* `content` is an object that in the REST request is described in the [request body](#developing-connector-configuration-system-actions-rest-parts-request-body) JSON.

* `params` is an object containing additional arguments, such as [scriptId=\<script_id\>](#developing-connector-configuration-system-actions-rest-parts-script-id), [scriptExecuteMode=resource](#developing-connector-configuration-system-actions-rest-parts-execute-mode), and any [additional arguments](#developing-connector-configuration-system-actions-rest-parts-request-params) included in the query string of the REST request.

* `fields` can be omitted.

#####  <a id="developing-connector-configuration-system-actions-idm-script-examples" name="developing-connector-configuration-system-actions-idm-script-examples"></a>Connector Configuration > "systemActions" > Invoking from IDM Script > Examples

[Back to Contents](#contents)

For comparison, let's assume the same connector configuration as in the preceding REST examples:

`provisioner.openicf-<connector-name>.json`
```json
{
    "connectorRef": {
        "connectorName": "org.forgerock.openicf.connectors.groovy.ScriptedConnector",
        [ . . . ]
    },
    [ . . . ]
    "configurationProperties": {
        [ . . . ]
        "scriptExtensions": [
            "groovy"
        ],
        "scriptRoots": [
            "/opt/openicf/scripts/<connector-name>"
        ],
        "scriptOnResourceScriptFileName": "ScriptOnResourceScript.groovy",
        [ . . . ]
    },
    [ . . . ]
    "systemActions": [
        {
            "scriptId" : "script-1",
            "actions" : [
                {
                    "systemType" : ".*ScriptedConnector",
                    "actionType" : "groovy",
                    "actionSource" : "println 'actionSource bindings: '; println binding.variables; [1, 2, 3];"
                }
            ]
        },
        [ . . . ]
    ]
}
```

Then:

* <a id="developing-connector-configuration-system-actions-idm-script-examples-on-connector" name="developing-connector-configuration-system-actions-idm-script-examples-on-connector"></a>"run on connector"

    [Back to Contents](#contents)

    A call to _"run on connector"_ operation in an IDM script could look like the following:

    `IDM Admin > Managed object > alpha_user > onRead Inline Script (text/javascript)`

    ```javascript
    try {
        const response = openidm.action(
            'system/groovy',
            'script',
            {
                arg1: 'value1'
            },
            {
                scriptId: 'script-1'
            }
        );

        logger.error(String(response));
    } catch (e) {
        logger.error(e.message);
    }
    ```

    In IDM logs, you will see the IDM script's output from the `logger.error(String(response));` statement; the logs will contain the JSON returned by the system action:

    `IDM logs`

    ```
    "SEVERE: { \"actions\": [ { \"result\": [ 1, 2, 3 ] } ] }"
    ```

    In RCS logs you will see the bindings available for the "run on connector" script:

    `RCS logs`

    ```
    [rcs] actionSource bindings:
    [rcs] [arg1:value1, operation:RUNSCRIPTONCONNECTOR, options:OperationOptions: {CAUD_TRANSACTION_ID:1660248281913-d3e36ed8cab1c078539d-31401/0/5}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@28200dd8, log:org.identityconnectors.common.logging.Log@4d49370f]
    ```

* <a id="developing-connector-configuration-system-actions-idm-script-examples-on-resource" name="developing-connector-configuration-system-actions-idm-script-examples-on-resource"></a>"run on resource"

    [Back to Contents](#contents)

    A call to "run script on resource" could look like the following in an IDM script:

    `IDM Admin > Managed object > alpha_user > onRead Inline Script (text/javascript)`
    ```javascript
    try {
        const response = openidm.action(
            'system/groovy',
            'script',
            {
                arg1: 'value1'
            },
            {
                scriptId: 'script-1',
                scriptExecuteMode: 'resource'
            }
        );

        logger.error(String(response));
    } catch (e) {
        logger.error(e.message);
    }
    ```

    Note the additional `scriptExecuteMode: 'resource'` parameter.

    Let's assume the following "script on resource" content:

    `ScriptOnResourceScript.groovy`

    ```groovy
    [ . . . ]

    Binding binding = new Binding();
    binding.setVariable('args', scriptArguments)

    new GroovyShell(binding).evaluate(scriptText)
    ```

    In the IDM logs, you will still see JSON returned from the system action (to the IDM script):

    `IDM logs`

    ```
    "SEVERE: { \"actions\": [ { \"result\": [ 1, 2, 3 ] } ] }"
    ```

    The RCS logs, however, will now include output of the bindings available for both scripts, the hosted on RCS `ScriptOnResourceScript.groovy` and the script sent to RCS in "actionSource", as both are going to be executed:

    `RCS logs`

    ```
    [rcs] ScriptOnResourceScript.groovy bindings: [scriptArguments:[arg1:value1], scriptText:println 'actionSource bindings: '; println binding.variables; [1, 2, 3];, scriptLanguage:groovy, operation:RUNSCRIPTONRESOURCE, options:OperationOptions: {CAUD_TRANSACTION_ID:1660248944505-d3e36ed8cab1c078539d-32885/0/5}, configuration:org.forgerock.openicf.connectors.groovy.ScriptedConfiguration@4ebedbe8, log:org.identityconnectors.common.logging.Log@4f41fd7e]

    [ . . . ]

    [rcs] actionSource bindings:
    [rcs] [args:[arg1:value1]]
    ```

    Once again, all considerations you might have for invoking a system action with a REST call, except the actual syntax, apply to invoking a system action from an IDM script.

### <a id="developing-connector-configuration-system-actions-support" name="developing-connector-configuration-system-actions-support"></a>Connector Configuration > "systemActions" > Support in Connectors

[Back to Contents](#contents)

The "systemAction" key and its content are only accepted and supported by connectors that implement [Script on connector operation](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/operations/operation-script-on-connector.html) and [Script on resource operation](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/operations/operation-script-on-resource.html).

At the time of writing, the following (Java) connectors have implemented both:

* [Kerberos](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/kerberos.html)

* [Marketo](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/marketo.html)

* [MongoDB](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/mongodb.html)

* [Groovy Connector Toolkit](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/groovy.html)

* [Scripted REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/scripted-rest.html)

* [Scripted SQL](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/scripted-sql.html)

* [SSH](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/ssh.html)

In addition, the [Salesforce](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/salesforce.html) connector has only "script on connector operation" implemented.

> Although unrelated to Java RCS, [Scripted connectors with PowerShell](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-dev-guide/powershell.html) support script on connector operation as well.

## <a id="example-connectors" name="example-connectors"></a>Example Connectors

[Back to Contents](#contents)

### <a id="example-connectors-scripted-sql" name="example-connectors-scripted-sql"></a>Example Connectors > Scripted SQL

[Back to Contents](#contents)

[README](./connectors/postgres/README.md) for a PostgreSQL connector configuration example.
