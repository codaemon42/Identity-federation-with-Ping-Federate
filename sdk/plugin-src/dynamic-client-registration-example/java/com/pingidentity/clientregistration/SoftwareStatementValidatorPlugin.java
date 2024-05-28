/*
 * **************************************************
 *  Copyright (C) 2019 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are the property of Ping Identity Corporation.
 *  You may not copy or use this file, in either source code or executable
 *  form, except in compliance with terms set by Ping Identity Corporation.
 *  For further information please contact:
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.pingidentity.clientregistration;

import com.pingidentity.access.TrustedCAAccessor;
import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.logging.LoggingUtil;
import com.pingidentity.sdk.oauth20.registration.ClientAuthType;
import com.pingidentity.sdk.oauth20.registration.ClientRegistrationException;
import com.pingidentity.sdk.oauth20.registration.DynamicClient;
import com.pingidentity.sdk.oauth20.registration.DynamicClientFields;
import com.pingidentity.sdk.oauth20.registration.DynamicClientRegistrationPlugin;
import com.pingidentity.sdk.oauth20.registration.DynamicClientRegistrationPluginDescriptor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.jose4j.http.Get;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;
import org.sourceid.openid.connect.UserIdType;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.conf.Field;
import org.sourceid.saml20.adapter.gui.FieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextAreaFieldDescriptor;
import org.sourceid.saml20.adapter.gui.TextFieldDescriptor;
import org.sourceid.saml20.adapter.gui.validation.FieldValidator;
import org.sourceid.saml20.adapter.gui.validation.ValidationException;
import org.sourceid.saml20.adapter.gui.validation.impl.HttpURLValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.JwksValidator;
import org.sourceid.saml20.adapter.gui.validation.impl.RequiredFieldValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384;
import static org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA384;
import static org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA512;

/**
 * A Dynamic Client Registration Plugin that uses the JWT (JSON Web Token) value of
 * software_statement to process a client registration request.
 */
public class SoftwareStatementValidatorPlugin implements DynamicClientRegistrationPlugin
{
    private final Log LOG = LogFactory.getLog(SoftwareStatementValidatorPlugin.class);
    private static final String TYPE = "Software Statement Validator plugin";
    private static final String VERSION = "1.0";

    private static final String ISSUER = "Issuer";
    private static final String JWKS_URL = "JWKS URL";
    private static final String JWKS = "JWKS";

    private String issuer;
    private String jwks;
    private String jwksUrl;
    private Configuration configuration;

    private static final AlgorithmConstraints SIG_REQUIRED_CONSTRAINTS =
            new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, RSA_USING_SHA256, RSA_USING_SHA384,
                                     RSA_USING_SHA512, ECDSA_USING_P256_CURVE_AND_SHA256, ECDSA_USING_P384_CURVE_AND_SHA384,
                                     ECDSA_USING_P521_CURVE_AND_SHA512);

    /**
     * This method is called by the PingFederate server to push configuration values
     * entered by the administrator in the PingFederate administration console.
     *
     * @param configuration
     */
    @Override
    public void configure(Configuration configuration)
    {
        issuer = configuration.getFieldValue(ISSUER);
        jwksUrl = configuration.getFieldValue(JWKS_URL);
        jwks = configuration.getFieldValue(JWKS);
        this.configuration = configuration;
    }

    /**
     * Returns the {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     * This includes how PingFederate will render the plugin in the administrative console, and
     * metadata on how PingFederate will treat this plugin at runtime.
     *
     * @return A {@link PluginDescriptor} that describes this plugin to the PingFederate server.
     */
    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        GuiConfigDescriptor guiDescriptor = new GuiConfigDescriptor(TYPE);

        FieldDescriptor issuer = new TextFieldDescriptor(ISSUER, "A unique identifier present in software statement JWT.");
        issuer.addValidator(new RequiredFieldValidator());
        guiDescriptor.addField(issuer);

        FieldDescriptor jwksEndpointUrl = new TextFieldDescriptor(JWKS_URL, "A set of JSON Web Keys (JWKS) are downloaded from this endpoint and used for JWT signature verification.");
        guiDescriptor.addField(jwksEndpointUrl);

        FieldDescriptor jwks = new TextAreaFieldDescriptor(JWKS, "A set of JSON Web Keys (JWKS) used for JWT signature verification.", 10, 48);
        guiDescriptor.addField(jwks);

        guiDescriptor.addValidator(configuration -> {

            Field jwksUrlField = configuration.getField(JWKS_URL);
            String jwksUriValue = jwksUrlField.getValue();
            Field jwksField = configuration.getField(JWKS);
            String jwksValue = jwksField.getValue();

            List<String> errors = new ArrayList<>();

            if (StringUtils.isBlank(jwksUriValue) && StringUtils.isBlank(jwksValue))
            {
                errors.add("Either " + JWKS_URL + " or " + JWKS + " is required to verify the software statement signature.");
            }
            else
            {
                if (StringUtils.isNotBlank(jwksUriValue) && StringUtils.isNotBlank(jwksValue))
                {
                    errors.add("You must provide either " + JWKS_URL + " or " + JWKS + " but not both.");
                }
                else
                {
                    if (StringUtils.isNotBlank(jwksUriValue))
                    {
                        FieldValidator httpURLValidator = new HttpURLValidator();
                        httpURLValidator.validate(jwksUrlField);
                    }

                    if (StringUtils.isNotBlank(jwksValue))
                    {
                        FieldValidator jwksValidator = new JwksValidator();
                        jwksValidator.validate(jwksField);
                    }
                }
            }

            if (!errors.isEmpty())
            {
                throw new ValidationException(errors);
            }
        });

        DynamicClientRegistrationPluginDescriptor pluginDescriptor = new DynamicClientRegistrationPluginDescriptor(TYPE, this, guiDescriptor, VERSION);
        pluginDescriptor.setSupportsExtendedContract(false);
        //temp fix for findbugs
        return pluginDescriptor;
    }

    /**
     * This method verifies the signature and processes the JWT value of the software statement
     * to populate a DynamicClient instance.
     *
     * @param request       the {@link HttpServletRequest} object
     * @param response      the {@link HttpServletResponse} object
     * @param dynamicClient the {@link DynamicClient} received and parsed by the request
     * @param inParameters  the parameters {@link Map}
     * @throws ClientRegistrationException if unable to continue the client registration
     */
    @Override
    public void processPlugin(HttpServletRequest request, HttpServletResponse response, DynamicClient dynamicClient, Map<String, Object> inParameters)
            throws ClientRegistrationException
    {
        String pluginId = this.configuration.getId();
        String softwareStatement = dynamicClient.getSoftwareStatement();
        // Check software statement is not null
        if (StringUtils.isBlank(softwareStatement))
        {
            throw new ClientRegistrationException(Response.Status.BAD_REQUEST, ClientRegistrationException.ErrorCode.invalid_payload, "[software_statement] is required");
        }
        // Validate the JWT
        VerificationKeyResolver resolver = createVerificationKeyResolver();
        if (resolver == null || StringUtils.isBlank(issuer))
        {
            String description = "[" + pluginId + "] policy plugin is not configured correctly. Please revisit the configuration";
            LOG.error(description);
            throw new ClientRegistrationException(Response.Status.INTERNAL_SERVER_ERROR, ClientRegistrationException.ErrorCode.internal_error, "Invalid configuration");
        }
        JwtContext jwtContext;
        try
        {
            JwtConsumer validateSubJwtConsumer = new JwtConsumerBuilder()
                    .setExpectedIssuer(true, issuer)// Ensure expected issuer
                    .setVerificationKeyResolver(resolver) // Verify the signature
                    .setJwsAlgorithmConstraints(SIG_REQUIRED_CONSTRAINTS)// Restrict the list of allowed signing algorithms
                    .build();
            jwtContext = validateSubJwtConsumer.process(softwareStatement);
        }
        catch (InvalidJwtSignatureException e)
        {
            String description = "[software_statement] signature verification failed";
            throw new ClientRegistrationException(Response.Status.BAD_REQUEST, ClientRegistrationException.ErrorCode.invalid_software_statement, description);
        }
        catch (InvalidJwtException e)
        {
            String description = null;

            if (e.getCause() != null && e.getCause() instanceof UnresolvableKeyException)
            {
                description = "[software_statement] signature verification failed";
            }
            else
            {
                description = "[software_statement] is not a valid JWT, " + e.getMessage();
            }

            throw new ClientRegistrationException(Response.Status.BAD_REQUEST, ClientRegistrationException.ErrorCode.invalid_software_statement, description);
        }

        //Process individual JWT claims
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        try
        {
            if (jwtContext.getJwtClaims().getJwtId() != null)
            {
                LoggingUtil.setRequestJti(jwtContext.getJwtClaims().getJwtId());
            }
        }
        catch (MalformedClaimException e)
        {
            // Log an error and continue.
            LOG.error("Unable to get jti from [software_statement]. " +
                      "Jti will not be included in the security audit log record for this transaction: " +
                      e.getMessage());
            LOG.debug(e);
        }

        Collection<String> claimNames = jwtClaims.getClaimNames();
        Set<String> standardClaims = getStandardClaims();
        for (String claimName : claimNames)
        {
            try
            {
                if (standardClaims.contains(claimName))
                {
                    processStandardClaim(dynamicClient, DynamicClientFields.valueOf(claimName.toUpperCase()), jwtClaims);
                }
                else if (dynamicClient.getClientMetadataKeys().contains(claimName))
                {
                    processExtendedClaims(dynamicClient, claimName, jwtClaims);
                }
                else
                {
                    LOG.debug("claim [" + claimName + "] is not a supported client metadata");
                }
            }
            catch (MalformedClaimException e)
            {
                LOG.error("claim [" + claimName + "] was not added to the client object due to a data conversion error.");
                LOG.debug(e.getMessage());
            }
        }
    }

    @Override
    public void processPluginUpdate(HttpServletRequest request, HttpServletResponse response, DynamicClient dynamicClient, DynamicClient existingDynamicClient, Map<String, Object> inParameters)
            throws ClientRegistrationException
    {
        if (StringUtils.isNotEmpty(dynamicClient.getSoftwareStatement()))
        {
            String description = "[software_statement] is not allowed in an update.";
            throw new ClientRegistrationException(Response.Status.BAD_REQUEST, ClientRegistrationException.ErrorCode.invalid_software_statement, description);
        }
    }

    /**
     * This method obtains a resolver using the configured JWKS or JWKS URI. This resolver shall
     * choose the key to be used for signature verification on the given JWS (JSON Web Signature).
     *
     * @return a new instance of {@link VerificationKeyResolver}
     */
    private VerificationKeyResolver createVerificationKeyResolver()
    {
        VerificationKeyResolver resolver = null;
        if (StringUtils.isNotEmpty(jwksUrl))
        {
            HttpsJwks httpsJwks = new HttpsJwks(jwksUrl);
            httpsJwks.setSimpleHttpGet(getSimpleGet());
            httpsJwks.setRetainCacheOnErrorDuration(3600);  //hold for 1 hour

            resolver = new HttpsJwksVerificationKeyResolver(httpsJwks);
        }
        else if (jwks != null)
        {
            try
            {
                JsonWebKeySet jsonKeySet = new JsonWebKeySet(jwks);
                resolver = new JwksVerificationKeyResolver(jsonKeySet.getJsonWebKeys());
            }
            catch (JoseException e)
            {
                LOG.error(e.getMessage());
            }
        }

        return resolver;
    }

    /**
     * This method performs hostname verification for the configured JWKS URI.
     */
    private Get getSimpleGet()
    {
        Get get = new Get();
        final TrustedCAAccessor trustedCAAccessor = new TrustedCAAccessor();
        final Set<TrustAnchor> allTrustAnchors = trustedCAAccessor.getAllTrustAnchors();
        Collection<X509Certificate> trustedCertificates = new ArrayList<>();
        for (TrustAnchor ta : allTrustAnchors)
        {
            trustedCertificates.add(ta.getTrustedCert());
        }
        get.setHostnameVerifier(SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        get.setTrustedCertificates(trustedCertificates);

        return get;
    }

    private Set<String> getStandardClaims()
    {
        Set<String> values = new HashSet<>();
        for (DynamicClientFields c : DynamicClientFields.values())
        {
            values.add(c.getName());
        }
        return values;
    }

    /**
     * This method processes a claim by translating it to appropriate value(s) of a
     * DynamicClient domain object.
     *
     * @param dynamicClient the {@link DynamicClient} received and parsed by the request
     * @param claimName     the claim name to process
     * @param jwtClaims     thhe JWT Clames object
     * @throws MalformedClaimException if the {@param claimName} processing fails
     */
    private void processStandardClaim(DynamicClient dynamicClient, DynamicClientFields claimName, JwtClaims jwtClaims)
            throws MalformedClaimException
    {
        switch (claimName)
        {
            case REDIRECT_URIS:
                List<String> redirectUris = getStringListClaimValue(jwtClaims, DynamicClientFields.REDIRECT_URIS.getName());
                if (redirectUris != null)
                {
                    dynamicClient.setRedirectUris(redirectUris);
                }
                break;
            case TOKEN_ENDPOINT_AUTH_METHOD:
                String tokenEndpointAuthMethod = jwtClaims.getStringClaimValue(DynamicClientFields.TOKEN_ENDPOINT_AUTH_METHOD.getName());
                dynamicClient.setClientAuthenticationType(tokenEndpointAuthMethod);
                if (tokenEndpointAuthMethod.equalsIgnoreCase(ClientAuthType.client_secret_basic.toString()) ||
                    tokenEndpointAuthMethod.equalsIgnoreCase(ClientAuthType.client_secret_post.toString()))
                {
                    dynamicClient.generateSecret(22);
                }
                break;
            case TLS_CLIENT_AUTH_SUBJECT_DN:
                dynamicClient.setClientCertSubjectDn(jwtClaims.getStringClaimValue(DynamicClientFields.TLS_CLIENT_AUTH_SUBJECT_DN.getName()));
                break;
            case ID_TOKEN_SIGNED_RESPONSE_ALG:
                dynamicClient.setIdTokenSigningAlgorithm(jwtClaims.getStringClaimValue(DynamicClientFields.ID_TOKEN_SIGNED_RESPONSE_ALG.getName()));
                break;
            case ID_TOKEN_ENCRYPTED_RESPONSE_ALG:
                dynamicClient.setIdTokenEncryptionAlgorithm(jwtClaims.getStringClaimValue(DynamicClientFields.ID_TOKEN_ENCRYPTED_RESPONSE_ALG.getName()));
                break;
            case ID_TOKEN_ENCRYPTED_RESPONSE_ENC:
                dynamicClient.setIdTokenContentEncryptionAlgorithm(jwtClaims.getStringClaimValue(DynamicClientFields.ID_TOKEN_ENCRYPTED_RESPONSE_ENC.getName()));
                break;
            case GRANT_TYPES:
                List<String> grantTypes = getStringListClaimValue(jwtClaims, DynamicClientFields.GRANT_TYPES.getName());
                if (grantTypes != null)
                {
                    dynamicClient.setGrantTypes(new HashSet<>(grantTypes));
                }
                break;
            case CLIENT_NAME:
                dynamicClient.setName(jwtClaims.getStringClaimValue(DynamicClientFields.CLIENT_NAME.getName()));
                break;
            case LOGO_URI:
                dynamicClient.setLogoUrl(jwtClaims.getStringClaimValue(DynamicClientFields.LOGO_URI.getName()));
                break;
            case SCOPE:
                String scopeClaim = jwtClaims.getStringClaimValue(DynamicClientFields.SCOPE.getName());
                dynamicClient.setScopes(new ArrayList<>(Arrays.asList(scopeClaim.split(" "))));
                break;
            case AUTHORIZATION_DETAILS_TYPES:
                List<String> authorizationDetailsTypesClaim
                        = getStringListClaimValue(jwtClaims, DynamicClientFields.AUTHORIZATION_DETAILS_TYPES.getName());
                if (authorizationDetailsTypesClaim != null)
                {
                    dynamicClient.setAllowedAuthorizationDetailsTypes(authorizationDetailsTypesClaim);
                }
                break;
            case JWKS_URI:
                dynamicClient.setJwksUrl(jwtClaims.getStringClaimValue(DynamicClientFields.JWKS_URI.getName()));
                break;
            case JWKS:
                dynamicClient.setJwks(jwtClaims.getStringClaimValue(DynamicClientFields.JWKS.getName()));
                break;
            case RESPONSE_TYPES:
                List<String> responseTypes = getStringListClaimValue(jwtClaims, DynamicClientFields.RESPONSE_TYPES.getName());
                if (responseTypes != null)
                {
                    dynamicClient.setRestrictedResponseTypes(responseTypes);
                }
                break;
            case REQUEST_OBJECT_SIGNING_ALG:
                final String requestObjectSigningAlgClaim =
                        jwtClaims.getStringClaimValue(DynamicClientFields.REQUEST_OBJECT_SIGNING_ALG.getName());
                dynamicClient.setRequestObjectSigningAlgorithm(requestObjectSigningAlgClaim);
                break;
            case TOKEN_ENDPOINT_AUTH_SIGNING_ALG:
                final String tokenEndpointAuthSigningAlgClaim =
                        jwtClaims.getStringClaimValue(DynamicClientFields.TOKEN_ENDPOINT_AUTH_SIGNING_ALG.getName());
                dynamicClient.setTokenEndpointAuthSigningAlgorithm(tokenEndpointAuthSigningAlgClaim);
                break;
            case BACKCHANNEL_TOKEN_DELIVERY_MODE:
                String deliveryMode = jwtClaims.getStringClaimValue(DynamicClientFields.BACKCHANNEL_TOKEN_DELIVERY_MODE.getName());
                dynamicClient.setCibaDeliveryMode(deliveryMode);
                break;
            case BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT:
                String notificationEndpoint = jwtClaims.getStringClaimValue(DynamicClientFields.BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.getName());
                dynamicClient.setCibaNotificationEndpoint(notificationEndpoint);
                break;
            case BACKCHANNEL_USER_CODE_PARAMETER:
                Boolean userCode = jwtClaims.getClaimValue(DynamicClientFields.BACKCHANNEL_USER_CODE_PARAMETER.getName(), Boolean.class);
                boolean value = userCode != null ? userCode : false;
                dynamicClient.setCibaSupportUserCode(value);
                break;
            case BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG:
                String backchannelAuthRequestSigningAlg = jwtClaims.getStringClaimValue(DynamicClientFields.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.getName());
                dynamicClient.setCibaRequestObjectSigningAlgorithm(backchannelAuthRequestSigningAlg);
                break;
            case SUBJECT_TYPE:
                String subjectType = jwtClaims.getStringClaimValue(DynamicClientFields.SUBJECT_TYPE.getName());
                boolean pairwiseUserType = UserIdType.PAIRWISE.equals(subjectType);
                dynamicClient.setPairwiseUserType(pairwiseUserType);
                break;
            case SECTOR_IDENTIFIER_URI:
                dynamicClient.setSectorIdentifierUri(jwtClaims.getStringClaimValue(DynamicClientFields.SECTOR_IDENTIFIER_URI.getName()));
                break;
            case INTROSPECTION_SIGNED_RESPONSE_ALG:
                final String signAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.INTROSPECTION_SIGNED_RESPONSE_ALG.getName());
                dynamicClient.setIntrospectionSigningAlgorithm(signAlgorithm);
                break;
            case INTROSPECTION_ENCRYPTED_RESPONSE_ALG:
                final String encryptionAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.INTROSPECTION_ENCRYPTED_RESPONSE_ALG.getName());
                dynamicClient.setIntrospectionEncryptionAlgorithm(encryptionAlgorithm);
                break;
            case INTROSPECTION_ENCRYPTED_RESPONSE_ENC:
                final String contentEncryptionAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.INTROSPECTION_ENCRYPTED_RESPONSE_ENC.getName());
                dynamicClient.setIntrospectionContentEncryptionAlgorithm(contentEncryptionAlgorithm);
                break;
            case AUTHORIZATION_SIGNED_RESPONSE_ALG:
                final String authorizationResponseSigningAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.AUTHORIZATION_SIGNED_RESPONSE_ALG.getName());
                dynamicClient.setAuthorizationResponseSigningAlgorithm(authorizationResponseSigningAlgorithm);
                break;
            case AUTHORIZATION_ENCRYPTED_RESPONSE_ALG:
                final String authorizationResponseEncryptionAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG.getName());
                dynamicClient.setAuthorizationResponseEncryptionAlgorithm(authorizationResponseEncryptionAlgorithm);
                break;
            case AUTHORIZATION_ENCRYPTED_RESPONSE_ENC:
                final String authorizationResponseContentEncryptionAlgorithm =
                        jwtClaims.getStringClaimValue(DynamicClientFields.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC.getName());
                dynamicClient.setAuthorizationResponseContentEncryptionAlgorithm(authorizationResponseContentEncryptionAlgorithm);
                break;
            case BACKCHANNEL_LOGOUT_URI:
                final String backChannelLogoutUri =
                        jwtClaims.getStringClaimValue(DynamicClientFields.BACKCHANNEL_LOGOUT_URI.getName());
                dynamicClient.setBackChannelLogoutUri(backChannelLogoutUri);
                break;
            case FRONTCHANNEL_LOGOUT_URI:
                final String frontChannelLogoutUri =
                        jwtClaims.getStringClaimValue(DynamicClientFields.FRONTCHANNEL_LOGOUT_URI.getName());
                dynamicClient.setFrontChannelLogoutUri(frontChannelLogoutUri);
                break;
            case POST_LOGOUT_REDIRECT_URIS:
                List<String> postLogoutRedirectUris = getStringListClaimValue(jwtClaims, DynamicClientFields.POST_LOGOUT_REDIRECT_URIS.getName());
                if (postLogoutRedirectUris != null)
                {
                    dynamicClient.setPostLogoutRedirectUris(postLogoutRedirectUris);
                }
                break;
            case CLIENT_DESCRIPTION:
                dynamicClient.setDescription(jwtClaims.getStringClaimValue(DynamicClientFields.CLIENT_DESCRIPTION.getName()));
                break;
            case DPOP_BOUND_ACCESS_TOKENS:
                Boolean booleanValue = jwtClaims.getClaimValue(DynamicClientFields.DPOP_BOUND_ACCESS_TOKENS.getName(), Boolean.class);
                boolean requireDpop = booleanValue != null ? booleanValue : false;
                dynamicClient.setRequireDpop(requireDpop);
                break;
            default:
                //do nothing: we only consider the OAuth spec and PF proprietary attributes
                // and the rest are treated as extended metadata
                break;
        }

    }

    /**
     * This method processes extended client metadata in claims.
     *
     * @param dynamicClient the {@link DynamicClient} received and parsed by the request
     * @param claimName     the claim name to process
     * @param jwtClaims     thhe JWT Clames object
     * @throws MalformedClaimException if the {@param claimName} processing fails
     */
    private void processExtendedClaims(DynamicClient dynamicClient, String claimName, JwtClaims jwtClaims)
            throws MalformedClaimException
    {
        Object claimValue = jwtClaims.getClaimValue(claimName);
        if (claimValue != null)
        {
            if (claimValue instanceof List)
            {
                List<String> stringListClaimValue = jwtClaims.getStringListClaimValue(claimName);
                DynamicClient.Status status = dynamicClient.addClientMetadataValues(claimName, stringListClaimValue);
                if (!status.equals(DynamicClient.Status.SUCCESS))
                {
                    LOG.error("claim [" + claimName + "] was not added to the client object with the status of [" + status + "]");
                }
            }
            else if (claimValue instanceof String)
            {
                String stringClaimValue = jwtClaims.getStringClaimValue(claimName);
                DynamicClient.Status status = dynamicClient.addClientMetadataValues(claimName, Collections.singletonList(stringClaimValue));
                if (!status.equals(DynamicClient.Status.SUCCESS))
                {
                    LOG.error("claim [" + claimName + "] was not added to the client object with the status of [" + status + "]");
                }
            }
            else
            {
                throw new MalformedClaimException("claim [" + claimName + "] value is not supported.");
            }
        }
    }

    /**
     * This method returns claim value as a List of String of a claim with the name matching
     * the input claimName.
     *
     * @param jwtClaims all the claims from a JWT
     * @param claimName name of the claim
     * @return a claim value list
     */
    private List<String> getStringListClaimValue(JwtClaims jwtClaims, String claimName)
    {
        try
        {
            if (jwtClaims.hasClaim(claimName))
            {
                if (jwtClaims.isClaimValueOfType(claimName, List.class))
                {
                    return jwtClaims.getStringListClaimValue(claimName);
                }
                else
                {
                    String value = jwtClaims.getStringClaimValue(claimName);
                    return Arrays.asList(value.split(" "));
                }
            }
        }
        catch (MalformedClaimException e)
        {
            LOG.error("claim [" + claimName + "] was not processed due to a data conversion error.");
            LOG.debug(e.getMessage());
        }
        return null;
    }
}
