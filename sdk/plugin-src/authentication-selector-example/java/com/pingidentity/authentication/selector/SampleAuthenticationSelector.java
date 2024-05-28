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

package com.pingidentity.authentication.selector;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pingidentity.authentication.selector.api.StateSpec;
import com.pingidentity.authentication.selector.api.SubmitEmailOrDomain;
import com.pingidentity.authentication.selector.api.ActionSpec;
import com.pingidentity.sdk.api.authn.AuthnApiPlugin;
import com.pingidentity.sdk.api.authn.common.CommonActionSpec;
import com.pingidentity.sdk.api.authn.exception.AuthnErrorException;
import com.pingidentity.sdk.api.authn.model.AuthnState;
import com.pingidentity.sdk.api.authn.spec.PluginApiSpec;
import com.pingidentity.sdk.api.authn.util.AuthnApiSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthenticationAdapter;

import com.pingidentity.sdk.AuthenticationSelector;
import com.pingidentity.sdk.AuthenticationSelectorContext;
import com.pingidentity.sdk.AuthenticationSelectorContext.ResultType;
import com.pingidentity.sdk.AuthenticationSelectorDescriptor;
import com.pingidentity.sdk.AuthenticationSourceKey;
import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.IdpAuthenticationAdapterV2;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.locale.LanguagePackMessages;
import com.pingidentity.sdk.locale.LocaleUtil;
import com.pingidentity.sdk.template.TemplateRendererUtil;
import com.pingidentity.sdk.template.TemplateRendererUtilException;

/**
 * This implementation of an authentication selector prompts the user for their email address or domain name.
 * A persistent cookie is then set in the browser to avoid prompting the user on future authentication events.
 * This sample also demonstrates how an authentication selector can be made API-capable, so that clients can interact
 * with it using the Authentication API.
 */
public class SampleAuthenticationSelector implements AuthenticationSelector, AuthnApiPlugin
{
    // HTML field names
    private static final String ERROR_MESSAGE_NAME = "errorMessageKey";
    private static final String EMAIL_INPUT_FIELD_NAME = "domainAuthnSelectorInputEmail";

    // HTML field values
    private static final String BLANK_EMAIL_ERROR = "Email address or Domain name field cannot be blank. Please try again.";
    private static final String PF_SUBMIT = "pf.submit";
    private static final String PF_CANCEL = "pf.cancel";

    // Default cookie name prefixes
    private static final String COOKIE_PREFIX = "pf-authn-selector-";

    // Configuration fields name
    private static final String FIELD_LOGIN_TEMPLATE_NAME = "Email address or Domain name template";
    private static final String FIELD_COOKIE_NAME = "Cookie Name";
    private static final String FIELD_COOKIE_AGE = "Cookie Age";

    // Configuration field default values
    private static final String PLUGIN_TYPE_NAME = "Domain Authentication Selector";
    private static final String DEFAULT_EMAIL_TEMPLATE_NAME = "sample.authn.selector.email.template.html";
    private static final String DEFAULT_COOKIE_AGE = "30";

    private static final int SECONDS_IN_A_DAY = 24 * 60 * 60;

    // Configuration field description
    private static final String DESC_EMAL_TEMPLATE_NAME = "HTML template (in <pf_home>/server/default/conf/template) to "
            + "render when a user is expected to provide an email address or a domain name. If the a email address is provided, a domain will be "
            + "extracted from the input email address. An attempt will be made to match the extracted domain with a Selector Result Value hence "
            + "resulting in the mapped authentication source.";
    private static final String DESC_COOKIE_NAME = "Name of the cookie which saves the domain name. Once the email address or domain name is provided,"
            + " upon successful authentication (or login), a cookie will be saved with this name. If left blank, a default cookie name, prefixed with "+ COOKIE_PREFIX
            + " will be generated.";
    private static final String DESC_COOKIE_AGE = "Number of days that the domain name is stored as a cookie in the browser. The cookie age is reset "
            + "upon each successful login. The default value is " + DEFAULT_COOKIE_AGE + ".";


    private Configuration configuration = null;

    // Configuration fields
    private String emailDomainHtmlTemplateFileName = null;
    private String cookieName = null;
    private int cookieAge;

    private final Log log = LogFactory.getLog(this.getClass());

    private final AuthnApiSupport apiSupport = AuthnApiSupport.getDefault();

    /**
     * This method is called by the PingFederate server to push configuration values entered by the administrator via
     * the dynamically rendered GUI configuration screen in the PingFederate administration console. This implementation
     * uses the {@link Configuration} parameter to configure its own internal state as needed.
     *
     * Whenever a new instance of this plugin implementation is created on PingFederate server, this method will be
     * invoked with the proper configuration. All concurrency issues are handled in the server so we do not have to
     * worry about them here. The server does not allow access to this plugin implementation instance until after
     * creation and configuration is completed.
     *
     * @param configuration
     *            the Configuration object constructed from the values entered by the user via the GUI.
     */
    @Override
    public void configure(Configuration configuration)
    {
        this.emailDomainHtmlTemplateFileName = configuration.getFieldValue(FIELD_LOGIN_TEMPLATE_NAME);
        this.cookieName = configuration.getFieldValue(FIELD_COOKIE_NAME);
        this.cookieAge = configuration.getIntFieldValue(FIELD_COOKIE_AGE);
        this.configuration = configuration;
    }

    /**
     * Returns the {@link PluginDescriptor} that describes this plugin to the PingFederate server. This includes how
     * PingFederate will render the plugin in the administrative console, and metadata on how PingFederate will treat
     * this plugin during runtime.
     *
     * @return A {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        GuiConfigDescriptor guiConfigDescriptor = new GuiConfigDescriptor();

        TextFieldDescriptor emailTemplateField = new TextFieldDescriptor(FIELD_LOGIN_TEMPLATE_NAME,
                DESC_EMAL_TEMPLATE_NAME);
        emailTemplateField.addValidator(new RequiredFieldValidator());
        emailTemplateField.setDefaultValue(DEFAULT_EMAIL_TEMPLATE_NAME);
        guiConfigDescriptor.addField(emailTemplateField);

        TextFieldDescriptor cookieNameField = new TextFieldDescriptor(FIELD_COOKIE_NAME,
                DESC_COOKIE_NAME);
        guiConfigDescriptor.addField(cookieNameField);

        TextFieldDescriptor cookieAgeField = new TextFieldDescriptor(FIELD_COOKIE_AGE,
                DESC_COOKIE_AGE);
        cookieAgeField.setDefaultValue(DEFAULT_COOKIE_AGE);
        guiConfigDescriptor.addField(cookieAgeField);

        AuthenticationSelectorDescriptor authSelectorDescriptor = new AuthenticationSelectorDescriptor(PLUGIN_TYPE_NAME,
                this, guiConfigDescriptor, null);
        authSelectorDescriptor.setSupportsExtendedResults(true);

        return authSelectorDescriptor;
    }

    /**
     * This is the method that the PingFederate server will invoke during Authentication Selection.
     *
     * For the current plugin implementation, a template to accept an Email address or Domain name is rendered.
     * The domain name is returned as AuthenticationSelectorContext.result, which will be later be compared to
     * one of the Selector Result Values of the current authentication selector instance. Upon a successful SSO
     * authentication, a persistent cookie is laid down in the browser to avoid prompting the user for input
     * email or domain on future SSO events.
     *
     * @param req the HttpServletRequest can be used to read cookies, parameters, headers, etc.  It can also be used
     *  to find out more about the request like the full URL the request was made to.  Accessing the HttpSession from
     *  the request is not recommended and doing so is deprecated.  Use
     * {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp the HttpServletResponse.
     * @param mappedAuthnSourcesNames the map of name value pairs containing the mapped authentication source information for the respective connection.
     * @param extraParameters the map of extra object data used for facilitating specific authentication selection implementations.  The
     * values found in this collection are stored in keys {@link AuthenticationSelector#AUTHN_REQ_DOC_PARAM_NAME} and
     * {@link AuthenticationSelector}.EXTRA_PARAMETER_*.
     * @param resumePath the relative URL that the user agent needs to return to, if the implementation of this method
     *  invocation needs to operate asynchronously.  If this method operates synchronously, this parameter can
     *  be ignored.  The resumePath is the full path portion of the URL - everything after hostname and port.  If
     *  the hostname, port, or protocol are needed, they can be derived using the HttpServletRequest.
     * @return AuthenticationSelectorContext the resulting context of the Authentication selection process.  The result type must be set to either
     * AuthenticationSelectorContext.ResultType.CONTEXT, AuthenticationSelectorContext.ResultType.ADAPTER_ID or AuthenticationSelectorContext.ResultType.IDP_CONN_ID depending on the desired behavior of the Authentication Selector.
     * If AuthenticationSelectorContext.ResultType.CONTEXT is returned, the respective mapping will be evaluated to arrive at the authentication source to be invoked.
     * If AuthenticationSelectorContext.ResultType.ADAPTER_ID is returned, the respective adapter instance will be invoked.
     * If AuthenticationSelectorContext.ResultType.IDP_CONN_ID is returned, the respective IdP connection will be invoked.
     */
    @Override
    public AuthenticationSelectorContext selectContext(HttpServletRequest req,
            HttpServletResponse resp,
            Map<AuthenticationSourceKey, String> mappedAuthnSourcesNames,
            Map<String, Object> extraParameters, String resumePath)
    {
        AuthenticationSelectorContext context = new AuthenticationSelectorContext();
        context.setResultType(ResultType.CONTEXT);
        // For the current instance of authn selector, if a cookie with the domain value exists,
        // use the cookie value to build the returned AuthenticationSelectorContext
        String cookieName = getCookieName();
        String domain = getCookieValue(cookieName, req);
        if (domain == null)
        {
            // There is no cookie with a domain name, check the request
            try
            {
                String inputEmailOrDomain = getInputEmailOrDomain(req);
                if (inputEmailOrDomain != null && !inputEmailOrDomain.isEmpty())
                {
                    // If there is an input in the request, then extract the domain from it.
                    domain = extractDomainFromEmail(inputEmailOrDomain);
                }
                else
                {
                    if (isCancelRequest(req))
                    {
                        // The cancel button was clicked
                        return context;
                    }

                    if (apiSupport.isApiRequest(req))
                    {
                        renderApiResponse(req, resp);
                    }
                    else
                    {
                        // No input email or domain in the request, render a template to allow the user to provide an email address.
                        boolean emailDomainAddressPostedBlank = inputEmailOrDomain != null && inputEmailOrDomain.isEmpty();
                        renderForm(req, resp, resumePath, emailDomainAddressPostedBlank);
                    }

                    log.debug("Rendering the response to allow the user to provide a email address or a domain name.");
                    return null;
                }
            }
            catch (AuthnErrorException e)
            {
                // An error occurred while validating an API request -- return an error response to the client.
                try
                {
                    apiSupport.writeErrorResponse(req, resp, e.getValidationError());
                }
                catch (IOException ex)
                {
                    log.error("Error occurred while writing API response", e);
                }
                return null;
            }
        }

        context.setResult(domain);

        return context;
    }

    /**
     * Check if the user provided an email address or domain.
     */
    private String getInputEmailOrDomain(HttpServletRequest req) throws AuthnErrorException
    {
        if (apiSupport.isApiRequest(req) && ActionSpec.SUBMIT_EMAIL_OR_DOMAIN.getId().equals(apiSupport.getActionId(req)))
        {
            try
            {
                SubmitEmailOrDomain model = apiSupport.deserializeAsModel(req, SubmitEmailOrDomain.class);
                return model.getEmailAddressOrDomain();
            }
            catch (IOException e)
            {
                log.error("Error deserializing API request", e);
                return null;
            }
        }
        else
        {
            return req.getParameter(EMAIL_INPUT_FIELD_NAME);
        }
    }

    /**
     * Check if the user selected 'Cancel'.
     */
    private boolean isCancelRequest(HttpServletRequest req)
    {
        if (apiSupport.isApiRequest(req))
        {
            return CommonActionSpec.CANCEL_AUTHENTICATION.getId().equals(apiSupport.getActionId(req));
        }
        else
        {
            return StringUtils.isNotBlank(req.getParameter(PF_CANCEL));
        }
    }

    /**
     * If the user has provided an email address, the domain is extracted and returned.
     * Otherwise, the provided text is returned as domain.
     * @param emailOrDomain the input email address or domain name provided by the user
     * @return a domain name
     */
    private String extractDomainFromEmail(String emailOrDomain)
    {
        String domain = null;
        String trimmedEmail = emailOrDomain.trim();
        String[] tokens = trimmedEmail.split("@");
        if (tokens.length >= 2)
        {
            domain = tokens[1];
            log.debug("Extracted the domain from input email address.");
        }
        else
        {
            domain = trimmedEmail;
        }

        return domain;
    }

    /**
     * This is a helper method that renders the template form via {@link TemplateRendererUtil} class.
     *
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. Accessing the
     *            HttpSession from the request is not recommended and doing so is deprecated. Use
     *            {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp
     *            the HttpServletResponse can be used to set cookies before continuing the SSO request.
     * @param resumeURL
     * 			  the relative URL that the user agent needs to return to, if the implementation of this method
     *            invocation needs to operate asynchronously. If this method operates synchronously, this parameter can
     *            be ignored. The resumePath is the full path portion of the URL - everything after hostname and port.
     *            If the hostname, port, or protocol are needed, they can be derived using the HttpServletRequest.

     * @param emailAddressDomainNameFieldBlank
     * 			   indicates whether the user has provided any input or not.
     */
    private void renderForm(HttpServletRequest req, HttpServletResponse resp, String resumeURL, boolean emailAddressDomainNameFieldBlank)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("url", resumeURL);
        params.put("submit", PF_SUBMIT);
        params.put("cancel", PF_CANCEL);

        // Load sample-authn-selector-email-template.properties file and store it in the map
        Locale userLocale = LocaleUtil.getUserLocale(req);
        LanguagePackMessages lpm = new LanguagePackMessages("sample-authn-selector-email-template", userLocale);
        params.put("pluginTemplateMessages", lpm);

        if (emailAddressDomainNameFieldBlank)
        {
            params.put(ERROR_MESSAGE_NAME,BLANK_EMAIL_ERROR);
        }

        try
        {
            TemplateRendererUtil.render(req, resp, this.emailDomainHtmlTemplateFileName, params);
        }
        catch (TemplateRendererUtilException e)
        {
            log.error("Error rendering the "+DEFAULT_EMAIL_TEMPLATE_NAME+" template.",e);
        }
    }

    /**
     * Write the response as JSON.
     */
    private void renderApiResponse(HttpServletRequest req, HttpServletResponse resp)
    {
        AuthnState<Void> state = StateSpec.EMAIL_OR_DOMAIN_REQUIRED.makeInstance(req, null);
        try
        {
            apiSupport.writeAuthnStateResponse(req, resp, state);
        }
        catch (IOException e)
        {
            log.error("Error writing API response", e);
        }
    }

    /**
     * This is the method that the PingFederate server will invoke after the selected adapter completes the
     * lookupAuthN(...) method. The callback method can be used to update resulting attributes from adapter invocation,
     * set cookies, etc. Writing content to the HttpServletResponse is not supported, doing so will result in unexpected
     * behavior.
     *
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. Accessing the
     *            HttpSession from the request is not recommended and doing so is deprecated. Use
     *            {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp
     *            the HttpServletResponse can be used to set cookies before continuing the SSO request.
     * @param authnIdentifiers
     *            the map of attribute values resulting from the completion of the lookupAuthN(...) method of the
     *            selected adapter.
     * @param authenticationSourceKey
     *            the ID of the selected adapter.
     * @param authnSelectorContext
     *            the resulting context of the Authentication Selection selectContext(...) method.
     *
     * @see IdpAuthenticationAdapterV2
     * @see IdpAuthenticationAdapter
     */
    @Override
    public void callback(HttpServletRequest req, HttpServletResponse resp,
            Map authnIdentifiers,
            AuthenticationSourceKey authenticationSourceKey,
            AuthenticationSelectorContext authnSelectorContext)
    {
        String validInputDomain = authnSelectorContext.getResult();
        String cookieName = getCookieName();

        if (validInputDomain != null && !validInputDomain.isEmpty())
        {
            Cookie cookie = new Cookie(cookieName, validInputDomain);
            cookie.setPath("/");
            cookie.setMaxAge(this.cookieAge * SECONDS_IN_A_DAY);
            resp.addCookie(cookie);
        }

    }

    /**
     * For an API-capable plugin, this method returns a description of the API supported by the plugin, including
     * each of the possible API states that it supports. The result of this method is used by
     * PingFederate in generating API documentation.
     *
     * @return An object that describes the selector's API.
     */
    @Override
    public PluginApiSpec getApiSpec()
    {
        return new PluginApiSpec(Arrays.asList(StateSpec.EMAIL_OR_DOMAIN_REQUIRED));
    }

    /**
     * Gets the domain cookie name for the current authn selector instance.
     *
     * @return If a cookie name was not configured for a selector instance, then a default cookie name is returned.
     * Otherwise, the configured cookie name is returned.
     *
     */
    private String getCookieName()
    {
        if (cookieName == null || cookieName.isEmpty())
        {
            String defaultCookieName = COOKIE_PREFIX +this.configuration.getId();
            return defaultCookieName;
        }
        return cookieName;
    }

    /**
     * If a cookie with name as param cookieName exists, the value of the matching cookie
     * @param cookieName, Name of the cookie.
     * @param req
     * @return
     */
    private String getCookieValue(String cookieName, HttpServletRequest req)
    {
        Cookie[] cookies = req.getCookies();
        if (cookies != null)
        {
            for (Cookie cookie : cookies)
            {
                if (cookieName.equals(cookie.getName()))
                {
                    log.debug("Domain cookie exists for the current Authentication selector.");
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
