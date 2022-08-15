# Developing Scripted Connectors for Java Remote Connector Server (RCS)

## <a id="contents" name="contents"></a>Contents

* [Choosing an IDE](#developing-ide)
* [Debugging Scripts](#developing-debugging-scripts)
    * [Custom Logs](#developing-debugging-scripts-custom-logs)
    * [Try and Catch](#developing-debugging-scripts-try-catch)
* [Scripting Context](#developing-connector-context)
* [Connector Configuration](#developing-connector-configuration)
    * ["configurationProperties"](#developing-connector-configuration-configuration-properties)
* [Bindings](#developing-bindings)
* [Example Connectors](#example-connectors)
    * [Scripted SQL Connector](#example-connectors-scripted-sql)

## <a id="developing-ide" name="developing-ide"></a>Choosing IDE

[Back to Contents](#contents)

For a Java RCS, you will write scripts in [the Apache Groovy programming language](https://groovy-lang.org/) (Groovy). Consult the [IDE integration](https://groovy-lang.org/ides.html) support for Groovy when you choose your IDE for RCS script development.

In general, you can get a better support for Groovy in a Java-specialized IDE, like [IntelliJ IDEA](https://www.jetbrains.com/idea/) (IntelliJ).

In a non-Java or in a polyglottal IDE, you might be able to effectively maintain your RCS scripts, but Groovy support may not be provided or be limited in comparison to IntelliJ.

> For example, as of this writing, no Groovy debugger extension is available for Visual Code Studio—a very popular code editor. This means that, if you want to do remote debugging and attach a debugger to your RCS process, you will have to use something like IntelliJ.

## <a id="developing-debugging-scripts" name="developing-debugging-scripts"></a>Debugging Scripts

[Back to Contents](#contents)

### <a id="developing-debugging-scripts-custom-logs" name="developing-debugging-scripts-custom-logs"></a>Debugging Scripts > Custom Logs

[Back to Contents](#contents)

You can use methods of the [Log](https://backstage.forgerock.com/docs/idcloud-idm/latest/_attachments/apidocs/org/identityconnectors/common/logging/Log.html) class to output custom logs from your connector scripts. For example:

```groovy
import org.identityconnectors.common.logging.Log

def log = log as Log
def operation = operation as OperationType

log.info 'This is ' + operation + ' script'
```

```
[rcs] Jul 20, 2022 12:41:12 AM INFO  TestScript: This is TEST script
```

> Using methods `Log` to output an object information without referencing its individual properties or keys might require converting the object to a strings. Otherwise, you could get a wordy error in the output:
>
> ```groovy
> log.info operation
> ```
>
> ```
> groovy.lang.MissingMethodException: No signature of method: org.identityconnectors.common.logging.Log.info() is applicable for argument types: (org.forgerock.openicf.connectors.groovy.OperationType) values: [TEST]
> ```

Use the `Log` class for the logs output that is to stay in the code.

During the development phase, for a quick temporary output, you could use the standard `println`. This will allow to print out content of objects and strings without the additional information about date and time and log level, which might not bear a lot of value during script development:

```groovy
println operation
```


```
[rcs] TEST
```

Also, you don't have to convert your object to a string explicitly before sending it to `println` because the latter will utilize `toString()` method available in all Java objects.

### <a id="developing-debugging-scripts-try-catch" name="developing-debugging-scripts-try-catch"></a>Debugging Scripts > Try and Catch

[Back to Contents](#contents)

Generally, you should wrap your code with a `try/catch` block, and observe custom error messages in the logs output. For example:

```groovy
import org.identityconnectors.common.logging.Log
[ . . . ]

def log = log as Log

try {
    def operation = operation as OperationType
    [ . . . ]
    switch (objectClass) {
        [ . . . ]
    }
} catch (Exception e) {
    def message = "${operation.name()} operation of type: ${objectClass.objectClassValue} is not supported."

    log.error message
    log.error "Exception: ${e.getMessage()}."

    throw new UnsupportedOperationException(message)
}
```

> [UnsupportedOperationException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/UnsupportedOperationException.html) is a Java exception, which, among other most commonly used Java classes, is [automatically provided in Groovy scripts](https://groovy-lang.org/structure.html#_default_imports).

A script error handled in this way will result in a message displayed on the connector data page in IDM Admin:

<img alt="Error Message in IDM Admin Connector Screen, which reads—SEARCH operation of type: organization is not supported." src="README_files/rcs.connector.search-script.handled-exception.png" width="1024">

Passing the exception information and/or some custom messages to the logs output could provide helpful content for debugging. For example:

```bash
[rcs] Jun 22, 2022 2:35:11 AM ERROR SearchScript: SEARCH operation of type: organization is not supported.
[rcs] Jun 22, 2022 2:35:11 AM ERROR SearchScript: Exception: ERROR: relation "organisations" does not exist%0A  Position: 15.
```

##  <a id="developing-connector-context" name="developing-connector-context"></a>On Scripting Context

[Back to Contents](#contents)

The bindings in a connector script will depend on the connector and script types. There are, however, some common properties available in any connector script that are worth of mentioning.

* `configuration.propertyBag`

    If you need to keep a global reference accessible in the scripts, you can save it in the `configuration.propertyBag` binding.

    For example, you can populate the `propertyBag` property manually in a script. Once set, it can serve as a global variable accessible in the other scripts at runtime. For example:

    ```groovy
    configuration.propertyBag.myCustomProperties = new LinkedHashMap()
    configuration.propertyBag.myCustomProperties.add('key': 'value')
    ```

    You can provide initial content for `propertyBag` in the connector configuration via "configurationProperties.customConfiguration" when you [configure your scripted connector over REST](https://backstage.forgerock.com/docs/idcloud/latest/solution-scripted-rest-connector.html).

    See the [Connector Configuration > Custom (Sensitive) Configuration](#developing-connector-configuration-custom-configuration) for additional details.


##  <a id="developing-connector-configuration" name="developing-connector-configuration"></a>Connector Configuration

[Back to Contents](#contents)

The docs provide general steps of [Configuring connectors over REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST). Configuration properties for the remote connector types available in Identity Cloud can be found under [Connector reference](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/preface.html) > Remote Connectors.

In this section, we will go over some connector configuration settings, using of which may benefit from additional details.

###  <a id="developing-connector-configuration-configuration-properties" name="developing-connector-configuration-configuration-properties"></a>Connector Configuration > "configurationProperties"

[Back to Contents](#contents)

The "configurationProperties" key in connector configuration contains settings that are specific to the target system.

## <a id="developing-bindings" name="developing-bindings"></a>Bindings

[Back to Contents](#contents)

## <a id="example-connectors" name="example-connectors"></a>Example Connectors

[Back to Contents](#contents)

### <a id="example-connectors-scripted-sql" name="example-connectors-scripted-sql"></a>Example Connectors > Scripted SQL

[Back to Contents](#contents)

[README](./connectors/postgres/README.md) for a PostgreSQL connector configuration example.
