package com.pingidentity.authentication.selector.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is the model for the submitEmailOrDomain API action. It defines the fields that may be included in the POST
 * body for this action.
 */
public class SubmitEmailOrDomain
{
    private String emailAddressOrDomain;

    /**
     * Get the user's email address or domain.
     *
     * It is recommended to annotate each getter with the @Schema annotation and provide a description.
     * The 'required' flag can also be specified. This information will be used in generating API documentation.
     */
    @Schema(description="The user's email address or domain.", required=true)
    public String getEmailAddressOrDomain()
    {
        return emailAddressOrDomain;
    }

    /**
     * Set the user's email address or domain.
     */
    public void setEmailAddressOrDomain(String emailAddressOrDomain)
    {
        this.emailAddressOrDomain = emailAddressOrDomain;
    }
}
