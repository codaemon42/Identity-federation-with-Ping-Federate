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

package com.pingidentity.generator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.wstrust.model.BinarySecurityToken;
import org.sourceid.wstrust.plugin.TokenProcessingException;
import org.sourceid.wstrust.plugin.generate.TokenContext;
import org.sourceid.wstrust.plugin.generate.TokenGenerator;

import com.pingidentity.common.util.xml.XmlIDUtil;
import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import org.sourceid.wstrust.plugin.process.TokenPluginDescriptor;
import org.apache.commons.codec.binary.Base64;

public class SampleTokenGenerator implements TokenGenerator
{
    private static final String ATTR_NAME_SUBJECT = "subject";
    private static final String TOKEN_TYPE = "urn:sample:token";
    private static final String NAME = "Sample Token Generator";
    private static final String FIELD_FOO_NAME = "foo";
    private static final String FIELD_FOO_DESCRIPTION = "Foo simple text configuration field";
    private TokenPluginDescriptor descriptor;

    private String foo;

    public SampleTokenGenerator()
    {
        Set<String> contract = Collections.singleton(ATTR_NAME_SUBJECT);
        GuiConfigDescriptor gui = new GuiConfigDescriptor(NAME);

        FieldDescriptor fooField = new TextFieldDescriptor(FIELD_FOO_NAME, FIELD_FOO_DESCRIPTION);
        gui.addField(fooField);

        descriptor = new TokenPluginDescriptor(NAME, this, gui, TOKEN_TYPE, contract);
    }

    /**
     * This is the method that the PingFederate server will invoke when constructing a Request Security Token Response
     * (RSTR). The appropriate token generator instance will be invoked using the value from
     * {@link com.pingidentity.sdk.PluginDescriptor#getType()} via {@link #getPluginDescriptor()}.
     * 
     * @param attributeContext
     *            contains attributes resulting from token generation. At a minimum,
     *            {@link org.sourceid.wstrust.plugin.process.TokenContext#getSubjectAttributes()} should return a
     *            name/value pair denoting the 'subject' of the token being generated.
     * @return SecurityToken as a BinarySecurityToken instance
     * @throws TokenProcessingException
     */
    @Override
    public BinarySecurityToken generateToken(TokenContext attributeContext) throws TokenProcessingException
    {
        Map<String, AttributeValue> attributes = attributeContext.getSubjectAttributes();
        String subject = attributes.get(ATTR_NAME_SUBJECT).getValue();
        BinarySecurityToken binarySecurityToken = new BinarySecurityToken(XmlIDUtil.createID(), TOKEN_TYPE);
        Base64 encoder = new Base64();

        try
        {
            binarySecurityToken.setEncodedData(new String(encoder.encodeBase64(subject.getBytes("UTF-8")), "UTF-8"));
        }
        catch (Exception e)
        {
            throw new TokenProcessingException(e.getMessage(), e);
        }

        return binarySecurityToken;
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
     * Returns the {@link PluginDescriptor} that describes this plugin to the PingFederate server. This includes how
     * PingFederate will render the plugin in the administrative console, and metadata on how PingFederate will treat
     * this plugin at runtime.
     * 
     * @return A {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        return descriptor;
    }
}
