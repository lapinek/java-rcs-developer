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

import ObjectCacheLibrary
import org.identityconnectors.common.logging.Log
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid


switch (objectClass) {
    case ObjectClass.ACCOUNT:
        ObjectCacheLibrary.instance.delete(objectClass, uid)
        break
    case ObjectClass.GROUP:
        ObjectCacheLibrary.instance.delete(objectClass, uid)
        break
    case ObjectClass.ALL:
        log.error("ICF Framework MUST reject this")
        break
    case TestHelper.TEST:
        //Sample script for IDME-180:Support MVCC Revision attribute
        TestHelper.exceptionTest(operation, objectClass, uid, options)
        break
    case TestHelper.SAMPLE:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}
