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

package com.pingidentity.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.wstrust.model.BinarySecurityToken;
import org.sourceid.wstrust.plugin.TokenProcessingException;
import org.sourceid.wstrust.plugin.process.InvalidTokenException;
import org.sourceid.wstrust.plugin.process.TokenContext;
import org.sourceid.wstrust.plugin.process.TokenProcessor;
import org.sourceid.wstrust.plugin.process.TokenProcessorDescriptor;

import com.pingidentity.sdk.GuiConfigDescriptor;

public class SampleTokenProcessor implements TokenProcessor<BinarySecurityToken>
{

    private static final String ATTR_NAME_SUBJECT = "subject";
    private static final String TOKEN_TYPE = "urn:sample:token";
    private static final String NAME = "Sample Token Processor";
    private static final String FIELD_FOO_NAME = "foo";
    private static final String FIELD_FOO_DESCRIPTION = "Foo simple text configuration field";
    private TokenProcessorDescriptor descriptor;

    private String foo;

    public SampleTokenProcessor()
    {
        Set<String> contract = Collections.singleton(ATTR_NAME_SUBJECT);
        GuiConfigDescriptor gui = new GuiConfigDescriptor(NAME);

        FieldDescriptor fooField = new TextFieldDescriptor(FIELD_FOO_NAME, FIELD_FOO_DESCRIPTION);
        gui.addField(fooField);

        descriptor = new TokenProcessorDescriptor(NAME, this, gui, TOKEN_TYPE, contract);
    }

    /**
     * This is the method that the PingFederate server will invoke during processing of a Request Security Token (RST)
     * request. The appropriate TokenProcessor instance will be invoked using the value from
     * {@link com.pingidentity.sdk.PluginDescriptor#getType()} via {@link #getPluginDescriptor()}. </p>
     * <p>
     * The returned {@link org.sourceid.wstrust.plugin.process.TokenContext} contains attributes resulting from token
     * processing. At a minimum, {@link org.sourceid.wstrust.plugin.process.TokenContext#getSubjectAttributes()} should
     * return a name/value pair denoting the 'subject' of the token being processed.
     * </p>
     * <p>
     * 
     * </p>
     * 
     * @param token
     * @return TokenContext
     * @throws InvalidTokenException
     * @throws TokenProcessingException
     */
    @Override
    public TokenContext processToken(BinarySecurityToken token) throws InvalidTokenException, TokenProcessingException
    {
        String subject;
        try
        {
            subject = new String(token.getDecodedData(), "UTF-8");
        }
        catch (Exception e)
        {
            throw new TokenProcessingException(e.getMessage(), e);
        }

        TokenContext tokenContext = new TokenContext();
        Map<String, AttributeValue> attrs = new HashMap<String, AttributeValue>(1);
        attrs.put(ATTR_NAME_SUBJECT, new AttributeValue(subject));
        tokenContext.setSubjectAttributes(attrs);

        return tokenContext;
    }

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
        foo = configuration.getFieldValue(FIELD_FOO_NAME);
    }

    /**
     * The PingFederate server will invoke this method on your token processor implementation to discover metadata about
     * the implementation. This included the token processor's attribute contract and a description of what
     * configuration fields to render in the GUI. <br/>
     * <br/>
     * Your implementation of this method should return the same TokenPluginDescriptor object from call to call -
     * behaviour of the system is undefined if this convention is not followed.
     * 
     * @return a TokenPluginDescriptor object that describes this token processor implementation.
     */
    @Override
    public TokenProcessorDescriptor getPluginDescriptor()
    {
        return descriptor;
    }
}
