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

import org.identityconnectors.common.logging.Log
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration

import groovy.sql.Sql
import java.sql.Connection

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log

def connection = new Sql(connection as Connection)

log.info('THIS IS ' + operation + ' SCRIPT')
// log.info('Ouch: ' + Globals.ouch)
// java.util.LinkedHashMap
this.binding.variables.each {
    key, value ->
    println(key + ': ' + value.toString() + ' class: ' + value.getClass())
}

def connectionExecute = connection.execute('SELECT 1;')
println('connectionExecute: ' + connectionExecute)

//throw new MissingResourceException("Test Failed", operation.name(), "")
