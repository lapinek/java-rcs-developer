# Developing Scripted Connectors for Java Remote Connector Server (RCS)

## <a id="contents" name="contents"></a>Contents

* [Choosing an IDE](#developing-ide)
* [Debugging Scripts](#developing-debugging-scripts)
    * [Try and Catch](#developing-debugging-scripts-try-catch)
    * [Custom Logs](#developing-debugging-scripts-custom-logs)
* [Connector Configuration](#developing-connector-configuration)
    * ["configurationProperties"](#developing-connector-configuration-configuration-properties)
* [Example Connectors](#example-connectors)
    * [Scripted SQL Connector](#example-connectors-scripted-sql)

## <a id="developing-ide" name="developing-ide"></a>Choosing IDE

[Back to Contents](#contents)

For a Java RCS, you will write scripts in [the Apache Groovy programming language](https://groovy-lang.org/) (Groovy). Consult the [IDE integration support for Groovy](https://groovy-lang.org/ides.html) when you choose your IDE for RCS script development.

In general, you can get a better support for Groovy in a Java-specialized IDE, like [IntelliJ IDEA](https://www.jetbrains.com/idea/) (IntelliJ).

In a non-Java or in a polyglottal IDE, you might be able to effectively maintain your RCS scripts, but Groovy-related features may not be readily available or have limited functionality and support (in comparison to IntelliJ).

> For example, as of this writing, no Groovy debugger extension is available for Visual Code Studio—a very popular code editor. This means that, if you want to do remote debugging and attach a debugger to your RCS process, you will have to use something like IntelliJ.

## <a id="developing-debugging-scripts" name="developing-debugging-scripts"></a>Debugging Scripts

[Back to Contents](#contents)

### <a id="developing-debugging-scripts-try-catch" name="developing-debugging-scripts-try-catch"></a>Debugging Scripts > Try and Catch

[Back to Contents](#contents)

If an unhandled error occurs in your RCS scripts, depending on the script, it may result in a malformed or blank screen in IDM admin UI, an unnecessarily detailed error message sent to the client side, and no debug information in the RCS logs.

Therefore, you should wrap your code with a `try/catch` block, send custom error messages to the logs output, and, potentially, throw a custom exception.

For example (where `[ . . . ]` denotes omission from the original source):

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

##  <a id="developing-connector-configuration" name="developing-connector-configuration"></a>Connector Configuration

[Back to Contents](#contents)

The docs outline general steps of [Configuring connectors over REST](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/configure-connector.html#connector-wiz-REST). Configuration properties for different remote connector types (that are available in Identity Cloud) can be found under [Connector reference](https://backstage.forgerock.com/docs/idcloud-idm/latest/connector-reference/preface.html) > Remote Connectors.

This section will elaborate on some additional details, not currently presented in the docs.

###  <a id="developing-connector-configuration-configuration-properties" name="developing-connector-configuration-configuration-properties"></a>Connector Configuration > "configurationProperties"

[Back to Contents](#contents)

The "configurationProperties" key in connector configuration contains settings that are specific to the target system.

## <a id="example-connectors" name="example-connectors"></a>Example Connectors

[Back to Contents](#contents)

### <a id="example-connectors-scripted-sql" name="example-connectors-scripted-sql"></a>Example Connectors > Scripted SQL

[Back to Contents](#contents)

[README](./connectors/postgres/README.md) for a PostgreSQL connector configuration example.
