package com.pingidentity.adapter.idp.api;

import com.pingidentity.sdk.api.authn.common.CommonErrorSpec;
import com.pingidentity.sdk.api.authn.spec.AuthnErrorDetailSpec;

/**
 * This class contains static definitions of custom error detail specs that may be returned by this API-capable adapter.
 * These specs are used by PingFederate in generating API documentation. They are also used by the adapter
 * at runtime in generating responses.
 */
public class ErrorDetailSpec
{
    /**
     * This error detail is returned if the API client submits a user attribute name that is not valid for the
     * adapter instance.
     */
    public final static AuthnErrorDetailSpec INVALID_ATTRIBUTE_NAME = new AuthnErrorDetailSpec.Builder()
            .code("INVALID_ATTRIBUTE_NAME")
            .message("An invalid attribute name was provided.")
            .parentCode(CommonErrorSpec.VALIDATION_ERROR.getCode())
            .build();
}
