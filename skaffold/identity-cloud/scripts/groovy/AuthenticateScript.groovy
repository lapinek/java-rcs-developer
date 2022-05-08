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

import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.exceptions.ConnectorSecurityException
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException
import org.identityconnectors.framework.common.exceptions.PasswordExpiredException
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException
import org.identityconnectors.framework.common.exceptions.UnknownUidException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def username = username as String
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def password = password as GuardedString

def unsupportedOperationException = new UnsupportedOperationException(
    operation.name() + ' operation of type:' + objectClass.objectClassValue + ' is not supported.'
)

log.info('This is AuthenticateScript')

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        throw unsupportedOperationException
    case ObjectClass.GROUP:
        throw unsupportedOperationException
    case ObjectClass.ALL:
        log.error('ICF Framework MUST reject this')
        break
    case TestHelper.TEST:
        switch (username) {
            case 'TEST1':
                throw new ConnectorSecurityException()
            case 'TEST2':
                throw new InvalidCredentialException()
            case 'TEST3':
                throw new InvalidPasswordException()
            case 'TEST4':
                throw new PermissionDeniedException()
            case 'TEST5':
                throw new PasswordExpiredException()
            case 'TESTOK1':
                def clearPassword = SecurityUtil.decrypt(password)
                if (clearPassword == 'Passw0rd') {
                    return new Uid(username)
                }
                throw new InvalidPasswordException()
            case 'TESTOK2':
                def clearPassword = SecurityUtil.decrypt(password)
                if (clearPassword == '') {
                    return new Uid(username)
                }
                throw new ConnectorException('The password must be empty in this test')
            default:
                throw new UnknownUidException()
        }
    case TestHelper.SAMPLE:
        throw unsupportedOperationException
    default:
        throw unsupportedOperationException
}

