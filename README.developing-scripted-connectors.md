# Developing Scripted Connectors for Java Remote Connector Server (RCS)

## <a id="contents" name="contents"></a>Contents

* [Choosing an IDE](#developing-ide)
* [Debugging Scripts](#developing-debugging-scripts)
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
