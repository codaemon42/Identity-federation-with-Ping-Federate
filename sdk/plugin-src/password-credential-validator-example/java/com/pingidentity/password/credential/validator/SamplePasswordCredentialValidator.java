/*
 * **************************************************
 *  Copyright (C) 2019 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are subject to the terms of the
 *  Ping Identity Corporation SDK Developer Guide.
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.pingidentity.password.credential.validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pingidentity.sdk.password.RecoverableUsername;
import com.pingidentity.sdk.password.UsernameRecoveryException;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.util.log.AttributeMap;

import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.password.ChangeablePasswordCredential;
import com.pingidentity.sdk.password.PasswordCredentialValidator;
import com.pingidentity.sdk.password.PasswordCredentialValidatorAuthnException;
import com.pingidentity.sdk.password.PasswordChangeResult;
import com.pingidentity.sdk.password.PasswordResetException;
import com.pingidentity.sdk.password.PasswordValidationException;
import com.pingidentity.sdk.password.ResettablePasswordCredential;

/**
 * A password credential validator containing a single username and password pair.
 *
 * Not for actual deployments but useful for POCs and as an SDK example.
 *
 * This sample also demonstrates how to implement a PCV that supports password change
 * and reset operations. Note that updates to the password value through non-admin
 * scenarios are not persisted to configuration or replicated in the cluster.
 * This is a limitation of this sample as it is based on the PingFederate configuration
 * replication model for credential storage. A proper implementation should depend
 * on an external system for replication of credential updates so it becomes
 * accessible to all nodes within a PingFederate deployment.
 */
public class SamplePasswordCredentialValidator implements PasswordCredentialValidator, ChangeablePasswordCredential, ResettablePasswordCredential,
        RecoverableUsername
{
    private static final String TYPE = "Sample Password Credential Validator";
    private static final String TYPE_DESC = "This sample PCV demonstrates how to validate a set of credentials and support password change and reset operations. " +
                                      "Please note that password updates through non-admin scenarios are not persisted to configuration or replicated in the cluster.";

    private static final String USERNAME = "Username";
    private static final String USERNAME_DESC = "Enter a username to be validated.";
    private static final String PASSWORD = "Password";
    private static final String PASSWORD_DESC = "Enter a password to be validated.";
    private static final String NAME = "Name";
    private static final String NAME_DESC = "For password reset, provide a descriptive name for the user to personalize notifications.";
    private static final String MAIL = "Mail";
    private static final String MAIL_DESC = "For password reset, provide an email address for request authorizations and notifications.";
    private static final String PHONE = "Phone";
    private static final String PHONE_DESC = "For password reset using text messages, provide a phone number to send one-time passwords.";
    private static final String PINGID_USERNAME = "PingID Username";
    private static final String PINGID_USERNAME_DESC = "For password reset using PingID, provide the username as provisioned in PingID services.";
    private static final String MAIL_VERIFIED = "Mail Verified";
    private static final String MAIL_VERIFIED_DESC = "For password reset, account unlock and username recovery, has the email address been verified.";

    private static final String USERNAME_ATTRIBUTE = "username";
    private static final String NAME_ATTRIBUTE = "givenName";
    private static final String MAIL_ATTRIBUTE = "mail";
    private static final String PHONE_ATTRIBUTE = "phone";
    private static final String PINGID_ATTRIBUTE = "pingid";
    private static final String MAIL_VERIFIED_ATTRIBUTE = "mailVerified";

    // Minimum password length to demonstrate password policy enforcement
    private static final int MIN_PASSWORD_LENGTH = 6;

    String username = null;
    String password = null;
    String name = null;
    String mail = null;
    String phone = null;
    String pingIdUsername = null;

    boolean mailVerified = false;

    /**
     * This method is called by the PingFederate server to push configuration values entered by the administrator via
     * the dynamically rendered GUI configuration screen in the PingFederate administration console. Your implementation
     * should use the {@link Configuration} parameter to configure its own internal state as needed. <br/>
     * <br/>
     * Each time the PingFederate server creates a new instance of your plugin implementation this method will be
     * invoked with the proper configuration. All concurrency issues are handled in the server so you don't need to
     * worry about them here. The server doesn't allow access to your plugin implementation instance until after
     * creation and configuration is completed.
     *
     * @param configuration
     *            the Configuration object constructed from the values entered by the user via the GUI.
     */
    @Override
    public void configure(Configuration configuration)
    {
        username = configuration.getFieldValue(USERNAME);
        password = configuration.getFieldValue(PASSWORD);
        name = configuration.getFieldValue(NAME);
        mail = configuration.getFieldValue(MAIL);
        phone = configuration.getFieldValue(PHONE);
        pingIdUsername = configuration.getFieldValue(PINGID_USERNAME);
        mailVerified = configuration.getBooleanFieldValue(MAIL_VERIFIED);
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
        RequiredFieldValidator requiredFieldValidator = new RequiredFieldValidator();

        GuiConfigDescriptor guiDescriptor = new GuiConfigDescriptor();
        guiDescriptor.setDescription(TYPE_DESC);

        TextFieldDescriptor usernameFieldDescriptor = new TextFieldDescriptor(USERNAME, USERNAME_DESC);
        usernameFieldDescriptor.addValidator(requiredFieldValidator);
        guiDescriptor.addField(usernameFieldDescriptor);

        TextFieldDescriptor passwordFieldDescriptor = new TextFieldDescriptor(PASSWORD, PASSWORD_DESC, true);
        passwordFieldDescriptor.addValidator(requiredFieldValidator);
        guiDescriptor.addField(passwordFieldDescriptor);

        TextFieldDescriptor nameFieldDescriptor = new TextFieldDescriptor(NAME, NAME_DESC);
        guiDescriptor.addField(nameFieldDescriptor);

        TextFieldDescriptor mailFieldDescriptor = new TextFieldDescriptor(MAIL, MAIL_DESC);
        guiDescriptor.addField(mailFieldDescriptor);

        TextFieldDescriptor phoneFieldDescriptor = new TextFieldDescriptor(PHONE, PHONE_DESC);
        guiDescriptor.addField(phoneFieldDescriptor);

        TextFieldDescriptor pingIdUsernameFieldDescriptor = new TextFieldDescriptor(PINGID_USERNAME, PINGID_USERNAME_DESC);
        guiDescriptor.addField(pingIdUsernameFieldDescriptor);

        CheckBoxFieldDescriptor mailVerifiedFieldDescriptor = new CheckBoxFieldDescriptor(MAIL_VERIFIED, MAIL_VERIFIED_DESC);
        guiDescriptor.addField(mailVerifiedFieldDescriptor);

        // Build plugin descriptor
        PluginDescriptor pluginDescriptor = new PluginDescriptor(TYPE, this, guiDescriptor);

        Set<String> contract = new HashSet<String>();
        contract.add(USERNAME_ATTRIBUTE);
        contract.add(NAME_ATTRIBUTE);
        contract.add(MAIL_ATTRIBUTE);

        pluginDescriptor.setAttributeContractSet(contract);

        pluginDescriptor.setSupportsExtendedContract(false);

        return pluginDescriptor;
    }

    /**
     * Validates the given username and password in the manner appropriate to the plugin implementation.
     *
     * @param username
     *            the given username/id
     * @param password
     *            the given password
     * @return An AttributeMap with at least one entry representing the principal. The key of the entry does not matter,
     *         so long as the map is not empty. If the map is empty or null, the username and password combination is
     *         considered invalid.
     * @throws PasswordValidationException
     *             runtime exception when the validator cannot process the username and password combination due to
     *             system failure such as data source off line, host name unreachable, etc.
     */
    @Override
    public AttributeMap processPasswordCredential(String username, String password) throws PasswordValidationException
    {
        AttributeMap attributeMap = null;

        if (username == null && password == null)
        {
            throw new PasswordValidationException("Unable to validate null credentials.");
        }

        // Validate against the single set of credentials defined by configuration
        if (this.username.equalsIgnoreCase(username) && this.password.equals(password))
        {
            attributeMap = new AttributeMap();

            attributeMap.put(USERNAME_ATTRIBUTE, new AttributeValue(username));
            attributeMap.put(NAME_ATTRIBUTE, new AttributeValue(name));
            attributeMap.put(MAIL_ATTRIBUTE, new AttributeValue(mail));
        }

        // If authentication failed - returns null (an empty map is also OK)
        return attributeMap;
    }

    /**
     * Change a user's password in the data store.
     *
     * @param username name of the user attempting to change their password
     * @param oldPassword the user's existing password
     * @param newPassword the user's new password
     * @param inParameters additional parameters that can be passed to an implementation
     *
     * @return PasswordChangeResult - for future use
     *
     * @throws PasswordValidationException for system errors
     * @throws PasswordCredentialValidatorAuthnException for user authentication errors
     */
    @Override
    public PasswordChangeResult changePassword(String username, String oldPassword, String newPassword, Map<String,Object> inParameters)
       throws PasswordValidationException
    {
        if (newPassword.length() < MIN_PASSWORD_LENGTH)
        {
            // Password policy violation, throw a recoverable exception
            throw new PasswordCredentialValidatorAuthnException(true, "Password is too short.");
        }
        this.password = newPassword;

        return new PasswordChangeResult();
    }


    /**
     * Indicates whether attributes required to email the user about password change will be returned by the {@link PasswordCredentialValidator}.
     * Here are the attributes that should be returned:
     *  1) The first name with the attribute name 'givenName'.
     *  2) The email address with the attribute name 'mail'.
     *
     */
    @Override
    public boolean isChangePasswordEmailNotifiable()
    {
        // Notification is possible if mail and given name are provided
        return ((mail != null && !mail.isEmpty()) && (name != null && !name.isEmpty()));
    }


    /**
     * Indicate whether passwords can be changed in the current state.
     * <br><br>
     * In some cases implementing this interface is not sufficient for enabling password changes
     * through a {@link PasswordCredentialValidator}, it could also depend on some system configuration
     * or current condition.  For example, passwords can only be changed in Active Directory if SSL is
     * enabled on the LDAP data store.
     * <br><br>
     * If this method returns false, it's recommended that a warning be logged indicating the reason and
     * to help the admin resolve the issue.
     *
     * Don't call this method if password changing is disabled by the admin
     * otherwise the PCV may log errors for a disabled feature.
     *
     * @return if conditions allow for password changes
     */
    @Override
    public boolean isPasswordChangeable()
    {
        // Password change is always possible
        return true;
    }

    /**
     * Indicates whether attributes required to warn the user about expiring password will be returned.
     * The required attribute is password expiration time of the user with an attribute name 'passwordExpiryTime'. This attribute value should be number of milliseconds since January 1, 1970.
     *
     */
    @Override
    public boolean isPendingPasswordExpiryNotifiable()
    {
        // No support for password expiry notifications in this sample
        return false;
    }

    /**
     * Retrieves a map of attributes from the user in the data store.
     *
     * If the user is found, an {@link AttributeMap} is returned with the following attributes:
     *
     * <pre>
     *  Key                             Value
     *  ===========================     ===========================
     *  Value of {@link #getMailAttribute()}     The email address of the user to send the password reset email to.
     *  Value of {@link #getNameAttribute()}     The name of the user.
     *  Value of {@link #getSmsAttribute()}      The phone number of the user to send the password reset SMS text to.
     *  Value of {@link #getPingIdUsernameAttribute()} The username to use for PingID based password reset.
     * </pre>
     *
     * If the user is not found a {@link PasswordResetException} is thrown.
     *
     * @param username the name of the user to find.
     * @return a map of user attributes
     * @throws PasswordResetException
     */
    @Override
    public AttributeMap	findUser(String username) throws PasswordResetException
    {
        if (!this.username.equalsIgnoreCase(username))
        {
            throw new PasswordResetException(true, "Username not found");
        }

        AttributeMap foundUser = new AttributeMap();

        foundUser.put(NAME_ATTRIBUTE, name);
        foundUser.put(MAIL_ATTRIBUTE, mail);
        foundUser.put(PHONE_ATTRIBUTE, phone);
        foundUser.put(PINGID_ATTRIBUTE, pingIdUsername);
        foundUser.put(MAIL_VERIFIED_ATTRIBUTE, Boolean.toString(mailVerified));

        return foundUser;
    }

    /**
     * Retrieves a List of AttributeMaps from the user(s) in the data store by the provided email search filter.
     *
     * If the user is found, an {@link List<AttributeMap>} is returned with the following attributes:
     *
     * <pre>
     *  Key                             Value
     *  ===========================     ===========================
     *  Value of {@link #getUsernameAttribute()}     The username to send in the recovered username email.
     *  Value of {@link #getMailVerifiedAttribute()}     The email verification status
     * </pre>
     *
     * If the user is not found a {@link UsernameRecoveryException} is thrown.
     *
     * @param mail the email address of the user to find.
     * @return a List of AttributeMaps. Each map corresponds to a found user account. Multiple accounts are possible as email addresses may not be unique.
     * @throws UsernameRecoveryException
     */
    @Override
    public List<AttributeMap> findUsersByMail(String mail) throws UsernameRecoveryException
    {
        if(!this.mail.equalsIgnoreCase(mail))
        {
            throw new UsernameRecoveryException("Username not found");
        }

        AttributeMap userAttributes = new AttributeMap();
        userAttributes.put(getUsernameAttribute(), username);
        userAttributes.put(getMailVerifiedAttribute(), Boolean.toString(mailVerified));

        return Collections.singletonList(userAttributes);
    }

    /**
     * The attribute/field that contains the registered email for the user. This value is used for email communication with the user.
     */
    @Override
    public String getMailAttribute()
    {
        return MAIL_ATTRIBUTE;
    }

    /**
     * The attribute/field that contains for the common name for the user. This value is used for email communication with the user.
     */
    @Override
    public String getNameAttribute()
    {
        return NAME_ATTRIBUTE;
    }

    /**
     * The attribute/field containing the username to use for PingID based password reset.
     */
    @Override
    public String getPingIdUsernameAttribute()
    {
        return PINGID_ATTRIBUTE;
    }

    /**
     * The attribute/field containing the verification status of the email address used for username recovery.
     */
    @Override
    public String getMailVerifiedAttribute()
    {
        return MAIL_VERIFIED_ATTRIBUTE;
    }

    /**
     * The attribute/field containing the user's username. Used for username recovery.
     */
    @Override
    public String getUsernameAttribute()
    {
        return USERNAME_ATTRIBUTE;
    }

    /**
     * The attribute/field that contains the phone number to send the password reset SMS text to.
     */
    @Override
    public String getSmsAttribute()
    {
        return PHONE_ATTRIBUTE;
    }

    /**
     * Indicate whether passwords can be changed in the current state.
     * <br><br>
     * In some cases implementing this interface is not sufficient for enabling password changes
     * through a {@link PasswordCredentialValidator}, it could also depend on some system configuration
     * or current condition.  For example, passwords can only be changed in Active Directory if SSL is
     * enabled on the LDAP data store.
     * <br><br>
     * If this method returns false, it's recommended that a warning be logged indicating the reason and
     * to help the admin resolve the issue.
     *
     * Don't call this method if password changing is disabled by the admin
     * otherwise the PCV may log errors for a disabled feature.
     *
     * @return if conditions allow for password changes
     */
    @Override
    public boolean isPasswordResettable()
    {
        // Check if any one of the required password reset fields are provided
        boolean supportsReset = false;

        if ((mail != null && !mail.isEmpty()) ||
            (phone != null && !phone.isEmpty()) ||
            (pingIdUsername != null && !pingIdUsername.isEmpty()))
        {
            supportsReset = true;
        }

        return supportsReset;
    }

    /**
     * Resets the password for the given user.
     * If there is an error setting the password due to a policy violation, a PasswordResetException is
     * thrown, which should be recoverable (the user could try a different password).  Any other error
     * is likely a system error that is not recoverable (by trying a different password).
     *
     * @param username  The user account to reset
     * @param password  The new password to set
     *
     * @throws PasswordResetException if password reset fails
     */
    @Override
    public void	resetPassword(String username, String password) throws PasswordResetException
    {
      if (password.length() < MIN_PASSWORD_LENGTH)
      {
          // Password policy violation, throw a recoverable exception
          throw new PasswordResetException(true, "Password is too short.");
      }

      this.password = password;
    }
}
