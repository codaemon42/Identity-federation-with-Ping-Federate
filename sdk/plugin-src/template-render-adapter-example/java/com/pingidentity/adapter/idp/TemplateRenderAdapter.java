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

package com.pingidentity.adapter.idp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.pingidentity.adapter.idp.api.ErrorDetailSpec;
import com.pingidentity.adapter.idp.api.StateSpec;
import com.pingidentity.adapter.idp.api.SubmitUserAttributes;
import com.pingidentity.adapter.idp.api.ActionSpec;
import com.pingidentity.adapter.idp.api.UserAttributesRequired;
import com.pingidentity.sdk.api.authn.AuthnApiPlugin;
import com.pingidentity.sdk.api.authn.common.CommonErrorDetailSpec;
import com.pingidentity.sdk.api.authn.common.CommonErrorSpec;
import com.pingidentity.sdk.api.authn.common.CommonActionSpec;
import com.pingidentity.sdk.api.authn.exception.AuthnErrorException;
import com.pingidentity.sdk.api.authn.model.AuthnError;
import com.pingidentity.sdk.api.authn.model.AuthnErrorDetail;
import com.pingidentity.sdk.api.authn.model.AuthnState;
import com.pingidentity.sdk.api.authn.spec.PluginApiSpec;
import com.pingidentity.sdk.api.authn.util.AuthnApiSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sourceid.saml20.adapter.AuthnAdapterException;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.idp.authn.AuthnPolicy;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthenticationAdapter;
import org.sourceid.saml20.adapter.idp.authn.IdpAuthnAdapterDescriptor;

import com.pingidentity.sdk.AuthnAdapterResponse;
import com.pingidentity.sdk.AuthnAdapterResponse.AUTHN_STATUS;
import com.pingidentity.sdk.IdpAuthenticationAdapterV2;
import com.pingidentity.sdk.locale.LanguagePackMessages;
import com.pingidentity.sdk.locale.LocaleUtil;
import com.pingidentity.sdk.template.TemplateRendererUtil;
import com.pingidentity.sdk.template.TemplateRendererUtilException;

/**
 * 
 * This class is an example of an IdP adapter that demonstrates the use of the Velocity Template Render {@link TemplateRendererUtil}
 * This sample also demonstrates how an IdP adapter can be made API-capable, so that clients can interact with it using
 * the Authentication API.
 * 
 * This adapter is meant to be mapped in an SP connection that when invoked will present end users with a form prompting
 * input for a 'username' and any 'extended attributes' configured for the adapter within the Administrative Console.
 * 
 * This class does not handle any session state support, adapter chaining or input validation.
 * 
 */
public class TemplateRenderAdapter implements IdpAuthenticationAdapterV2, AuthnApiPlugin
{

    private static final Logger log = LogManager.getLogger(TemplateRenderAdapter.class);

    private AuthnApiSupport apiSupport = AuthnApiSupport.getDefault();

    private static final String USERNAME = "username";
    private static final String FORM_TEMPLATE_NAME_FIELD = "HTML Form Template Name";
   
    // Fields
    private IdpAuthnAdapterDescriptor descriptor = null;
    private Configuration configuration = null;
    private Set<String> extendedAttr = null;
    

    /**
     * Constructor for the Sample Template Render Adapter. Initializes the adapter descriptor so PingFederate can
     * generate the proper configuration GUI
     */
    public TemplateRenderAdapter()
    {
        // Create input text field to represent name of velocity html template file
        TextFieldDescriptor formTemplateConfig = new TextFieldDescriptor(FORM_TEMPLATE_NAME_FIELD, "HTML template (in <pf_home>/server/default/conf/template) to render for form submission. The default value is attribute.form.template.html.");
        formTemplateConfig.setDefaultValue("attribute.form.template.html");
        formTemplateConfig.addValidator(new RequiredFieldValidator());
        
        // Create an adapter GUI descriptor
        AdapterConfigurationGuiDescriptor configurationGuiDescriptor = new AdapterConfigurationGuiDescriptor("Template Render Adapter");
        configurationGuiDescriptor.addField(formTemplateConfig);
        
        // Create an Idp adapter descriptor 
        Set<String> attributeContract = new HashSet<String>();
        attributeContract.add(USERNAME);
        this.descriptor = new IdpAuthnAdapterDescriptor(this, "Template Render Adapter", attributeContract, true, configurationGuiDescriptor, false);
    }
    
    /**
     * This method is called by the PingFederate server to push configuration values entered by the administrator via
     * the dynamically rendered GUI configuration screen in the PingFederate administration console. Your implementation
     * should use the {@link Configuration} parameter to configure its own internal state as needed. The tables and
     * fields available in the Configuration object will correspond to the tables and fields defined on the
     * {@link org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor} on the AuthnAdapterDescriptor returned
     * by the {@link #getAdapterDescriptor()} method of this class. <br/>
     * <br/>
     * Each time the PingFederate server creates a new instance of your adapter implementation this method will be
     * invoked with the proper configuration. All concurrency issues are handled in the server so you don't need to
     * worry about them here. The server doesn't allow access to your adapter implementation instance until after
     * creation and configuration is completed.
     * 
     * @param config
     *            the Configuration object constructed from the values entered by the user via the GUI.
     */ 
    @Override
    public void configure(Configuration config) 
    {
        this.configuration = config;
        this.extendedAttr = config.getAdditionalAttrNames();            
    }

    /**
     * The PingFederate server will invoke this method on your adapter implementation to discover metadata about the
     * implementation. This included the adapter's attribute contract and a description of what configuration fields to
     * render in the GUI. <br/>
     * <br/>
     * 
     * @return an IdpAuthnAdapterDescriptor object that describes this IdP adapter implementation.
     */
    @Override
    public IdpAuthnAdapterDescriptor getAdapterDescriptor() 
    {
        return this.descriptor;
    }

     /**
     * This is an extended method that the PingFederate server will invoke during processing of a single sign-on
     * transaction to lookup information about an authenticated security context or session for a user at the external
     * application or authentication provider service.
     * <p>
     * In this example, the adapter simply saves the username and extended attribute values into a Map that is put into
     * its response.  It also calls a helper method to render the input form.
     * </p>
     * 
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. It can also be used to
     *            find out more about the request like the full URL the request was made to. Accessing the HttpSession
     *            from the request is not recommended and doing so is deprecated. Use
     *            {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp
     *            the HttpServletResponse. The response can be used to facilitate an asynchronous interaction. Sending a
     *            client side redirect or writing (and flushing) custom content to the response are two ways that an
     *            invocation of this method allows for the adapter to take control of the user agent. Note that if
     *            control of the user agent is taken in this way, then the agent must eventually be returned to the
     *            <code>resumePath</code> endpoint at the PingFederate server to complete the protocol transaction.
     * @param inParameters
     *            A map that contains a set of input parameters. The input parameters provided are detailed in
     *            {@link IdpAuthenticationAdapterV2}, prefixed with <code>IN_PARAMETER_NAME_*</code> i.e.
     *            {@link IdpAuthenticationAdapterV2#IN_PARAMETER_NAME_RESUME_PATH}.
     * @return {@link AuthnAdapterResponse} The return value should not be null.
     * @throws AuthnAdapterException
     *             for any unexpected runtime problem that the implementation cannot handle.
     * @throws IOException
     *             for any problem with I/O (typically any operation that writes to the HttpServletResponse).
     */
    @Override
    public AuthnAdapterResponse lookupAuthN(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> inParameters) throws AuthnAdapterException, IOException 
    {
        AuthnAdapterResponse authnAdapterResponse = new AuthnAdapterResponse();

        try
        {
            if (isSubmitAttributesRequest(req))
            {
                SubmitUserAttributes submitted = getSubmittedAttributes(req);
                validateSubmittedAttributes(req, submitted);

                // Handle Submit
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                attributeMap.put(USERNAME, submitted.getUsername());

                for (String key : extendedAttr)
                {
                    attributeMap.put(key, submitted.getUserAttributes().get(key));
                }

                // Set the authn instant to the current time.
                attributeMap.put(AUTHN_INSTANT_ATTRIBUTE_NAME, System.currentTimeMillis());

                authnAdapterResponse.setAttributeMap(attributeMap);
                authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.SUCCESS);
                return authnAdapterResponse;
            }

            if (isCancelRequest(req))
            {
                // Handle Cancel
                authnAdapterResponse.setErrorMessage("User clicked Cancel");
                authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.FAILURE);
                return authnAdapterResponse;
            }

            if (apiSupport.getActionId(req) != null)
            {
                // An action ID was provided but it does not match one of those expected in the current state.
                throw new AuthnErrorException(CommonErrorSpec.INVALID_ACTION_ID.makeInstance());
            }
        }
        catch (AuthnErrorException e)
        {
            // A validation error occurred while processing an API request, return an error response to the API client
            apiSupport.writeErrorResponse(req, resp, e.getValidationError());
            authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.IN_PROGRESS);
            return authnAdapterResponse;
        }

        // Render the response
        renderResponse(req, resp, inParameters);
        authnAdapterResponse.setAuthnStatus(AUTHN_STATUS.IN_PROGRESS);
        return authnAdapterResponse;
    }

    /**
     * Retrieve the submitted username and user attributes from the request. This method handles both API requests and
     * form submissions.
     *
     * @param req
     *             The servlet request.
     * @return
     *             An object containing the submitted username and user attributes from the request.
     * @throws AuthnErrorException
     *             For an API request, this exception is thrown if an error occurs while converting the
     *             JSON request body to the expected model. This exception may be thrown if a JSON field is not
     *             recognized or if the JSON field type does not match the type of the field in the model. It may also
     *             be thrown if a field specified as required (using the @Schema annotation) was not provided.
     * @throws AuthnAdapterException
     *             If an unexpected error occurs while trying to retrieve fields from the request.
     */
    private SubmitUserAttributes getSubmittedAttributes(HttpServletRequest req) throws AuthnErrorException, AuthnAdapterException
    {
        if (apiSupport.isApiRequest(req))
        {
            try
            {
                return apiSupport.deserializeAsModel(req, SubmitUserAttributes.class);
            }
            catch (IOException e)
            {
                throw new AuthnAdapterException(e);
            }
        }
        else
        {
            SubmitUserAttributes result = new SubmitUserAttributes();
            result.setUsername(req.getParameter("username"));

            for (String key : extendedAttr)
            {
                result.getUserAttributes().put(key, req.getParameter(key));
            }
            return result;
        }
    }

    /**
     * Determine if the user chose "Submit".
     */
    private boolean isSubmitAttributesRequest(HttpServletRequest req)
    {
        if (apiSupport.isApiRequest(req))
        {
            return ActionSpec.SUBMIT_USER_ATTRIBUTES.getId().equals(apiSupport.getActionId(req));
        }
        return StringUtils.isNotBlank(req.getParameter("pf.submit"));
    }

    /**
     * Determine if the user chose "Cancel".
     */
    private boolean isCancelRequest(HttpServletRequest req)
    {
        if (apiSupport.isApiRequest(req))
        {
            return CommonActionSpec.CANCEL_AUTHENTICATION.getId().equals(apiSupport.getActionId(req));
        }
        return StringUtils.isNotBlank(req.getParameter("pf.cancel"));
    }

    /**
     * For API requests, validate that all of the submitted attribute names are valid.
     * This validation is only needed for API requests. For form posts, this adapter generated the form, so the
     * form fields should all be valid.
     */
    private void validateSubmittedAttributes(HttpServletRequest req, SubmitUserAttributes submitted) throws AuthnErrorException
    {
        if (apiSupport.isApiRequest(req))
        {
            List<AuthnErrorDetail> errorDetails = new ArrayList<>();
            for (String attrName : submitted.getUserAttributes().keySet())
            {
                if (!extendedAttr.contains(attrName))
                {
                    errorDetails.add(ErrorDetailSpec.INVALID_ATTRIBUTE_NAME.makeInstanceBuilder()
                            .message("Invalid attribute name: " + attrName).build());
                }
            }
            if (!errorDetails.isEmpty())
            {
                AuthnError authnError = CommonErrorSpec.VALIDATION_ERROR.makeInstance();
                authnError.setDetails(errorDetails);
                throw new AuthnErrorException(authnError);
            }
        }
    }

    /**
     * 
     * This is a helper method that renders the template form via {@link TemplateRendererUtil} class.
     * 
     *@param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. It can also be used to
     *            find out more about the request like the full URL the request was made to. Accessing the HttpSession
     *            from the request is not recommended and doing so is deprecated. Use
     *            {@link org.sourceid.saml20.adapter.state.SessionStateSupport} as an alternative.
     * @param resp
     *            the HttpServletResponse. The response can be used to facilitate an asynchronous interaction. Sending a
     *            client side redirect or writing (and flushing) custom content to the response are two ways that an
     *            invocation of this method allows for the adapter to take control of the user agent. Note that if
     *            control of the user agent is taken in this way, then the agent must eventually be returned to the
     *            <code>resumePath</code> endpoint at the PingFederate server to complete the protocol transaction.
     * @param inParameters
     *            A map that contains a set of input parameters. The input parameters provided are detailed in
     *            {@link IdpAuthenticationAdapterV2}, prefixed with <code>IN_PARAMETER_NAME_*</code> i.e.
     *            {@link IdpAuthenticationAdapterV2#IN_PARAMETER_NAME_RESUME_PATH}.
     * @throws AuthnAdapterException
     *             for any unexpected runtime problem that the implementation cannot handle.
     */
    private void renderResponse(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> inParameters) throws AuthnAdapterException
    {
        if (apiSupport.isApiRequest(req))
        {
            renderApiResponse(req, resp, inParameters);
        }
        else
        {
            renderFormResponse(req, resp, inParameters);
        }
    }

    /**
     * Render the response as JSON.
     */
    private void renderApiResponse(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> inParameters) throws AuthnAdapterException
    {
        UserAttributesRequired model = new UserAttributesRequired();
        model.setAttributeNames(new ArrayList<>(extendedAttr));
        AuthnState<UserAttributesRequired> authnState = apiSupport.makeAuthnState(req, StateSpec.USER_ATTRIBUTES_REQUIRED, model);
        try
        {
            apiSupport.writeAuthnStateResponse(req, resp, authnState);
        }
        catch (IOException e)
        {
            throw new AuthnAdapterException(e);
        }
    }

    /**
     * Render the response as an HTML form.
     */
    private void renderFormResponse(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> inParameters) throws AuthnAdapterException
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("extendedAttr", extendedAttr);
        params.put("resumePath", inParameters.get(IN_PARAMETER_NAME_RESUME_PATH));
        params.put("submit", "pf.submit");
        params.put("cancel", "pf.cancel");

        // Load attribute-form-template.properties file and store it in the map.
        // Note on internationalization, if a language properties file like attribute-from-template_fr.properties
        // is created then a corresponding PingFederate properties needs to also be created (i.e. pingfederate-messages_fr.properties).
        Locale userLocale = LocaleUtil.getUserLocale(req);
        LanguagePackMessages lpm = new LanguagePackMessages("attribute-form-template", userLocale);
        params.put("pluginTemplateMessages", lpm);

        try
        {
            TemplateRendererUtil.render(req, resp, configuration.getFieldValue(FORM_TEMPLATE_NAME_FIELD), params);
        }
        catch (TemplateRendererUtilException e)
        {
            throw new AuthnAdapterException(e);
        }
    }

    /**
     * This is the method that the PingFederate server will invoke during processing of a single logout to terminate a
     * security context for a user at the external application or authentication provider service.
     *
     * <p>
     * In this example, no extra action is needed to logout so simply return true.
     * </p>
     * 
     * @param authnIdentifiers
     *            the map of authentication identifiers originally returned to the PingFederate server by the
     *            {@link #lookupAuthN} method. This enables the adapter to associate a security context or session
     *            returned by lookupAuthN with the invocation of this logout method.
     * @param req
     *            the HttpServletRequest can be used to read cookies, parameters, headers, etc. It can also be used to
     *            find out more about the request like the full URL the request was made to.
     * @param resp
     *            the HttpServletResponse. The response can be used to facilitate an asynchronous interaction. Sending a
     *            client side redirect or writing (and flushing) custom content to the response are two ways that an
     *            invocation of this method allows for the adapter to take control of the user agent. Note that if
     *            control of the user agent is taken in this way, then the agent must eventually be returned to the
     *            <code>resumePath</code> endpoint at the PingFederate server to complete the protocol transaction.
     * @param resumePath
     *            the relative URL that the user agent needs to return to, if the implementation of this method
     *            invocation needs to operate asynchronously. If this method operates synchronously, this parameter can
     *            be ignored. The resumePath is the full path portion of the URL - everything after hostname and port.
     *            If the hostname, port, or protocol are needed, they can be derived using the HttpServletRequest.
     * @return a boolean indicating if the logout was successful.
     * @throws AuthnAdapterException
     *             for any unexpected runtime problem that the implementation cannot handle.
     * @throws IOException
     *             for any problem with I/O (typically any operation that writes to the HttpServletResponse will throw
     *             an IOException.
     * 
     * @see IdpAuthenticationAdapter#logoutAuthN(Map, HttpServletRequest, HttpServletResponse, String)
     */
    @Override
    public boolean logoutAuthN(Map authnIdentifiers, HttpServletRequest req, HttpServletResponse resp, String resumePath) throws AuthnAdapterException, IOException 
    {
        return true;
    }
    
    /**
     * This method is used to retrieve information about the adapter (e.g. AuthnContext).
     * <p>
     * In this example the method is not used and simply returns null
     * </p>
     * 
     * @return a map
     */
    @Override
    public Map<String, Object> getAdapterInfo() 
    {
        return null;
    }

    /**
     * For an API-capable plugin, this method returns a description of the API supported by the plugin, including
     * each of the possible API states that it supports. The result of this method is used by
     * PingFederate in generating API documentation.
     *
     * @return An object that describes the adapter's API.
     */
    @Override
    public PluginApiSpec getApiSpec()
    {
        PluginApiSpec result = new PluginApiSpec(Arrays.asList(StateSpec.USER_ATTRIBUTES_REQUIRED));
        return result;
    }

    /**
     * This method is deprecated. It is not called when IdpAuthenticationAdapterV2 is implemented. It is replaced by
     * {@link #lookupAuthN(HttpServletRequest, HttpServletResponse, Map)}
     * 
     */
    @Override 
    @Deprecated
    public Map lookupAuthN(HttpServletRequest req, HttpServletResponse resp, String partnerSpEntityId, AuthnPolicy authnPolicy, String resumePath) throws AuthnAdapterException, IOException 
    {
        throw new UnsupportedOperationException();
    }

}
