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



package com.pingidentity.adapter.sp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sourceid.saml20.adapter.AuthnAdapterDescriptor;
import org.sourceid.saml20.adapter.AuthnAdapterException;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.Field;
import org.sourceid.saml20.adapter.conf.FieldList;
import org.sourceid.saml20.adapter.conf.Table;
import org.sourceid.saml20.adapter.gui.AbstractSelectionFieldDescriptor;
import org.sourceid.saml20.adapter.gui.ActionDescriptor;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.CheckBoxFieldDescriptor;
import org.sourceid.saml20.adapter.gui.RadioGroupFieldDescriptor;
import org.sourceid.saml20.adapter.gui.SelectFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TableDescriptor;
import org.sourceid.saml20.adapter.gui.TextAreaFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.UploadFileFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.ConfigurationValidator;
import org.sourceid.saml20.adapter.gui.validation.FieldValidator;
import org.sourceid.saml20.adapter.gui.validation.RowValidator;
import org.sourceid.saml20.adapter.gui.validation.ValidationException;
import org.sourceid.saml20.adapter.gui.validation.impl.IntegerValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;
import org.sourceid.saml20.adapter.sp.authn.LocalIdPasswordLookup;
import org.sourceid.saml20.adapter.sp.authn.SpAuthenticationAdapter;
import org.sourceid.saml20.adapter.sp.authn.SsoContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class SpAuthnAdapterExample implements SpAuthenticationAdapter
{
    private static final String CHECKBOX_FIELD_NAME = "Checkbox";
    private static final String USERS_FILE_FIELD_NAME = "Users properties file";

    private Log log = LogFactory.getLog(this.getClass());

    /**
     * Hold on to an instance so we don't have to rebuild it every time getAdapterDescriptor() is called.
     * 
     * See the initDescriptor() method for an example of creating an AuthnAdapterDescriptor
     */
    private AuthnAdapterDescriptor authnAdapterDesc = initDescriptor();

    /**
     * a value that will be set via GUI configuration when configure(Configuration configuration) is called
     */
    private boolean showHeaderAndFooter;

    private Properties users;

    private LocalIdPasswordLookup localIdPasswordLookup = new LocalIdPasswordLookup()
    {
        @Override
        public String getLocalIdentifier(String username, String password)
        {
            String foundpass = users.getProperty(username);
            return (foundpass != null && password != null && password.equals(foundpass)) ? username : null;
        }
    };

    /**
     * This implementation doesn't do anything but log the attributes it receives.
     */
    @Override
    public Serializable createAuthN(SsoContext ssoContext, HttpServletRequest req, HttpServletResponse resp,
            String resumePath) throws AuthnAdapterException, IOException
    {
        StringBuilder msg = new StringBuilder();

        if (showHeaderAndFooter)
        {
            msg.append("\n---------------SpAuthnAdapterExample.createAuthN(...)-----------------\n");
        }

        msg.append("Attributes: ").append(ssoContext.getSubjectAttrs());

        if (showHeaderAndFooter)
        {
            msg.append("\n----------------------------------------------------------------------\n");
        }

        log.info(msg);

        // Return a copy of the attributes.
        HashMap<String, AttributeValue> copyOfAttrs = new HashMap<String, AttributeValue>(ssoContext.getSubjectAttrs());

        // Note that whatever we return here will be passed to the adapter as the first arg of the logout method
        return copyOfAttrs;
    }

    /**
     * This implementation doesn't do anything but log.
     */
    @Override
    public boolean logoutAuthN(Serializable authnBean, HttpServletRequest req, HttpServletResponse resp,
            String resumePath) throws AuthnAdapterException, IOException
    {
        StringBuffer msg = new StringBuffer();
        if (showHeaderAndFooter)
        {
            msg.append("\n---------------SpAuthnAdapterExample.logoutAuthN(...)-----------------\n");
        }

        msg.append("Serializable authnBean: ").append(authnBean);

        if (showHeaderAndFooter)
        {
            msg.append("\n----------------------------------------------------------------------\n");
        }

        log.info(msg);

        return true;
    }

    /**
     * Lookup the local user ID from our users properties file
     */
    @Override
    public String lookupLocalUserId(HttpServletRequest req, HttpServletResponse resp, String partnerIdpEntityId,
            String resumePath) throws AuthnAdapterException, IOException
    {
        String localId = localIdPasswordLookup.lookupViaPassword(req, resp, partnerIdpEntityId, resumePath);

        StringBuffer msg = new StringBuffer();
        if (showHeaderAndFooter)
        {
            msg.append("\n---------------SpAuthnAdapterExample.lookupLocalUserId(...)-----------------\n");
        }
        msg.append(localId);
        if (showHeaderAndFooter)
        {
            msg.append("\n----------------------------------------------------------------------\n");
        }

        log.info(msg);

        return localId;
    }

    /**
     * The PingFederate server will invoke this method on your adapter implementation to discover metadata about the
     * implementation. This included the adapter's attribute contract and a description of what configuration fields to
     * render in the GUI. <br/>
     * <br/>
     * Your implementation of this method should return the same AuthnAdapterDescriptor object from call to call -
     * behaviour of the system is undefined if this convention is not followed.
     * 
     * @return an AuthnAdapterDescriptor object that describes this adapter implementation.
     */
    @Override
    public AuthnAdapterDescriptor getAdapterDescriptor()
    {
        return authnAdapterDesc;
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
        // Just going to use the value of one of the fields here but you get the idea I hope...
        showHeaderAndFooter = configuration.getBooleanFieldValue(CHECKBOX_FIELD_NAME);

        // Also going to use the uploaded file to get a map of username/password pairs.
        byte[] fileBytes = configuration.getFileFieldValueAsByteArray(USERS_FILE_FIELD_NAME);
        users = new Properties();
        try
        {
            users.load(new ByteArrayInputStream(fileBytes));
        }
        catch (IOException e)
        {
            log.warn("Problem loading users properties file", e);
        }
    }

    /**
     * Build the AuthnAdapterDescriptor for this adapter implementation.
     * 
     * @return a descriptor for this adapter
     */
    private AuthnAdapterDescriptor initDescriptor()
    {
        // Create an AdapterConfigurationGuiDescriptor that will tell PingFederate how to render a configuration
        // GUI screen and how to validate the user input
        String description = "The main description that will show up on the admin console GUI configuration page";
        AdapterConfigurationGuiDescriptor adapterConfGuiDesc = new AdapterConfigurationGuiDescriptor(description);

        // Add a file upload field - the file will need to be a java properties file that will have usernames and
        // passwords
        String aDesc = "A properties file containing valid username/password pairs.";
        UploadFileFieldDescriptor fileField = new UploadFileFieldDescriptor(USERS_FILE_FIELD_NAME, aDesc);
        fileField.addValidator(new RequiredFieldValidator());
        fileField.addValidator(new FieldValidator()
        {
            @Override
            public void validate(Field field) throws ValidationException
            {
                Properties users = new Properties();
                InputStream usersInputStream = new ByteArrayInputStream(field.getFileValueAsByteArray());
                try
                {
                    users.load(usersInputStream);
                    if (users.isEmpty())
                    {
                        throw new ValidationException(
                                "Users properties file must contain at least one username/password combo");
                    }
                }
                catch (IOException e)
                {
                    throw new ValidationException("Invalid users properties file: " + e.getMessage());
                }

            }
        });
        adapterConfGuiDesc.addField(fileField);

        // Add a text entry field that is required and has a default value
        String textName = "Text";
        TextFieldDescriptor textField = new TextFieldDescriptor(textName, "Description of text");
        textField.addValidator(new RequiredFieldValidator());
        textField.setDefaultValue("default value");
        adapterConfGuiDesc.addField(textField);

        // Add a text field that is optional but must be a number if entered
        TextFieldDescriptor numberField = new TextFieldDescriptor("Number", "A number between 2 and 47 (inclusive)");
        numberField.addValidator(new IntegerValidator(2, 47), true);
        adapterConfGuiDesc.addField(numberField);

        // Add a checkbox field that defaults to 'checked'
        String desc = "Include a header and footer in the log message?";
        CheckBoxFieldDescriptor checkBoxField = new CheckBoxFieldDescriptor(CHECKBOX_FIELD_NAME, desc);
        checkBoxField.setDefaultValue(true);
        adapterConfGuiDesc.addField(checkBoxField);

        // Add a field that is a group of radio buttons
        String[] radioOptions = new String[] { "One", "Two", "Three" };
        RadioGroupFieldDescriptor radioField = new RadioGroupFieldDescriptor("Radio buttons", "", radioOptions);
        adapterConfGuiDesc.addField(radioField);

        // Add a field that is a selection drop down box. Note that the values displayed to the user can be different
        // than the values submitted and stored in configuration.
        List<AbstractSelectionFieldDescriptor.OptionValue> selectOptions;
        selectOptions = new ArrayList<AbstractSelectionFieldDescriptor.OptionValue>();
        selectOptions.add(SelectFieldDescriptor.SELECT_ONE);
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("First", "1"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Second", "2"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Third", "3"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Fourth", "4"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Fifth", "5"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Other", "other"));
        selectOptions.add(new AbstractSelectionFieldDescriptor.OptionValue("Last", "lots"));
        SelectFieldDescriptor selectField = new SelectFieldDescriptor("Select", "", selectOptions);
        adapterConfGuiDesc.addField(selectField);

        TextFieldDescriptor passwordField = new TextFieldDescriptor("Password", "An encrypted text field", true);
        adapterConfGuiDesc.addField(passwordField);

        // Add a text area field as an 'advanced' field. Advanced fields only show up when the user
        // selects to see the advanced configuration options.
        // Care should be taken when using advanced fields - they should be used for configuration options that will
        // rarely need to be changed and default values that won't cause validation errors should be provided.
        TextAreaFieldDescriptor textAreaField = new TextAreaFieldDescriptor("Text Area", "An advanced field", 3, 50);
        adapterConfGuiDesc.addAdvancedField(textAreaField);

        // Add a table of configurable values
        final String tableName = "A Table of Values";
        TableDescriptor table = new TableDescriptor(tableName, "A description of the table");
        final String columnOneName = "Column One";
        TextFieldDescriptor column1 = new TextFieldDescriptor(columnOneName, "A description of it.");
        column1.addValidator(new RequiredFieldValidator());
        column1.addValidator(new IntegerValidator());
        table.addRowField(column1);
        final String columnTwoName = "Column Two";
        TextFieldDescriptor column2 = new TextFieldDescriptor(columnTwoName, "Description.");
        column2.addValidator(new RequiredFieldValidator());
        column2.addValidator(new IntegerValidator(0, 200));
        table.addRowField(column2);
        final String columnThreeName = "Checkbox column";
        table.addRowField(new CheckBoxFieldDescriptor(columnThreeName, ""));
        // Show how a row can be validated by checking dependencies between field values
        table.addValidator(new RowValidator()
        {
            @Override
            public void validate(FieldList fieldsInRow) throws ValidationException
            {
                boolean booleanFieldValue = fieldsInRow.getBooleanFieldValue(columnThreeName);
                if (booleanFieldValue)
                {
                    int max = 100;
                    int one = fieldsInRow.getIntFieldValue(columnOneName);
                    int two = fieldsInRow.getIntFieldValue(columnTwoName);
                    if (one + two > max)
                    {
                        throw new ValidationException("The sum of the values for '" + columnOneName + "' and '"
                                + columnTwoName + "' cannot exceed " + max + ", if '" + columnThreeName
                                + "' is checked.");
                    }
                }
            }
        });
        adapterConfGuiDesc.addTable(table);

        // Add an action. An action lets you invoke some code as the result of the user clicking a link
        // and return a string that will be displayed. This action will just display the content of the upload file.
        ActionDescriptor action = new ActionDescriptor("Action", "Show the file content", new ActionDescriptor.Action()
        {
            @Override
            public String actionInvoked(Configuration configuration)
            {
                String value = configuration.getFileFiledValueAsString(USERS_FILE_FIELD_NAME);
                if (value == null)
                {
                    value = configuration.getFieldValue(USERS_FILE_FIELD_NAME);
                    if (value == null)
                    {
                        value = "File content value was null";
                    }
                }
                return value;
            }
        });
        adapterConfGuiDesc.addAction(action);

        // You can add validators that can validate the whole configuration by checking dependencies between field
        // values and table field values and whatever else by adding an implementation of ConfigurationValidator.
        adapterConfGuiDesc.addValidator(new ConfigurationValidator()
        {
            @Override
            public void validate(Configuration configuration) throws ValidationException
            {
                if (!configuration.getBooleanFieldValue(CHECKBOX_FIELD_NAME))
                {
                    Table table = configuration.getTable(tableName);
                    final int min = 2;
                    if (table.getRows().size() < min)
                    {
                        throw new ValidationException("'" + tableName + "' must have at least " + min + " row(s). "
                                + "if '" + CHECKBOX_FIELD_NAME + "' is not checked.");
                    }
                }
            }
        });

        // The type or name of the adapter that will show up in the GUI (usually in drop down lists)
        String type = "SP API Usage Example Adapter";

        // A set of attribute names are the attribute contract for the adapter
        Set<String> attributeContract = new HashSet<String>();
        attributeContract.add("Attribute One");
        attributeContract.add("Attribute Two");

        return new AuthnAdapterDescriptor(this, type, attributeContract, true, adapterConfGuiDesc);
    }
}
