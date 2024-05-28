package com.pingidentity.adapter.idp.api;

import com.pingidentity.sdk.api.authn.common.CommonActionSpec;
import com.pingidentity.sdk.api.authn.spec.AuthnStateSpec;

/**
 * This class contains static definitions of the states that are exposed by this API-capable adapter.
 * These specs are used by PingFederate in generating API documentation. They are also used by the adapter
 * at runtime in handling API requests and generating responses.
 */
public class StateSpec
{
    /**
     * The only state exposed by this adapter. In this state, the API client must submit the user's username and
     * attributes.
     */
    public final static AuthnStateSpec<UserAttributesRequired> USER_ATTRIBUTES_REQUIRED = new AuthnStateSpec.Builder<UserAttributesRequired>()
            .status("USER_ATTRIBUTES_REQUIRED")
            .description("The user's username and attributes are required.")
            .modelClass(UserAttributesRequired.class)
            .action(ActionSpec.SUBMIT_USER_ATTRIBUTES)
            .action(CommonActionSpec.CANCEL_AUTHENTICATION)
            .build();
}
