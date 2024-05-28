package com.pingidentity.adapter.idp.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the model for the USER_ATTRIBUTES_REQUIRED state. It defines the fields that are returned to the
 * API client in a GET response for this state.
 */
public class UserAttributesRequired
{
    private List<String> attributeNames = new ArrayList<>();

    /**
     * Get the list of user attributes supported by this adapter instance.
     *
     * It is recommended to annotate each getter with the @Schema annotation and provide a description.
     * This description will be used in generating API documentation.
     */
    @Schema(description="A list of user attribute names that are supported by this adapter instance.")
    public List<String> getAttributeNames()
    {
        return attributeNames;
    }

    /**
     * Set the list of user attributes supported by this adapter instance.
     */
    public void setAttributeNames(List<String> attributeNames)
    {
        this.attributeNames = attributeNames;
    }
}
