package com.pingidentity.authentication.selector.api;

import com.pingidentity.sdk.api.authn.common.CommonActionSpec;
import com.pingidentity.sdk.api.authn.spec.AuthnStateSpec;

/**
 * This class contains static definitions of the states that are exposed by this API-capable selector.
 * These specs are used by PingFederate in generating API documentation. They are also used by the selector
 * at runtime in handling API requests and generating responses.
 */
public class StateSpec
{
    /**
     * The only state exposed by this selector. In this state, the API client must submit the user's email
     * address or domain.
     */
    public final static AuthnStateSpec<Void> EMAIL_OR_DOMAIN_REQUIRED = new AuthnStateSpec.Builder<Void>()
            .status("EMAIL_OR_DOMAIN_REQUIRED")
            .description("The user's email address or domain is required.")
            .action(ActionSpec.SUBMIT_EMAIL_OR_DOMAIN)
            .action(CommonActionSpec.CANCEL_AUTHENTICATION)
            .build();
}
