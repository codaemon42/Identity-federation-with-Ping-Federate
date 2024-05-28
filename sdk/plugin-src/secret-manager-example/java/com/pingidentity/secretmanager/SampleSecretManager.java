/*
 * **************************************************
 *  Copyright (C) 2021 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are the property of Ping Identity Corporation.
 *  You may not copy or use this file, in either source code or executable
 *  form, except in compliance with terms set by Ping Identity Corporation.
 *  For further information please contact:
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.pingidentity.secretmanager;

import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.secretmanager.SecretInfo;
import com.pingidentity.sdk.secretmanager.SecretManagerException;
import com.pingidentity.sdk.secretmanager.SecretManager;
import com.pingidentity.sdk.secretmanager.SecretManagerDescriptor;
import com.pingidentity.sdk.secretmanager.SecretReferenceUtil;
import org.sourceid.common.VersionUtil;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.SimpleFieldList;
import org.sourceid.saml20.adapter.gui.ActionDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.util.log.AttributeMap;

import java.util.Map;

/**
 * This class provides a sample implementation for a secret manager that will retrieve
 * environment variable values. </br>
 * </br>
 * This sample also demonstrates how to create an {@link ActionDescriptor} with
 * {@link org.sourceid.saml20.adapter.gui.FieldDescriptor parameters} that are passed to the plugin during action invocation.
 */

public class SampleSecretManager implements SecretManager
{
    private static final String TYPE = "Sample Secret Manager";
    private static final String DESCRIPTION = "Nothing to configure, this secret manager will return the value of the environment variable in the secretId.";

    private static final String SECRET_REFERENCE_GENERATOR_ACTION_NAME = "Generate Secret Reference";
    private static final String SECRET_REFERENCE_GENERATOR_ACTION_DESC = "Generate the secret reference to be used with this secret manager.";
    private static final String ENVIRONMENT_VARIABLE_ACTION_FIELD = "Environment Variable";
    private static final String ENVIRONMENT_VARIABLE_ACTION_FIELD_DESC = "Enter an environment variable. Its value will be used as secret for this secret manager.";

    private static final String SECRET_REFERENCE_VERIFIER_ACTION_NAME = "Verify Secret Reference";
    private static final String SECRET_REFERENCE_VERIFIER_ACTION_DESC = "Verify whether secret reference is valid";
    private static final String SECRET_REFERENCE_ACTION_FIELD = "Secret Reference";
    private static final String SECRET_REFERENCE_ACTION_DESC = "Enter the secret reference to verify.";

    private static final String INVALID_SECRET_REFERENCE = "Invalid secret reference.";
    private static final String VALID_SECRET_REFERENCE = "Valid secret reference. Environment variable value: ";

    /**
     * This method is called by the PingFederate server to push configuration values entered by the administrator via
     * the dynamically rendered GUI configuration screen in the PingFederate administration console. Your implementation
     * should use the {@link Configuration} parameter to configure its own internal state as needed. The tables and
     * fields available in the Configuration object will correspond to the tables and fields defined on the
     * {@link GuiConfigDescriptor} on the PluginDescriptor returned by the {@link #getPluginDescriptor()} method of this class. <br/>
     * <br/>
     * Each time the PingFederate server creates a new instance of your adapter implementation this method will be
     * invoked with the proper configuration. All concurrency issues are handled in the server so you don't need to
     * worry about them here. The server doesn't allow access to your adapter implementation instance until after
     * creation and configuration is completed.
     *
     * @param configuration the Configuration object constructed from the values entered by the user via the GUI.
     */
    @Override
    public void configure(Configuration configuration)
    {
        // no configuration
    }

    /**
     * Returns the {@link PluginDescriptor} that describes this plugin to the PingFederate server. This includes how
     * PingFederate will render the plugin in the administrative console, and metadata on how PingFederate will treat
     * this plugin at runtime.
     *
     * @return A {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        // Build field configuration
        GuiConfigDescriptor guiDescriptor = new GuiConfigDescriptor();
        RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator();
        guiDescriptor.setDescription(DESCRIPTION);

        ActionDescriptor generateActionDescriptor = new ActionDescriptor(SECRET_REFERENCE_GENERATOR_ACTION_NAME, SECRET_REFERENCE_GENERATOR_ACTION_DESC, new SecretReferenceAction());
        TextFieldDescriptor environmentVariableField = new TextFieldDescriptor(ENVIRONMENT_VARIABLE_ACTION_FIELD, ENVIRONMENT_VARIABLE_ACTION_FIELD_DESC);
        environmentVariableField.addValidator(requiredFieldValidator);
        generateActionDescriptor.addParameter(environmentVariableField);
        guiDescriptor.addAction(generateActionDescriptor);

        ActionDescriptor verifyActionDescriptor = new ActionDescriptor(SECRET_REFERENCE_VERIFIER_ACTION_NAME, SECRET_REFERENCE_VERIFIER_ACTION_DESC, new SecretReferenceVerifier());
        TextFieldDescriptor secretReferenceField = new TextFieldDescriptor(SECRET_REFERENCE_ACTION_FIELD, SECRET_REFERENCE_ACTION_DESC);
        secretReferenceField.addValidator(requiredFieldValidator);
        verifyActionDescriptor.addParameter(secretReferenceField);
        guiDescriptor.addAction(verifyActionDescriptor);

        // Build plugin descriptor
        return new SecretManagerDescriptor(TYPE, this, guiDescriptor, VersionUtil.getVersion());
    }

    /**
     * Returns the {@link SecretInfo} containing the secret and any attributes retrieved.
     * Attributes that may be returned are described in detail in {@link SecretInfo}.
     *
     * @param secretId the secret identifier parsed from the secret reference.
     * @param inParameters A map that contains a set of additional input parameters.
     *                     The available input parameters are detailed in {@link SecretManager}
     *                     prefixed with <code>IN_PARAMETER_NAME_*</code>.
     * @return A {@link SecretInfo} that contains the secret and any additional attributes.
     * @throws SecretManagerException A checked exception indicating the retrieval of secret has failed.
     */
    @Override
    public SecretInfo getSecretInfo(String secretId, Map<String, Object> inParameters) throws SecretManagerException
    {
        return getEnvironmentVariableValue(secretId);
    }

    private static SecretInfo getEnvironmentVariableValue(String environmentVariable) throws SecretManagerException
    {
        if (environmentVariable == null)
        {
            throw new SecretManagerException("Environment variable cannot be null.");
        }

        String secret;
        try
        {
            secret = System.getenv(environmentVariable);
        }
        catch (SecurityException e)
        {
            // proper logging should be considered here
            throw new SecretManagerException(e);
        }

        if (secret == null)
        {
            throw new SecretManagerException("Environment variable not defined.");
        }

        return new SecretInfo(secret, new AttributeMap());
    }

    /**
     * This {@link ActionDescriptor.Action} provides a sample implementation for a secret manager Action
     * that will generate a secret reference for applicable PingFederate configurations.</br>
     * </br>
     * It is recommended to provide a secret manager with an {@link ActionDescriptor.Action}
     * to generate the secret reference (format <code>OBF:MGR:{secretManagerId}:{secretId}</code>) as PingFederate will invoke
     * {@link #getSecretInfo} with the secretId of the corresponding secret manager identified by the <code>secretManagerId</code>.
     */
    public static class SecretReferenceAction implements ActionDescriptor.Action
    {
        /**
         * This method will not be called if {@link #actionInvoked(Configuration, SimpleFieldList)}
         * is overridden.
         */
        @Override
        public String actionInvoked(Configuration configuration)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Generates the secret reference in the format <code>OBF:MGR:{secretManagerId}:{secretId}</code></br>
         * <code>secretManagerId</code> is the identifier of this secret manager</br>
         * <code>secretId</code> is the {@link String} passed into {@link #getSecretInfo(String, Map)}
         *
         * @param configuration the configuration of the adapter.
         * @param actionParameters the parameter list used to invoke the action,
         *                         the parameters available are those added to the corresponding {@link ActionDescriptor}.
         * @return A {@link String}, the result of the action. This value will be rendered in the GUI after invocation
         * or returned in response through admin API action invocation.
         */
        @Override
        public String actionInvoked(Configuration configuration, SimpleFieldList actionParameters)
        {
            String environmentVariable = actionParameters.getFieldValue(ENVIRONMENT_VARIABLE_ACTION_FIELD);

            return SECRET_REFERENCE_PREFIX + configuration.getId() + ":" + environmentVariable;
        }
    }

    /**
     * This {@link ActionDescriptor.Action} provides a sample implementation for a secret manager Action
     * that will verify whether a secret reference is valid or not.</br>
     * </br>
     * It is recommended to provide a secret manager with an {@link ActionDescriptor.Action}
     * to verify the secret reference (format <code>OBF:MGR:{secretManagerId}:{secretId}</code>) to check that
     * PingFederate is retrieving the correct secret value.
     */
    public static class SecretReferenceVerifier implements ActionDescriptor.Action
    {
        /**
         * This method will not be called if {@link #actionInvoked(Configuration, SimpleFieldList)}
         * is overridden.
         */
        @Override
        public String actionInvoked(Configuration configuration)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Verifies the components of the secret reference format <code>OBF:MGR:{secretManagerId}:{secretId}</code>
         *
         * @param configuration the configuration of the adapter.
         * @param actionParameters the parameter list used to invoke the action,
         *                         the parameters available are those added to the corresponding {@link ActionDescriptor}.
         * @return A {@link String}, the result of the action. This value will be rendered in the GUI after invocation
         * or returned in response through admin API action invocation.
         */
        @Override
        public String actionInvoked(Configuration configuration, SimpleFieldList actionParameters)
        {
            String secretReference = actionParameters.getFieldValue(SECRET_REFERENCE_ACTION_FIELD);

            String secretManagerId = SecretReferenceUtil.getSecretManagerId(secretReference);
            if (secretManagerId == null || !secretManagerId.equals(configuration.getId()))
            {
                return INVALID_SECRET_REFERENCE;
            }

            SecretInfo secretInfo;
            try
            {
                secretInfo = getEnvironmentVariableValue(SecretReferenceUtil.getSecretId(secretReference));
            }
            catch (SecretManagerException e)
            {
                return INVALID_SECRET_REFERENCE + " Due to: " + e.getMessage();
            }

            return VALID_SECRET_REFERENCE + secretInfo.getSecret();
        }
    }
}
