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

package com.pingidentity.customdatastore;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.SimpleFieldList;
import org.sourceid.saml20.adapter.gui.AdapterConfigurationGuiDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;

import com.pingidentity.sources.CustomDataSourceDriver;
import com.pingidentity.sources.CustomDataSourceDriverDescriptor;
import com.pingidentity.sources.SourceDescriptor;
import com.pingidentity.sources.gui.FilterFieldsGuiDescriptor;

/**
 * This class is an example of a custom data store that retrieves a user's favorites' attributes, i.e. favorite movie,
 * book, etc. from a collection of properties files. This data store has a configuration setting to specify a path to a
 * file system directory and is used in its {@link #retrieveValues(Collection, SimpleFieldList)} method to return the
 * matching attributes from the corresponding properties file.
 * 
 * In this example, this data store loads a user defined properties file containing the following properties:
 * <p>
 * 
 * <pre>
 * favoriteMovie=someMovie
 * favoriteBook=someBook
 * favoriteSong=someSong
 * </pre>
 * 
 * </p>
 * If a property does not exist, an empty string will be returned (for no particular reason). If the properties file
 * cannot be loaded, an empty map will be returned, as per the interface documentation.
 */
public class SamplePropertiesDataStore implements CustomDataSourceDriver
{
    private static final String CONFIG_PROPS_PATH = "Path to properties directory";
    private static final String FILTER_USERNAME = "SamplePropertiesDataStore Username";

    // A reference to our CustomDataSourceDriverDescriptor
    private final CustomDataSourceDriverDescriptor descriptor;

    // A list of fields that will be returned to the user, which can be selected and mapped
    // to an adapter contract.
    private static final List<String> listOfFields = new ArrayList<String>();

    static
    {
        listOfFields.add("favoriteMovie");
        listOfFields.add("favoriteBook");
        listOfFields.add("favoriteSong");
    }

    // Path to the directory containing all the properties files
    private String propertiesDirectory;

    public SamplePropertiesDataStore()
    {
        // create a FilterFieldsGuiDescriptor in order to filter values from our data store. The filter value can be a
        // static string or based on a SAML attribute
        FilterFieldsGuiDescriptor filterFieldsDescriptor = new FilterFieldsGuiDescriptor();
        filterFieldsDescriptor.addField(new TextFieldDescriptor(
                FILTER_USERNAME,
                "The value in this filter is used to retrieve the appropriate properties file. i.e If the value is "
                        + "'everyone', 'everyone.properties' will be loaded from the store. You can also use attribute "
                        + "values determined at runtime, i.e. If the value is '${username}', the corresponding user's "
                        + "properties file will be loaded."));

        // create the configuration descriptor for our custom data store
        AdapterConfigurationGuiDescriptor dataStoreConfigGuiDesc = new AdapterConfigurationGuiDescriptor(
                "Configuration settings for the sample properties data store.");
        dataStoreConfigGuiDesc.addField(new TextFieldDescriptor(CONFIG_PROPS_PATH,
                "The path specifies which directory the properties files are located. Each properties file in the "
                        + "directory should contain entries for 'favoriteMovie', 'favoriteBook' and 'favoriteSong'."));

        descriptor = new CustomDataSourceDriverDescriptor(this, "Sample SDK Properties Data Store",
                dataStoreConfigGuiDesc, filterFieldsDescriptor);
    }

    /**
     * PingFederate will invoke this method on your driver to discover the metadata necessary to correctly configure it.
     * PingFederate will utilize this information to dynamically draw a screen that will allow a user to correctly
     * configure your driver for use. <br/>
     * <br/>
     * The metadata returned by this method should be static. Allowing the same driver to produce different
     * configuration screens is not supported.
     * 
     * @return a SourceDescriptor that contains the UI information necessary to display the configuration screen.
     */
    @Override
    public SourceDescriptor getSourceDescriptor()
    {
        return descriptor;
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
        // load the data store configuration settings from the Configuration object
        propertiesDirectory = configuration.getFieldValue(CONFIG_PROPS_PATH);
    }

    /**
     * This method is used to determine whether the connection managed by a specific driver instance is available. This
     * method is used by the PingFederate UI prior to rendering to determine whether the driver information should be
     * editable.
     * 
     * @return true if the connection is available
     */
    @Override
    public boolean testConnection()
    {
        // Test that the directory actually exists
        try
        {
            File handle = new File(propertiesDirectory);
            return handle.canRead() && handle.isDirectory();
        }
        catch (Exception e)
        {
            // do nothing
        }

        return false;
    }

    /**
     * This method is called by PingFederate when a connection (either IdP or SP) needs to retrieve information from the
     * specified driver. This method is expected to return a map containing the resulting values.
     * 
     * @param attributeNamesToFill
     *            An array of names to retrieve values for. In the JDBC paradigm, these would be column names.
     * @param filterConfiguration
     *            A {@link org.sourceid.saml20.adapter.conf.SimpleFieldList} list of filter criteria to use when
     *            retrieve values. May be null if no filter configuration is provided. These fields are described by the
     *            {@link CustomDataSourceDriverDescriptor} class.
     * @return A map, keyed by values from the attributeNamesToFill array, that contains values retrieved by the custom
     *         driver. If no data matches the filter criteria, then an empty Map should be returned. Null should not be
     *         returned.
     */
    @Override
    public Map<String, Object> retrieveValues(Collection<String> attributeNamesToFill,
            SimpleFieldList filterConfiguration)
    {
        String propertiesName = filterConfiguration.getFieldValue(FILTER_USERNAME);
        Map<String, Object> results = new HashMap<String, Object>();
        try
        {
            // load the properties file
            Properties loadedProperties = new Properties();
            loadedProperties.load(new FileReader(new File(propertiesDirectory, propertiesName + ".properties")));

            // read the corresponding attributes from the properties file
            for (String attributeName : attributeNamesToFill)
            {
                String value = loadedProperties.getProperty(attributeName);

                // If the property doesn't exist, we'll default to an empty string (for no particular reason)
                if (value == null)
                {
                    value = "";
                }

                results.put(attributeName, value);
            }
        }
        catch (IOException e)
        {
            // Unable to find properties file, meaning the filter criteria doesn't match
            // Return an empty map instead of null as per the interface documentation
            return new HashMap<String, Object>();
        }

        return results;
    }

    /**
     * PingFederate will take the list returned from this method, and display the field names as individual checkbox
     * items. The user can select those fields for which they want values, and then map those selected fieldnames
     * against adapter contracts. During execution, the names that the user has mapped will be sent to the
     * {@link #retrieveValues(Collection, SimpleFieldList)} method.
     * 
     * @return A list of available fields to display to the user.
     */
    @Override
    public List<String> getAvailableFields()
    {
        return listOfFields;
    }
}
