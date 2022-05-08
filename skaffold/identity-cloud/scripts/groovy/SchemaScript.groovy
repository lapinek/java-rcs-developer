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
            attribute OperationalAttributeInfos.PASSWORD
            attribute PredefinedAttributeInfos.DESCRIPTION
            attribute 'groups', String, EnumSet.of(MULTIVALUED)
            attributes {
                userName String, REQUIRED
                email REQUIRED, MULTIVALUED
                __ENABLE__ Boolean
                createDate NOT_CREATABLE, NOT_UPDATEABLE
                lastModified Long, NOT_CREATABLE, NOT_UPDATEABLE, NOT_RETURNED_BY_DEFAULT
                passwordHistory String, MULTIVALUED, NOT_UPDATEABLE, NOT_READABLE, NOT_RETURNED_BY_DEFAULT
                firstName()
                sn()
            }

        }

        objectClass {
            type ObjectClass.GROUP_NAME
            attribute PredefinedAttributeInfos.DESCRIPTION
            attribute '__NAME__'
            attributes {
                cn REQUIRED
                member REQUIRED, MULTIVALUED
            }
        // ONLY CRUD
        }

        objectClass {
            type '__TEST__'
            container()
            attributes {

                // All possible attribute types

                attributeString String
                attributeStringMultivalue String, MULTIVALUED

                attributelongp Long.TYPE
                attributelongpMultivalue Long.TYPE, MULTIVALUED

                attributeLong Long
                attributeLongMultivalue Long, MULTIVALUED

                attributechar Character.TYPE
                attributecharMultivalue Character.TYPE, MULTIVALUED

                attributeCharacter Character
                attributeCharacterMultivalue Character, MULTIVALUED

                attributedoublep Double.TYPE
                attributedoublepMultivalue Double.TYPE, MULTIVALUED

                attributeDouble Double
                attributeDoubleMultivalue Double, MULTIVALUED

                attributefloatp Float.TYPE
                attributefloatpMultivalue Float.TYPE, MULTIVALUED

                attributeFloat Float
                attributeFloatMultivalue Float, MULTIVALUED

                attributeint Integer.TYPE
                attributeintMultivalue Integer.TYPE, MULTIVALUED

                attributeInteger Integer
                attributeIntegerMultivalue Integer, MULTIVALUED

                attributebooleanp Boolean.TYPE
                attributebooleanpMultivalue Boolean.TYPE, MULTIVALUED

                attributeBoolean Boolean
                attributeBooleanMultivalue Boolean, MULTIVALUED

                attributebytep Byte.TYPE
                attributebytepMultivalue Byte.TYPE, MULTIVALUED

                attributeByte Byte
                attributeByteMultivalued Byte, MULTIVALUED

                attributeByteArray byte[].class
                attributeByteArrayMultivalue byte[].class, MULTIVALUED

                // attributeBigDecimal BigDecimal
                // attributeBigDecimalMultivalue BigDecimal, MULTIVALUED

                attributeBigInteger BigInteger
                attributeBigIntegerMultivalue BigInteger, MULTIVALUED

                attributeGuardedByteArray GuardedByteArray
                attributeGuardedByteArrayMultivalue GuardedByteArray, MULTIVALUED

                attributeGuardedString GuardedString
                attributeGuardedStringMultivalue GuardedString, MULTIVALUED

                attributeMap Map
                attributeMapMultivalue Map, MULTIVALUED

            }
        }
        operationOption {
            name 'notify'
            disable AuthenticateOp, ResolveUsernameOp, SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SyncOp, TestOp
        }
        operationOption {
            name 'force'
            type Boolean
        }

        defineOperationOption OperationOptionInfoBuilder.buildPagedResultsCookie(), SearchOp
        defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
        defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
        defineOperationOption OperationOptionInfoBuilder.buildSortKeys(), SearchOp
        defineOperationOption OperationOptionInfoBuilder.buildRunWithUser()
        defineOperationOption OperationOptionInfoBuilder.buildRunWithPassword()
    }
} catch (e) {
    println('Exception: ' + e.getMessage())
    throw new UnsupportedOperationException(e.getMessage())
}
