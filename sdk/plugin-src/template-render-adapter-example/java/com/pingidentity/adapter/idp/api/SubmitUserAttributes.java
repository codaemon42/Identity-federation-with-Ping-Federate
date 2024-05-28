package com.pingidentity.adapter.idp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the model for the submitUserAttributes API action. It defines the fields that may be included in the POST
 * body for this action.
 */
public class SubmitUserAttributes
{
    private String username;
    private Map<String,Object> userAttributes = new HashMap<>();

    /**
     * Get the username.
     *
     * It is recommended to annotate each getter with the @Schema annotation and provide a description.
     * The 'required' flag can also be specified. This information will be used in generating API documentation.
     */
    @Schema(description="The user's username.", required=true)
    public String getUsername()
    {
        return username;
    }

    /**
     * Set the username.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Get the user attributes.
     */
    @Schema(description="Additional user attributes, as name-value pairs.")
    public Map<String, Object> getUserAttributes()
    {
        return userAttributes;
    }

    /**
     * Set the user attributes.
     */
    public void setUserAttributes(Map<String, Object> userAttributes)
    {
        this.userAttributes = userAttributes;
    }
}
