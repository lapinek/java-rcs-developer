# Configuring Scripted Connector over REST

## Scripted SQL Connector Configuration

```javascript
// Configure Connectors Over REST
(async function () {
    var settings;

    // List the available connectors.
    settings = {
        headers: {
            'Accept-API-Version': 'resource=1.0'
        },
        method: 'POST',
        url: '/openidm/system?_action=availableConnectors'
    };
    var connectorRef = await $.ajax(settings).then((response) => {
        console.log(response);
        var connectorRef = response.connectorRef.find((connectorRef) => {
            return connectorRef.connectorName === 'org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector';
        });

        return connectorRef;
    });
    console.log('connectorRef', connectorRef)

    // Generate a core configuration.
    settings = {
        headers: {
            'Accept-API-Version': 'resource=1.0',
            'Content-Type': 'application/json'
        },
        method: 'POST',
        url: '/openidm/system?_action=createCoreConfig',
        data: JSON.stringify({
            connectorRef: connectorRef
        })
    };
    var coreConfig = await $.ajax(settings).then((response) => {
        return response;
    });
    console.log('coreConfig', coreConfig);

    /**
     * Generate full, source-specific configuration.
     * (Optional) Sort configuration properties for easy navigation and comparison:
     * @example JavaScript
     * configurationProperties = Object.entries(configurationProperties).sort().reduce((object, [key, value]) => {
     *     object[key] = value;
     *     return object;
     * }, {});
     */
    coreConfig.configurationProperties = {
        "abandonWhenPercentageFull": 0,
        "accessToUnderlyingConnectionAllowed": true,
        "alternateUsernameAllowed": false,
        "authenticateScriptFileName": null,
        "classpath": [],
        "commitOnReturn": false,
        "connectionProperties": null,
        "createScriptFileName": null,
        "customConfiguration": null,
        "customSensitiveConfiguration": null,
        "customizerScriptFileName": null,
        "dataSourceJNDI": null,
        "debug": false,
        "defaultAutoCommit": null,
        "defaultCatalog": null,
        "defaultReadOnly": null,
        "defaultTransactionIsolation": -1,
        "deleteScriptFileName": null,
        "disabledGlobalASTTransformations": null,
        "driverClassName": "org.postgresql.Driver",
        "fairQueue": true,
        "ignoreExceptionOnPreLoad": false,
        "initSQL": null,
        "initialSize": 10,
        "jdbcInterceptors": null,
        "jmxEnabled": true,
        "logAbandoned": false,
        "logValidationErrors": false,
        "maxActive": 100,
        "maxAge": 0,
        "maxIdle": 100,
        "maxWait": 30000,
        "minEvictableIdleTimeMillis": 60000,
        "minIdle": 10,
        "minimumRecompilationInterval": 100,
        "name": "Tomcat Connection Pool[1-929266212]",
        "numTestsPerEvictionRun": 0,
        "password": "openidm",
        "propagateInterruptState": false,
        "recompileGroovySource": false,
        "removeAbandoned": false,
        "removeAbandonedTimeout": 60,
        "resolveUsernameScriptFileName": null,
        "rollbackOnReturn": false,
        "schemaScriptFileName": "SchemaScript.groovy",
        "scriptBaseClass": null,
        "scriptExtensions": [
            "groovy"
        ],
        "scriptOnResourceScriptFileName": null,
        "scriptRoots": [
            "/opt/openicf/scripts/postgres"
        ],
        "searchScriptFileName": "SearchScript.groovy",
        "sourceEncoding": "UTF-8",
        "suspectTimeout": 0,
        "syncScriptFileName": null,
        "targetDirectory": null,
        "testOnBorrow": false,
        "testOnConnect": false,
        "testOnReturn": false,
        "testScriptFileName": "TestScript.groovy",
        "testWhileIdle": false,
        "timeBetweenEvictionRunsMillis": 5000,
        "tolerance": 10,
        "updateScriptFileName": null,
        "url": "jdbc:postgresql://postgresql:5432/openidm",
        "useDisposableConnectionFacade": true,
        "useEquals": true,
        "useLock": false,
        "useStatementFacade": true,
        "username": "openidm",
        "validationInterval": 3000,
        "validationQuery": null,
        "validationQueryTimeout": -1,
        "validatorClassName": null,
        "verbose": false,
        "warningLevel": 1
    };
    console.log('coreConfig (updated)', coreConfig);
    settings = {
        headers: {
            'Accept-API-Version': 'resource=1.0',
            'Content-Type': 'application/json'
        },
        method: 'POST',
        url: '/openidm/system?_action=createFullConfig',
        data: JSON.stringify(coreConfig)
    };
    var fullConfig = await $.ajax(settings).then((response) => {
        return response;
    });
    console.log('fullConfig', fullConfig);
}());
```

Result:

```json
{
  "connectorRef": {
    "connectorHostRef": "rcs-0",
    "bundleVersion": "1.5.20.5-SNAPSHOT",
    "bundleName": "org.forgerock.openicf.connectors.scriptedsql-connector",
    "connectorName": "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConnector"
  },
  "poolConfigOption": {
    "maxObjects": 10,
    "maxIdle": 10,
    "maxWait": 150000,
    "minEvictableIdleTimeMillis": 120000,
    "minIdle": 1
  },
  "resultsHandlerConfig": {
    "enableNormalizingResultsHandler": false,
    "enableFilteredResultsHandler": false,
    "enableCaseInsensitiveFilter": false,
    "enableAttributesToGetSearchResultsHandler": true
  },
  "operationTimeout": {
    "CREATE": -1,
    "UPDATE": -1,
    "DELETE": -1,
    "TEST": -1,
    "SCRIPT_ON_CONNECTOR": -1,
    "SCRIPT_ON_RESOURCE": -1,
    "GET": -1,
    "RESOLVEUSERNAME": -1,
    "AUTHENTICATE": -1,
    "SEARCH": -1,
    "VALIDATE": -1,
    "SYNC": -1,
    "SCHEMA": -1
  },
  "configurationProperties": {
    "createScriptFileName": null,
    "targetDirectory": null,
    "customizerScriptFileName": null,
    "warningLevel": 1,
    "scriptExtensions": [
      "groovy"
    ],
    "password": "openidm",
    "scriptBaseClass": null,
    "scriptRoots": [
      "/opt/openicf/scripts/postgres"
    ],
    "resolveUsernameScriptFileName": null,
    "tolerance": 10,
    "updateScriptFileName": null,
    "disabledGlobalASTTransformations": [
      null
    ],
    "schemaScriptFileName": "SchemaScript.groovy",
    "sourceEncoding": "UTF-8",
    "recompileGroovySource": false,
    "customSensitiveConfiguration": null,
    "authenticateScriptFileName": null,
    "scriptOnResourceScriptFileName": null,
    "minimumRecompilationInterval": 100,
    "deleteScriptFileName": null,
    "customConfiguration": null,
    "searchScriptFileName": "SearchScript.groovy",
    "debug": false,
    "classpath": [],
    "verbose": false,
    "testScriptFileName": "TestScript.groovy",
    "syncScriptFileName": null,
    "connectionProperties": null,
    "propagateInterruptState": false,
    "useDisposableConnectionFacade": true,
    "defaultCatalog": null,
    "validationInterval": 3000,
    "ignoreExceptionOnPreLoad": false,
    "jmxEnabled": true,
    "commitOnReturn": false,
    "logAbandoned": false,
    "maxIdle": 100,
    "testWhileIdle": false,
    "removeAbandoned": false,
    "abandonWhenPercentageFull": 0,
    "minIdle": 10,
    "defaultReadOnly": null,
    "maxWait": 30000,
    "logValidationErrors": false,
    "driverClassName": "org.postgresql.Driver",
    "name": "Tomcat Connection Pool[1-929266212]",
    "useStatementFacade": true,
    "initSQL": null,
    "validationQueryTimeout": -1,
    "validationQuery": null,
    "rollbackOnReturn": false,
    "alternateUsernameAllowed": false,
    "dataSourceJNDI": null,
    "validatorClassName": null,
    "suspectTimeout": 0,
    "useEquals": true,
    "removeAbandonedTimeout": 60,
    "defaultAutoCommit": null,
    "testOnConnect": false,
    "jdbcInterceptors": null,
    "initialSize": 10,
    "defaultTransactionIsolation": -1,
    "numTestsPerEvictionRun": 0,
    "url": "jdbc:postgresql://postgresql:5432/openidm",
    "testOnBorrow": false,
    "fairQueue": true,
    "accessToUnderlyingConnectionAllowed": true,
    "maxAge": 0,
    "minEvictableIdleTimeMillis": 60000,
    "timeBetweenEvictionRunsMillis": 5000,
    "testOnReturn": false,
    "useLock": false,
    "maxActive": 100,
    "username": "openidm"
  },
  "objectTypes": {
    "organization": {
      "$schema": "http://json-schema.org/draft-03/schema",
      "id": "organization",
      "type": "object",
      "nativeType": "organization",
      "properties": {
        "description": {
          "type": "string",
          "required": true,
          "nativeName": "description",
          "nativeType": "string"
        },
        "__NAME__": {
          "type": "string",
          "nativeName": "__NAME__",
          "nativeType": "string"
        },
        "name": {
          "type": "string",
          "required": true,
          "nativeName": "name",
          "nativeType": "string"
        }
      }
    },
    "__GROUP__": {
      "$schema": "http://json-schema.org/draft-03/schema",
      "id": "__GROUP__",
      "type": "object",
      "nativeType": "__GROUP__",
      "properties": {
        "gid": {
          "type": "string",
          "required": true,
          "nativeName": "gid",
          "nativeType": "string"
        },
        "description": {
          "type": "string",
          "required": true,
          "nativeName": "description",
          "nativeType": "string"
        },
        "__NAME__": {
          "type": "string",
          "nativeName": "__NAME__",
          "nativeType": "string"
        },
        "name": {
          "type": "string",
          "required": true,
          "nativeName": "name",
          "nativeType": "string"
        }
      }
    },
    "__ACCOUNT__": {
      "$schema": "http://json-schema.org/draft-03/schema",
      "id": "__ACCOUNT__",
      "type": "object",
      "nativeType": "__ACCOUNT__",
      "properties": {
        "organization": {
          "type": "string",
          "required": true,
          "nativeName": "organization",
          "nativeType": "string"
        },
        "uid": {
          "type": "string",
          "required": true,
          "nativeName": "uid",
          "nativeType": "string"
        },
        "password": {
          "type": "string",
          "required": true,
          "nativeName": "password",
          "nativeType": "string"
        },
        "lastname": {
          "type": "string",
          "required": true,
          "nativeName": "lastname",
          "nativeType": "string"
        },
        "firstname": {
          "type": "string",
          "required": true,
          "nativeName": "firstname",
          "nativeType": "string"
        },
        "__NAME__": {
          "type": "string",
          "nativeName": "__NAME__",
          "nativeType": "string"
        },
        "email": {
          "type": "string",
          "required": true,
          "nativeName": "email",
          "nativeType": "string"
        },
        "fullname": {
          "type": "string",
          "required": true,
          "nativeName": "fullname",
          "nativeType": "string"
        }
      }
    }
  },
  "operationOptions": {
    "CREATE": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "UPDATE": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "DELETE": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "TEST": {
      "objectFeatures": {}
    },
    "SCRIPT_ON_CONNECTOR": {
      "objectFeatures": {}
    },
    "SCRIPT_ON_RESOURCE": {
      "objectFeatures": {}
    },
    "GET": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "RESOLVEUSERNAME": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "AUTHENTICATE": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "SEARCH": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "VALIDATE": {
      "objectFeatures": {}
    },
    "SYNC": {
      "objectFeatures": {
        "organization": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__GROUP__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        },
        "__ACCOUNT__": {
          "operationOptionInfo": {
            "$schema": "http://json-schema.org/draft-03/schema",
            "type": "object",
            "properties": {}
          }
        }
      }
    },
    "SCHEMA": {
      "objectFeatures": {}
    }
  }
}
```