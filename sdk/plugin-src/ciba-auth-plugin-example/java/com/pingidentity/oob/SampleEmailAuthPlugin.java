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

package com.pingidentity.oob;

import com.pingidentity.access.BaseUrlAccessor;
import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.authorizationdetails.AuthorizationDetail;
import com.pingidentity.sdk.authorizationdetails.AuthorizationDetails;
import com.pingidentity.sdk.locale.LanguagePackMessages;
import com.pingidentity.sdk.locale.LocaleUtil;
import com.pingidentity.sdk.oobauth.*;
import com.pingidentity.sdk.template.TemplateRendererUtil;
import com.pingidentity.sdk.template.TemplateRendererUtilException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jose4j.base64url.Base64Url;
import org.jose4j.lang.ByteUtil;
import org.sourceid.common.VersionUtil;
import org.sourceid.oauth20.protocol.Parameters;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.Field;
import org.sourceid.saml20.adapter.gui.AbstractSelectionFieldDescriptor;
import org.sourceid.saml20.adapter.gui.ActionDescriptor;
import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.SelectFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.ValidationException;
import org.sourceid.saml20.adapter.gui.validation.impl.EmailValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.HostnameValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.IntegerValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.state.KeyValueStateSupport;
import org.sourceid.util.log.AttributeMap;
import org.sourceid.websso.servlet.adapter.Handler;
import org.sourceid.websso.servlet.adapter.HandlerRegistry;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.pingidentity.sdk.oobauth.OOBAuthResultContext.Status.FAILURE;
import static com.pingidentity.sdk.oobauth.OOBAuthResultContext.Status.IN_PROGRESS;
import static com.pingidentity.sdk.oobauth.OOBAuthResultContext.Status.SUCCESS;

/**
 * A Sample OOBAuthPlugin implementation that sends an email to the user with a unique link
 * back to a page where they can review and approve or deny the request.
 */
@SuppressWarnings("unchecked")
public class SampleEmailAuthPlugin implements OOBAuthPlugin
{
    private final Log log = LogFactory.getLog(SampleEmailAuthPlugin.class);

    private Configuration configuration;
    private static final String CORE_ATTRIBUTE_EMAIL = "email";
    /*
    The keyValueStateSupport will hold "id -> Object[7] approvalRequestState"
    The array has a set size of 6 object elements
    approvalRequestState[0] = OOBAuthResultContext.Status status
    approvalRequestState[1] = Map<String,String> requestedScopes
    approvalRequestState[2] = Set<String> approvedScopes
    approvalRequestState[3] = String requestingApplicationName
    approvalRequestState[4] = Map<AuthorizationDetail, String> requestedAuthorizationDetails
    approvalRequestState[5] = Map<String, AuthorizationDetail> identifiersToRequestedAuthorizationDetails
    approvalRequestState[6] = Set<AuthorizationDetails> approvedAuthorizationDetails
     */
    private KeyValueStateSupport keyValueStateSupport = new KeyValueStateSupport();
    private final static int STATUS = 0;
    private final static int REQUESTED_SCOPES = 1;
    private final static int APPROVED_SCOPES = 2;
    private final static int REQUESTING_APPLICATION = 3;
    private final static int REQUESTED_AUTHORIZATION_DETAILS = 4;
    private final static int IDENTIFIERS_TO_REQUESTED_AUTHORIZATION_DETAILS = 5;
    private final static int APPROVED_AUTHORIZATION_DETAILS = 6;
    private static final String SCOPES = "scopes";
    private static final String AUTHORIZATION_DETAILS = "authorization_details";


    private static final String APPROVE_PATH = "/oob-auth/approve";
    private static final String REJECT_PATH = "/oob-auth/deny";
    private static final String OOB_REQUEST_FOR_APPROVAL_PATH = "/oob-auth/review";

    private static final String DESCRIPTION = "Configure the server to communicate with your organization's SMTP mail server.";

    private static final String FROM_TEXT_FIELD_NAME = "From Address";
    private static final String FROM_TEXT_FIELD_DESC = "The email address that appears in the 'From' header line in email messages generated by PingFederate. The address must be in valid format but need not be set up on your system.";

    private static final String EMAIL_SERVER_TEXT_FIELD_NAME = "Email Server";
    private static final String EMAIL_SERVER_TEXT_FIELD_DESC = "The IP address or hostname of your email server.";

    private static final String SMTP_PORT_TEXT_FIELD_NAME = "SMTP Port";
    private static final String SMTP_PORT_TEXT_FIELD_DESC = "The SMTP port on your email server. The default value is 25.";
    private static final String SMTP_PORT_TEXT_FIELD_DEFAULT_VALUE = "25";

    private static final String ENCRYPTION_METHOD_SELECT_FIELD_NAME = "Encryption Method";
    private static final String ENCRYPTION_METHOD_SELECT_FIELD_DESC = "Select 'None' (the default) to establish an unencrypted connection to the email server at the SMTP port.\n" +
                                                                      "Select 'SSL/TLS' to establish a secure connection to the email server at the SMTPS port.\n" +
                                                                      "Select 'STARTTLS' to establish an unencrypted connection to the email server at the SMTP port and initiate a secure channel afterward.";
    private static final String ENCRYPTION_METHOD_SELECT_FIELD_DEFAULT_VALUE = "NONE";

    private static final String SMTPS_PORT_TEXT_FIELD_NAME = "SMTPS Port";
    private static final String SMTPS_PORT_TEXT_FIELD_DESC = "The secure SMTP port on your email server. This field is not active unless SSL/TLS is the chosen encryption method. The default value is 465.";
    private static final String SMTPS_PORT_TEXT_FIELD_DEFAULT_VALUE = "465";

    private static final String USERNAME_TEXT_FIELD_NAME = "Username";
    private static final String USERNAME_TEXT_FIELD_DESC = "Authorized email account.";

    private static final String PASSWORD_TEXT_FIELD_NAME = "Password";
    private static final String PASSWORD_TEXT_FIELD_DESC = "Password for the authorized email account.";

    private static final String TEST_ADDRESS_TEXT_FIELD_NAME = "Test Address";
    private static final String TEST_ADDRESS_TEXT_FIELD_DESC = "Enter an email address the PingFederate should use to verify connectivity with the configured email server.";

    private static final String CONNECTION_TIMEOUT_TEXT_FIELD_NAME = "Connection Timeout";
    private static final String CONNECTION_TIMEOUT_TEXT_FIELD_DESC = "The amount of time in seconds that PingFederate waits before it times out connecting to the SMTP server. The default value is 30.";
    private static final String CONNECTION_TIMEOUT_TEXT_FIELD_DEFAULT_VALUE = "30";

    private static final String COMPANY_LOGO_TEXT_FIELD_NAME = "Company Logo";
    private static final String COMPANY_LOGO_TEXT_FIELD_DESC = "The company logo file used by the email templates.";
    private static final String COMPANY_LOGO_TEXT_FIELD_DEFAULT_VALUE = "company-logo.png";

    private static final String MAKE_STATUS_CHANGE_CALLBACK_FIELD_NAME = "Make Status Change Callback";
    private static final String MAKE_STATUS_CHANGE_CALLBACK_FIELD_DESC = "Indicates whether to call the OOBAuthStatusChangeReceiver on handling requests to the approve or deny URLs.";

    private static final String TEST_EMAIL_ACTION_NAME = "Test";
    private static final String TEST_EMAIL_ACTION_DESC = "Test Email Connectivity";

    private static final String EMAIL_TEST_ERROR_MESSAGE = "There was an error sending the test email message. " +
                                                           "Please ensure that the email server settings are correct.";
    private static final String EMAIL_TEST_SUCCESS_MESSAGE = "Email test succeeded.";


    /**
     * Returns the {@link PluginDescriptor} that describes this plugin to the PingFederate server. This includes how PingFederate will render the plugin in the
     * administrative console, and metadata on how PingFederate will treat this plugin at runtime.
     *
     * @return A {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        final String TYPE = "Sample Email Out-of-Band Auth";

        GuiConfigDescriptor guiDescriptor = createGuiConfigDescriptor();

        PluginDescriptor pluginDescriptor = new PluginDescriptor(TYPE, this, guiDescriptor, VersionUtil.getVersion());
        pluginDescriptor.setSupportsExtendedContract(true);
        pluginDescriptor.setAttributeContractSet(Collections.singleton(CORE_ATTRIBUTE_EMAIL));

        return pluginDescriptor;

    }

    private GuiConfigDescriptor createGuiConfigDescriptor()
    {
        GuiConfigDescriptor guiConfigDescriptor = new GuiConfigDescriptor(DESCRIPTION);

        // ***************************
        // Basic fields
        // ***************************

        // from field
        TextFieldDescriptor fromAddress = new TextFieldDescriptor(FROM_TEXT_FIELD_NAME, FROM_TEXT_FIELD_DESC);
        fromAddress.addValidator(new RequiredFieldValidator());
        fromAddress.addValidator(new EmailValidator());
        guiConfigDescriptor.addField(fromAddress);

        // mail server field
        TextFieldDescriptor mailServer = new TextFieldDescriptor(EMAIL_SERVER_TEXT_FIELD_NAME, EMAIL_SERVER_TEXT_FIELD_DESC);
        mailServer.addValidator(new RequiredFieldValidator());
        mailServer.addValidator(new HostnameValidator());
        guiConfigDescriptor.addField(mailServer);

        // smtp port field
        TextFieldDescriptor smtpPort = new TextFieldDescriptor(SMTP_PORT_TEXT_FIELD_NAME, SMTP_PORT_TEXT_FIELD_DESC);
        smtpPort.addValidator(new RequiredFieldValidator());
        smtpPort.addValidator(new IntegerValidator(0, 65535));
        smtpPort.setDefaultValue(SMTP_PORT_TEXT_FIELD_DEFAULT_VALUE);
        guiConfigDescriptor.addField(smtpPort);

        // encryption method field
        List<AbstractSelectionFieldDescriptor.OptionValue> encryptionMethodOptions = Arrays.asList(new AbstractSelectionFieldDescriptor.OptionValue("None", ENCRYPTION_METHOD_SELECT_FIELD_DEFAULT_VALUE),
                                                                                                   new AbstractSelectionFieldDescriptor.OptionValue("SSL/TLS", "SSL"),
                                                                                                   new AbstractSelectionFieldDescriptor.OptionValue("STARTTLS", "TLS"));
        SelectFieldDescriptor encryptionMethod = new SelectFieldDescriptor(ENCRYPTION_METHOD_SELECT_FIELD_NAME,
                                                                           ENCRYPTION_METHOD_SELECT_FIELD_DESC,
                                                                           encryptionMethodOptions);
        encryptionMethod.setDefaultValue(ENCRYPTION_METHOD_SELECT_FIELD_DEFAULT_VALUE);
        guiConfigDescriptor.addField(encryptionMethod);

        // smtps port field
        TextFieldDescriptor smtpsPort = new TextFieldDescriptor(SMTPS_PORT_TEXT_FIELD_NAME, SMTPS_PORT_TEXT_FIELD_DESC);
        smtpsPort.addValidator(new RequiredFieldValidator());
        smtpsPort.addValidator(new IntegerValidator(0, 65535));
        smtpsPort.setDefaultValue(SMTPS_PORT_TEXT_FIELD_DEFAULT_VALUE);
        guiConfigDescriptor.addField(smtpsPort);

        // username field
        TextFieldDescriptor username = new TextFieldDescriptor(USERNAME_TEXT_FIELD_NAME, USERNAME_TEXT_FIELD_DESC);
        guiConfigDescriptor.addField(username);

        // password field
        TextFieldDescriptor password = new TextFieldDescriptor(PASSWORD_TEXT_FIELD_NAME,
                                                               PASSWORD_TEXT_FIELD_DESC,
                                                               true);
        guiConfigDescriptor.addField(password);

        // test address field
        TextFieldDescriptor testAddress = new TextFieldDescriptor(TEST_ADDRESS_TEXT_FIELD_NAME,
                                                                  TEST_ADDRESS_TEXT_FIELD_DESC);
        testAddress.addValidator(new EmailValidator()
        {
            @Override
            public void validate(Field field) throws ValidationException
            {
                if (field.getValue() != null && !field.getValue().isEmpty())
                {
                    super.validate(field);
                }
            }

        });
        guiConfigDescriptor.addField(testAddress);

        // ***************************
        // advanced fields
        // ***************************

        // connection timeout field
        TextFieldDescriptor connectionTimeout = new TextFieldDescriptor(CONNECTION_TIMEOUT_TEXT_FIELD_NAME,
                                                                        CONNECTION_TIMEOUT_TEXT_FIELD_DESC);
        connectionTimeout.addValidator(new RequiredFieldValidator());
        connectionTimeout.addValidator(new IntegerValidator(0, 3600));
        connectionTimeout.setDefaultValue(CONNECTION_TIMEOUT_TEXT_FIELD_DEFAULT_VALUE);
        guiConfigDescriptor.addAdvancedField(connectionTimeout);

        // company logo field
        TextFieldDescriptor companyLogo = new TextFieldDescriptor(COMPANY_LOGO_TEXT_FIELD_NAME,
                                                                  COMPANY_LOGO_TEXT_FIELD_DESC);
        companyLogo.setDefaultValue(COMPANY_LOGO_TEXT_FIELD_DEFAULT_VALUE);
        guiConfigDescriptor.addAdvancedField(companyLogo);

        // make status change callback
        CheckBoxFieldDescriptor callback = new CheckBoxFieldDescriptor(MAKE_STATUS_CHANGE_CALLBACK_FIELD_NAME, MAKE_STATUS_CHANGE_CALLBACK_FIELD_DESC);
        callback.setDefaultValue(false);
        guiConfigDescriptor.addAdvancedField(callback);

        //Actions
        // Test settings action
        ActionDescriptor.Action testSettingsAction = (ActionDescriptor.Action) configuration -> {

            // get test address value
            String testAddressValue = configuration.getFieldValue(TEST_ADDRESS_TEXT_FIELD_NAME);
            if (StringUtils.isBlank(testAddressValue))
            {
                return String.format("%s is required to perform %s action.",
                                     TEST_ADDRESS_TEXT_FIELD_NAME,
                                     TEST_EMAIL_ACTION_DESC);
            }

            EmailServerSettings emailServerSettings = getEmailServerSettings(configuration);

            if (sendTestEmail(emailServerSettings))
            {
                return EMAIL_TEST_SUCCESS_MESSAGE;
            }
            else
            {
                return EMAIL_TEST_ERROR_MESSAGE;
            }

        };
        guiConfigDescriptor.addAction(new ActionDescriptor(TEST_EMAIL_ACTION_NAME,
                                                           TEST_EMAIL_ACTION_DESC,
                                                           testSettingsAction));

        return guiConfigDescriptor;
    }

    private static EmailServerSettings getEmailServerSettings(Configuration configuration)
    {
        return new EmailServerSettings()
                .setEmailServer(configuration.getFieldValue(EMAIL_SERVER_TEXT_FIELD_NAME))
                .setConnectionTimeout(configuration.getFieldValue(CONNECTION_TIMEOUT_TEXT_FIELD_NAME))
                .setFromAddress(configuration.getFieldValue(FROM_TEXT_FIELD_NAME))
                .setPassword(configuration.getFieldValue(PASSWORD_TEXT_FIELD_NAME))
                .setSmtpPort(configuration.getFieldValue(SMTP_PORT_TEXT_FIELD_NAME))
                .setSmtpsPort(configuration.getFieldValue(SMTPS_PORT_TEXT_FIELD_NAME))
                .setTestAddress(configuration.getFieldValue(TEST_ADDRESS_TEXT_FIELD_NAME))
                .setUsername(configuration.getFieldValue(USERNAME_TEXT_FIELD_NAME))
                .setUseSSLEncryption("SSL".equals(configuration.getFieldValue(ENCRYPTION_METHOD_SELECT_FIELD_NAME)))
                .setUseSTARTTLSEncryption("TLS".equals(configuration.getFieldValue(ENCRYPTION_METHOD_SELECT_FIELD_NAME)));
    }

    private boolean sendTestEmail(EmailServerSettings emailServerSettings)
    {
        Session session = getSession(emailServerSettings);

        try
        {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailServerSettings.fromAddress));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailServerSettings.testAddress));
            message.setSubject("Test Message from Your PingFederate Server!");
            String testEmailContent = "<p>Hello,</p>" +
                                      "<p>" +
                                      "  This is a test message from PingFederate verifying that your email configurations have been set up properly." +
                                      "</p> " +
                                      "<p>" +
                                      "  If you received this message in error, please contact your system administrator." +
                                      "</p>" +
                                      "<p>Best Regards, </p>" +
                                      "<p>Your PingFederate Team </p>";
            message.setContent(testEmailContent, "text/html");
            Transport.send(message);
        }
        catch (MessagingException mex)
        {
            log.warn("Problem sending email", mex);
            return false;
        }

        return true;
    }

    private static Session getSession(EmailServerSettings emailServerSettings)
    {
        Properties properties = new Properties();

        properties.setProperty("mail.host", emailServerSettings.emailServer);
        properties.setProperty("mail.user", emailServerSettings.username);

        properties.put("mail.smtp.auth", "true");

        // SSL / TLS
        if (emailServerSettings.useSSLEncryption)
        {
            properties.put("mail.smtp.ssl.enable", "true");
            properties.put("mail.smtp.ssl.socketFactory.port", emailServerSettings.smtpsPort);
            properties.put("mail.smtp.ssl.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.ssl.socketFactory.fallback", "false");
        }
        // Start TLS
        else if (emailServerSettings.useSTARTTLSEncryption)
        {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.socketFactory.port", emailServerSettings.smtpsPort);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "false");
        }
        // No encryption
        else
        {
            properties.put("mail.smtp.port", emailServerSettings.smtpPort);
        }

        if (emailServerSettings.connectionTimeout != null)
        {
            properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(Integer.parseInt(emailServerSettings.connectionTimeout) * 1000));
        }

        return Session.getInstance(properties, new Authenticator()
        {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(emailServerSettings.username, emailServerSettings.password);
            }
        });
    }

    private static void sendEmail(EmailServerSettings emailServerSettings, String email, String requestForApprovalPath,
                                  String userVerificationMessage, String requestingApplication, Locale locale)
            throws MessagingException
    {
        LanguagePackMessages lpm = getLanguagePackMessages(locale);

        StringBuilder content = new StringBuilder();
        content.append("<p>")
               .append(lpm.getMessage("email.message", new String[]{requestingApplication}))
               .append("</p>")
               .append("<p>")
               .append("<a href=\"").append(requestForApprovalPath).append("\" >").append(requestForApprovalPath).append("</a>")
               .append("</p>");

        if (userVerificationMessage != null && !userVerificationMessage.isEmpty())
        {
            userVerificationMessage = Utils.escapeForHtml(userVerificationMessage);
            content.append("<p>")
                   .append(lpm.getMessage("email.verification.msg", new String[]{userVerificationMessage}))
                   .append("</p>");
        }

        Session session = getSession(emailServerSettings);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailServerSettings.fromAddress));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
        message.setSubject(lpm.getMessage("email.subject"));
        message.setContent(content.toString(), "text/html");
        Transport.send(message);
    }


    public static class EmailServerSettings
    {
        private String fromAddress;
        private String emailServer;
        private String smtpPort;
        private boolean useSSLEncryption;
        private boolean useSTARTTLSEncryption;
        private String smtpsPort = "465";
        private String username;
        private String password;
        private String testAddress;
        //Advanced Fields
        private String connectionTimeout = CONNECTION_TIMEOUT_TEXT_FIELD_DEFAULT_VALUE;

        EmailServerSettings setFromAddress(String fromAddress)
        {
            this.fromAddress = fromAddress;
            return this;
        }

        EmailServerSettings setEmailServer(String emailServer)
        {
            this.emailServer = emailServer;
            return this;
        }

        EmailServerSettings setSmtpPort(String smtpPort)
        {
            this.smtpPort = smtpPort;
            return this;
        }

        EmailServerSettings setUseSSLEncryption(boolean useSSLEncryption)
        {
            this.useSSLEncryption = useSSLEncryption;
            return this;
        }

        EmailServerSettings setUseSTARTTLSEncryption(boolean useSTARTTLSEncryption)
        {
            this.useSTARTTLSEncryption = useSTARTTLSEncryption;
            return this;
        }

        EmailServerSettings setSmtpsPort(String smtpsPort)
        {
            this.smtpsPort = smtpsPort;
            return this;
        }

        EmailServerSettings setUsername(String username)
        {
            this.username = username;
            return this;
        }

        EmailServerSettings setPassword(String password)
        {
            this.password = password;
            return this;
        }

        EmailServerSettings setTestAddress(String testAddress)
        {
            this.testAddress = testAddress;
            return this;
        }

        EmailServerSettings setConnectionTimeout(String connectionTimeout)
        {
            this.connectionTimeout = connectionTimeout;
            return this;
        }
    }

    @Override
    public void configure(Configuration configuration)
    {
        this.configuration = configuration;
        HandlerRegistry.registerHandler(OOB_REQUEST_FOR_APPROVAL_PATH, new OOBRequestForApprovalHandler());
        HandlerRegistry.registerHandler(APPROVE_PATH, new ApproveHandler());
        HandlerRegistry.registerHandler(REJECT_PATH, new DenyHandler());

    }

    public class OOBRequestForApprovalHandler implements Handler
    {
        @Override
        public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException
        {
            String xssSafeId = Utils.escapeForHtml(req.getParameter("v"));
            String baseUrl = BaseUrlAccessor.getCurrentBaseUrl();
            String callback = baseUrl + "/ext";
            String param = "?v=" + xssSafeId;

            StringBuilder allowAction = new StringBuilder();
            allowAction.append("\"").append(callback).append(APPROVE_PATH).append(param).append("\"");
            StringBuilder denyAction = new StringBuilder();
            denyAction.append("\"").append(callback).append(REJECT_PATH).append(param).append("\"");

            String id = req.getParameter("v");
            Object[] approvalRequestState = (Object[]) keyValueStateSupport.getValue(id);
            if (approvalRequestState == null)
            {
                renderPage(req, resp, "request.approval.error.response.page.html", null);

                return;
            }

            Map<String, Object> params = new HashMap<>();
            Map<String, String> scopeDescriptions = (Map<String, String>) approvalRequestState[REQUESTED_SCOPES];

            Map<AuthorizationDetail, String> authorizationDetailToDescription = (Map<AuthorizationDetail, String>) approvalRequestState[REQUESTED_AUTHORIZATION_DETAILS];
            Map<String, AuthorizationDetail> identifiersToAuthorizationDetails = new HashMap<>();
            Map<String, String> identifiersToDescriptions = generateIdentifiersForAuthorizationDetails(authorizationDetailToDescription,
                                                                                                       identifiersToAuthorizationDetails);

            approvalRequestState[IDENTIFIERS_TO_REQUESTED_AUTHORIZATION_DETAILS] = identifiersToAuthorizationDetails;
            keyValueStateSupport.setValue(id, approvalRequestState);

            params.put("scopeDescriptions", scopeDescriptions);
            params.put("authorizationDetailsDescriptions", identifiersToDescriptions);
            params.put("allowAction", allowAction.toString());
            params.put("denyAction", denyAction.toString());
            params.put(Parameters.CLIENT_ID, approvalRequestState[REQUESTING_APPLICATION]);

            renderPage(req, resp, "request.approval.page.html", params);
        }
    }

    /**
     * This is a helper method to map authorization details to a random identifiers, so not to expose the full
     * authorization detail on the consent form.
     *
     * @param authorizationDetailToDescription
     *             the authorization detail and their descriptions.
     *
     * @param identifiersToAuthorizationDetails
     *              a map to populate the identifiers to authorization details for lookups on consent approval.
     * @return
     *              a mapping of identifiers to descriptions
     */
    private Map<String, String> generateIdentifiersForAuthorizationDetails(Map<AuthorizationDetail, String> authorizationDetailToDescription,
                                                                           Map<String, AuthorizationDetail> identifiersToAuthorizationDetails)
    {
        Map<String, String> idToDescriptions = new HashMap<>();

        for (Map.Entry<AuthorizationDetail, String> entry: authorizationDetailToDescription.entrySet())
        {
            String randomId = getNextId(identifiersToAuthorizationDetails);
            identifiersToAuthorizationDetails.put(randomId, entry.getKey());
            idToDescriptions.put(randomId, entry.getValue());
        }

        return idToDescriptions;
    }

    /**
     * Helper method to generate a random integer string identifier between 10,000 and 99,999 inclusive.
     *
     * @param idToAuthorizationDetails
     *              map of existing identifiers associated with authorization details.
     * @return the next random int string identifier.
     */
    private String getNextId(Map<String, AuthorizationDetail> idToAuthorizationDetails)
    {
        int randomId;
        do
        {
            randomId = ThreadLocalRandom.current().nextInt(10000, 100000);
        } while (idToAuthorizationDetails.containsKey(Integer.toString(randomId)));

        return Integer.toString(randomId);
    }

    /**
     * Helper method to retrieve the authorization details associated with the identifiers which were approved
     * on the consent form.
     *
     * @param identifiers
     *            the random int string identifiers that were previously assigned and stored in session and
     *            associated to authorization details.
     * @param identifiersToAuthorizationDetails
     *            the identifiers to authorization details mapping
     * @return
     */
    private Set<AuthorizationDetail> retrieveAuthorizationDetailsByIdentifier(String[] identifiers,
                                                                              Map<String, AuthorizationDetail> identifiersToAuthorizationDetails)
    {
        return Arrays.stream(identifiers)
                     .distinct()
                     .map(identifiersToAuthorizationDetails::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toSet());
    }

    private LanguagePackMessages getLanguagePackMessages(HttpServletRequest req)
    {
        // Load request-approval-page.properties file and store it in the map.
        // Note on internationalization, if a language properties file like request-approval-page_fr.properties
        // is created then a corresponding PingFederate properties needs to also be created (i.e. pingfederate-messages_fr.properties).
        Locale userLocale = LocaleUtil.getUserLocale(req);
        return getLanguagePackMessages(userLocale);
    }

    private static LanguagePackMessages getLanguagePackMessages(Locale userLocale)
    {
        return new LanguagePackMessages("request-approval-page", userLocale);
    }


    public class ApproveHandler implements Handler
    {
        OOBAuthStatusChangeReceiver callbackReceiver = new OOBAuthStatusChangeReceiver();

        @Override
        public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException
        {
            String id = req.getParameter("v");
            Object[] approvalRequestState = (Object[]) keyValueStateSupport.getValue(id);
            OOBAuthResultContext.Status status =
                    approvalRequestState != null ? (OOBAuthResultContext.Status) approvalRequestState[STATUS] : null;

            if (status == IN_PROGRESS)
            {
                Set<String> approvedScopes = (Set<String>) approvalRequestState[APPROVED_SCOPES];
                if (req.getParameterValues(SCOPES) != null)
                {
                    Collections.addAll(approvedScopes, req.getParameterValues(SCOPES));
                }
                Set<AuthorizationDetail> approvedAuthorizationDetails = (Set<AuthorizationDetail>) approvalRequestState[APPROVED_AUTHORIZATION_DETAILS];
                if (req.getParameterValues(AUTHORIZATION_DETAILS) != null)
                {
                    Map<String, AuthorizationDetail> identifiersToAuthorizationDetails
                            = (Map<String, AuthorizationDetail>) approvalRequestState[IDENTIFIERS_TO_REQUESTED_AUTHORIZATION_DETAILS];
                    Set<AuthorizationDetail> approvedDetails = retrieveAuthorizationDetailsByIdentifier(req.getParameterValues(AUTHORIZATION_DETAILS),
                                                                                                        identifiersToAuthorizationDetails);
                    approvedAuthorizationDetails.addAll(approvedDetails);
                }
                approvalRequestState[STATUS] = SUCCESS;
                keyValueStateSupport.setValue(id, approvalRequestState);
                if (configuration.getBooleanFieldValue(MAKE_STATUS_CHANGE_CALLBACK_FIELD_NAME))
                {
                    callbackReceiver.statusChange(id);
                }

                renderPage(req, resp, "request.approval.approve.response.page.html", null);
            }
            else
            {
                renderPage(req, resp, "request.approval.error.response.page.html", null);
            }
        }
    }

    public class DenyHandler implements Handler
    {
        OOBAuthStatusChangeReceiver callbackReceiver = new OOBAuthStatusChangeReceiver();

        @Override
        public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException
        {
            String id = req.getParameter("v");
            Object[] approvalRequestState = (Object[]) keyValueStateSupport.getValue(id);
            OOBAuthResultContext.Status status = approvalRequestState != null
                                                 ? (OOBAuthResultContext.Status) approvalRequestState[STATUS]
                                                 : null;

            if (status == IN_PROGRESS)
            {
                approvalRequestState[STATUS] = FAILURE;
                keyValueStateSupport.setValue(id, approvalRequestState);
                if (configuration.getBooleanFieldValue(MAKE_STATUS_CHANGE_CALLBACK_FIELD_NAME))
                {
                    callbackReceiver.statusChange(id);
                }

                renderPage(req, resp, "request.approval.deny.response.page.html", null);
            }
            else
            {
                renderPage(req, resp, "request.approval.error.response.page.html", null);
            }
        }
    }

    private void renderPage(HttpServletRequest req, HttpServletResponse resp, String templateFile, Map<String, Object> params)
            throws ServletException
    {
        params = params == null ? new HashMap<>() : params;
        LanguagePackMessages lpm = getLanguagePackMessages(req);
        params.put("pluginTemplateMessages", lpm);
        try
        {
            TemplateRendererUtil.render(req, resp, templateFile, params);
        }
        catch (TemplateRendererUtilException e)
        {
            throw new ServletException("Failed to render page from template " + templateFile, e);
        }
    }

    @Override
    public OOBAuthTransactionContext initiate(OOBAuthRequestContext requestContext, Map<String, Object> inParameters)
            throws UnknownUserException, UserAuthBindingMessageException, OOBAuthGeneralException
    {
        String userAuthBindingMsg = requestContext.getUserAuthBindingMessage();
        int maxLength = 100; // an arbitrary limit on the length of the binding message
        if (userAuthBindingMsg != null && userAuthBindingMsg.length() > maxLength)
        {
            UserAuthBindingMessageException uabme = new UserAuthBindingMessageException("Binding message shouldn't exceed " + maxLength + " characters.");
            uabme.setAllowMessagePropagation(true);
            throw uabme;
        }

        log.info("initiate was called w/ " + requestContext);
        Map<String, String> requestedScopes = new HashMap<>(requestContext.getRequestedScope());
        Map<AuthorizationDetail, String> requestedAuthorizationDetails = new HashMap<>(requestContext.getRequestedAuthorizationDetails());

        AttributeMap userAttributes = requestContext.getUserAttributes();
        AttributeValue attributeValue = userAttributes.get(CORE_ATTRIBUTE_EMAIL);
        if (attributeValue == null || attributeValue.getValue() == null)
        {
            throw new UnknownUserException("No attribute provided for " + CORE_ATTRIBUTE_EMAIL);
        }

        String emailAddress = attributeValue.getValue();

        String id = Base64Url.encode(ByteUtil.randomBytes(18));

        Object[] approvalRequestState = new Object[7];
        approvalRequestState[STATUS] = IN_PROGRESS;
        approvalRequestState[REQUESTED_SCOPES] = requestedScopes;
        approvalRequestState[APPROVED_SCOPES] = new HashSet<>();
        approvalRequestState[REQUESTING_APPLICATION] = requestContext.getRequestingApplication().getName();
        approvalRequestState[REQUESTED_AUTHORIZATION_DETAILS] = requestedAuthorizationDetails;
        approvalRequestState[APPROVED_AUTHORIZATION_DETAILS] = new HashSet<>();
        keyValueStateSupport.setValue(id, approvalRequestState);

        OOBAuthTransactionContext context = new OOBAuthTransactionContext();
        context.setTransactionIdentifier(id);

        String baseUrl = BaseUrlAccessor.getCurrentBaseUrl();
        String callback = baseUrl + "/ext";
        String param = "?v=" + id;

        context.setStatusChangeCallbackCapable(configuration.getBooleanFieldValue(MAKE_STATUS_CHANGE_CALLBACK_FIELD_NAME));

        //send email
        String requestForApprovalPath = callback + OOB_REQUEST_FOR_APPROVAL_PATH + param;
        try
        {
            String requestingAppName = requestContext.getRequestingApplication().getName();
            EmailServerSettings settings = getEmailServerSettings(configuration);
            Locale locale = requestContext.getLocale();
            sendEmail(settings, emailAddress, requestForApprovalPath, userAuthBindingMsg, requestingAppName, locale);
        }
        catch (MessagingException e)
        {
            throw new OOBAuthGeneralException("Problem sending email: " + e.getMessage(), e);
        }

        log.info("returning from initiate " + context);

        return context;
    }

    @Override
    public OOBAuthResultContext check(String transactionIdentifier, Map<String, Object> inParameters)
    {
        Object[] approvalRequestState = (Object[]) keyValueStateSupport.getValue(transactionIdentifier);
        OOBAuthResultContext.Status status = (OOBAuthResultContext.Status) approvalRequestState[STATUS];
        Set<String> approvedScopes = (Set<String>) approvalRequestState[APPROVED_SCOPES];
        Set<AuthorizationDetail> approvedAuthorizationDetails = (Set<AuthorizationDetail>) approvalRequestState[APPROVED_AUTHORIZATION_DETAILS];
        OOBAuthResultContext context = new OOBAuthResultContext();
        context.setStatus(status == null ? FAILURE : status);
        if (status == SUCCESS)
        {
            context.setApprovedScope(approvedScopes);
            context.setApprovedAuthorizationDetails(new AuthorizationDetails(approvedAuthorizationDetails.toArray(new AuthorizationDetail[]{})));
        }

        log.info("check called w/ " + transactionIdentifier + " returning " + context);
        return context;
    }

    @Override
    public void finished(String transactionIdentifier)
    {
        log.info("finished called w/ " + transactionIdentifier);
        keyValueStateSupport.removeValue(transactionIdentifier);
    }

}