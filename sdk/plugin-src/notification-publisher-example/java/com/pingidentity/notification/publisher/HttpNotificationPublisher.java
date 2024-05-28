/*
 * **************************************************
 *  Copyright (C) 2020 Ping Identity Corporation
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

package com.pingidentity.notification.publisher;

import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.notification.NotificationPublisherPlugin;
import com.pingidentity.sdk.notification.NotificationSenderPluginDescriptor;
import com.pingidentity.sdk.notification.PublishResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
import org.sourceid.common.VersionUtil;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;

import java.io.IOException;
import java.util.Map;

/**
 * This class provides a sample implementation for a notification publisher that will send
 * messages via a HTTP POST to a configured URL.
 */

public class HttpNotificationPublisher implements NotificationPublisherPlugin
{
    public static final String TYPE = "Sample HTTP Notification Publisher";

    private static final String POST_ENDPOINT_NAME = "POST Endpoint";

    private final PluginDescriptor pluginDescriptor;
    private Configuration configuration;

    public HttpNotificationPublisher()
    {
        GuiConfigDescriptor guiConfig = getGuiDescriptor();
        pluginDescriptor = new NotificationSenderPluginDescriptor(TYPE, this, guiConfig, VersionUtil.getVersion());
        pluginDescriptor.setSupportsExtendedContract(false);
    }

    /**
     * This is the method that the PingFederate server will invoke when publishing a notification.
     * The appropriate notification publisher instance will be invoked using the value from
     * {@link com.pingidentity.sdk.PluginDescriptor#getType()} via {@link #getPluginDescriptor()}.
     *
     * @param eventType
     *            contains information regarding the event triggering the notification publisher.
     * @param data
     *            contains map of key/values from which a notification message can be constructed.
     * @param configuration
     *            contains configuration metadata related to the notification.
     * @return PublishResult containing the status of the notification sent by the plugin instance.
     */
    @Override
    @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
    public PublishResult publishNotification(String eventType, Map<String, String> data, Map<String, String> configuration)
    {
        String endpoint = this.configuration.getFieldValue(POST_ENDPOINT_NAME);

        JSONObject jsonObject = new JSONObject();
        try
        {
            jsonObject.put("eventType", eventType);
            jsonObject.put("data", data);
        }
        catch (JSONException e)
        {
            // proper logging should be considered here
        }

        PublishResult sendResult = new PublishResult();
        try (CloseableHttpClient httpClient = HttpClients.custom().useSystemProperties().setUserAgent("PingFederate").build())
        {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
            HttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == 200)
            {
                sendResult.setNotificationStatus(PublishResult.NOTIFICATION_STATUS.SUCCESS);
            }
            else
            {
                sendResult.setNotificationStatus(PublishResult.NOTIFICATION_STATUS.FAILURE);
            }
        }
        catch (IOException e)
        {
            // proper logging should be considered here
            sendResult.setNotificationStatus(PublishResult.NOTIFICATION_STATUS.FAILURE);
        }
        return sendResult;
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
        this.configuration = configuration;
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
        return this.pluginDescriptor;
    }

    /**
     * Method to build GUI field configuration
     */
    private GuiConfigDescriptor getGuiDescriptor()
    {
        String description = "This Notification Publisher will send messages via a HTTP POST to a configured URL.";

        String endpointDescription = "Endpoint where the notification will be published.";

        GuiConfigDescriptor guiConfigDescriptor = new GuiConfigDescriptor(description);

        // post endpoint field
        TextFieldDescriptor endpoint = new TextFieldDescriptor(POST_ENDPOINT_NAME, endpointDescription);
        guiConfigDescriptor.addField(endpoint);

        return guiConfigDescriptor;
    }
}