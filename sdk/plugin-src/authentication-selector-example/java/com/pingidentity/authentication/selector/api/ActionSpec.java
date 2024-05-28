package com.pingidentity.authentication.selector.api;

import com.pingidentity.sdk.api.authn.spec.AuthnActionSpec;

/**
 * This class contains static definitions of the actions that are exposed by this API-capable selector.
 * These specs are used by PingFederate in generating API documentation. They are also used by the selector
 * at runtime in handling API requests and generating responses.
 */
public class ActionSpec
{
    /**
     * This action is used to submit the user's email address or domain.
     */
    public final static AuthnActionSpec<SubmitEmailOrDomain> SUBMIT_EMAIL_OR_DOMAIN = new AuthnActionSpec.Builder<SubmitEmailOrDomain>()
            .id("submitEmailOrDomain")
            .description("Submit the user's email address or domain.")
            .modelClass(SubmitEmailOrDomain.class)
            .build();
}
