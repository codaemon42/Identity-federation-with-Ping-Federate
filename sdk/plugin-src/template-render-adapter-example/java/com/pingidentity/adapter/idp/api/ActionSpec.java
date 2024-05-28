package com.pingidentity.adapter.idp.api;

import com.pingidentity.sdk.api.authn.common.CommonErrorSpec;
import com.pingidentity.sdk.api.authn.spec.AuthnActionSpec;

/**
 * This class contains static definitions of the actions that are exposed by this API-capable adapter.
 * These specs are used by PingFederate in generating API documentation. They are also used by the adapter
 * at runtime in handling API requests and generating responses.
 */
public class ActionSpec
{
    /**
     * This action is used to submit the user's username and attributes.
     */
    public final static AuthnActionSpec<SubmitUserAttributes> SUBMIT_USER_ATTRIBUTES = new AuthnActionSpec.Builder<SubmitUserAttributes>()
            .id("submitUserAttributes")
            .description("Submit the user's username and attributes.")
            .modelClass(SubmitUserAttributes.class)
            .error(CommonErrorSpec.VALIDATION_ERROR)
            .errorDetail(ErrorDetailSpec.INVALID_ATTRIBUTE_NAME)
            .build();
}
