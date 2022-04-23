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
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException
import org.identityconnectors.framework.common.objects.OperationOptions

import static org.identityconnectors.common.security.SecurityUtil.decrypt

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log
def options = options as OperationOptions
def scriptArguments = scriptArguments as Map
def scriptLanguage = scriptLanguage as String
def scriptText = scriptText as String

println('scriptText: ' + scriptText)
println('options.runAsUser: ' + options.runAsUser)
println('scriptArguments.class: ' + scriptArguments.getClass().getName())
println('scriptArguments: ' + scriptArguments.get('arg1'))

if ("groovy".equalsIgnoreCase(scriptLanguage)) {
    log.info("Evaluate {0} Script", operation)
    if (null != options.runAsUser) {
        if ("admin".equals(options.runAsUser)) {
            if ("Passw0rd".equals(decrypt(options.runWithPassword))) {
                log.info("Successfully authenticated")
            } else {
                throw new InvalidPasswordException()
            }
        } else {
            throw new InvalidCredentialException();
        }
    }
    Binding binding = new Binding();
    binding.setVariable("arg", scriptArguments)
    return new GroovyShell(binding).evaluate(scriptText);
} else {
    throw new InvalidAttributeValueException("Unsupported scriptLanguage:" + scriptLanguage)
}
