/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal-notices/CDDLv1_0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal-notices/CDDLv1_0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2017 ForgeRock AS.
 */

// import ObjectCacheLibrary
import groovy.json.JsonOutput
import org.forgerock.openicf.connectors.groovy.ICFObjectBuilder
import org.forgerock.openicf.connectors.groovy.MapFilterVisitor
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.ConnectorObject
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.exceptions.ConnectorException
import java.util.LinkedHashMap

//class Globals {
//
//    static String ouch = "I'm global.."
//
//}

// Bindings:
def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
// Have to assign a non-final variable (because the script is reused by the same instance?).
def empty = new ObjectClass('__EMPTY__')
// query: org.forgerock.openicf.connectors.groovy.ScriptedConnectorBase$_executeQuery_closure3@263ff233
// handler: org.forgerock.openicf.connectors.groovy.ScriptedConnectorBase$_executeQuery_closure2@1d88f253

try {
    log.info('THIS IS ' + operation + ' SCRIPT')
    // log.info('Ouch: ' + Globals.ouch)
    // java.util.LinkedHashMap
    this.binding.variables.each {
        key, value ->
        println(key + ': ' + value.toString() + ' class: ' + value.getClass())
    }

    // configuration.propertyBag.customConfiguration = new LinkedHashMap()
    // configuration.propertyBag.customConfiguration.put('key', 'value')
    println('configuration.propertyBag: ' + configuration.propertyBag)

    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            def resultSet = ObjectCacheLibrary.instance.search(objectClass, filter, options.getSortKeys())

            // Handle the results
            if (null != options.getPageSize()) {
                // Paged Search
                // Sample script for IDME-178
                final String PAGED_RESULTS_COOKIE = options.getPagedResultsCookie()
                String currentPagedResultsCookie = options.getPagedResultsCookie()
                final Integer PAGED_RESULTS_OFFSET =
                        null != options.getPagedResultsOffset() ? Math.max(0, options
                                .getPagedResultsOffset()) : 0
                final Integer PAGE_SIZE = options.getPageSize()

                int index = 0
                int handled = 0
                int pageStartIndex = PAGED_RESULTS_COOKIE == null ? 0 : -1

                for (ConnectorObject entry : resultSet) {
                    if (pageStartIndex < 0 && PAGED_RESULTS_COOKIE == entry.getName().getNameValue()) {
                        pageStartIndex = index + 1
                    }

                    if (pageStartIndex < 0 || index < pageStartIndex) {
                        index++
                        continue
                    }

                    if (handled >= PAGE_SIZE) {
                        break
                    }

                    if (index >= PAGED_RESULTS_OFFSET + pageStartIndex) {
                        if (handler(entry)) {
                            handled++
                            currentPagedResultsCookie = entry.getName().getNameValue()
                        } else {
                            break
                        }
                    }
                    index++
                }

                if (index == resultSet.size()) {
                    currentPagedResultsCookie = null
                }

                return new SearchResult(currentPagedResultsCookie, resultSet.size() - index)
            }

            // Normal Search
            for (ConnectorObject entry : resultSet) {
                if (!handler(entry)) {
                    break
                }
            }

            break
        case TestHelper.TEST:
            Set<String> attributesToGet = null
            if (null != options.attributesToGet) {
                attributesToGet = options.attributesToGet as Set<String>
            }

            if (filter instanceof EqualsFilter && (filter as EqualsFilter).name == Uid.NAME) {
                //This is a Read
                handler {
                    uid AttributeUtil.getStringValue((filter as EqualsFilter).attribute)
                    id AttributeUtil.getStringValue((filter as EqualsFilter).attribute)
                    delegate.objectClass(objectClass)
                    TestHelper.connectorObjectTemplate.each { key, value ->
                        if (attributesToGet == null || attributesToGet.contains(key)) {
                            attribute key, value
                        }
                    }
                }
            } else {
                for (i in 0..9) {
                    def co = ICFObjectBuilder.co {
                        uid String.format('UID%02d', i)
                        id String.format('TEST%02d', i)
                        delegate.objectClass(objectClass)
                        TestHelper.connectorObjectTemplate.each { key, value ->
                            if (attributesToGet == null || attributesToGet.contains(key)) {
                                attribute key, value
                            }
                        }
                    }
                    if (filter?.accept(co)) {
                        handler(co)
                    } else {
                        handler(co)
                    }
                }
            }
            if (null != filter) {
                def map = filter.accept(MapFilterVisitor.INSTANCE, null)
                return new SearchResult(JsonOutput.toJson(map), -1)
            }
            break
        case TestHelper.SAMPLE:
            handler(
                    ICFObjectBuilder.co {
                        uid '12'
                        id '12'
                        delegate.objectClass(objectClass)
                        attribute {
                            name 'sureName'
                            value 'Foo'
                        }
                        attribute {
                            name 'lastName'
                            value 'Bar'
                        }
                        attribute {
                            name 'groups'
                            values 'Group1', 'Group2'
                        }
                        attribute 'active', true
                        attribute 'NULL'
                    }
            )
            handler({
                uid '13'
                id '13'
                delegate.objectClass(objectClass)
                attribute {
                    name 'sureName'
                    value 'Foo'
                }
                attribute {
                    name 'lastName'
                    value 'Bar'
                }
                attribute {
                    name 'groups'
                    values 'Group1', 'Group2'
                }
                attribute 'active', true
                attribute 'NULL'
                attributes(new Attribute('emails', [
                        [
                                'address'   : 'foo@example.com',
                                'type'      : 'home',
                                'customType': '',
                                'primary'   : true]
                ]))
            }
            )
            return new SearchResult()
        case empty:
            return new SearchResult()
        default:
            throw new UnsupportedOperationException(operation.name() + ' operation of type:' +
                    objectClass.objectClassValue + ' is not supported.')
    }
} catch (Exception e) {
    def message = 'Exception: ' + e.getMessage()
    log.error(message)
    UnsupportedOperationException(message)
}
