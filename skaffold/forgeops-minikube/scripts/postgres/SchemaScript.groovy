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

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_CREATABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_READABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.NOT_UPDATEABLE
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED

import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedByteArray
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos
import org.identityconnectors.framework.spi.operations.AuthenticateOp
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp
import org.identityconnectors.framework.spi.operations.SchemaOp
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp
import org.identityconnectors.framework.spi.operations.SearchOp
import org.identityconnectors.framework.spi.operations.SyncOp
import org.identityconnectors.framework.spi.operations.TestOp

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED

try {
    // Q: Is operation to be used in this script?
    def operation = operation as OperationType
    def configuration = configuration as ScriptedConfiguration
    def log = log as Log

    log.info('THIS IS ' + operation + ' SCRIPT')
    // log.info('Ouch: ' + Globals.ouch)
    // java.util.LinkedHashMap
    this.binding.variables.each {
        key, value ->
        println(key + ': ' + value.toString() + ' class: ' + value.getClass())
    }

    builder.schema {
        objectClass {
            type ObjectClass.ACCOUNT_NAME
            attributes {
                uid String.class, REQUIRED
                password String.class, REQUIRED
                firstname String.class, REQUIRED
                lastname String.class, REQUIRED
                fullname String.class, REQUIRED
                email String.class, REQUIRED
                organization String.class, REQUIRED
            }

        }
        objectClass {
            type ObjectClass.GROUP_NAME
            attributes {
                name String.class, REQUIRED
                gid String.class, REQUIRED
                description String.class, REQUIRED
            }
        }
        objectClass {
            type 'organization'
            attributes {
                name String.class, REQUIRED
                description String.class, REQUIRED
            }
        }
    }
} catch (e) {
    println('Exception: ' + e.getMessage())
    throw new UnsupportedOperationException(e.getMessage())
}
