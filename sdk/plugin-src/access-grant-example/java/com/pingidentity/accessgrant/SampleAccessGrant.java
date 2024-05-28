/*
 * **************************************************
 *  Copyright (C) 2019 Ping Identity Corporation
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

package com.pingidentity.accessgrant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.pingidentity.sdk.accessgrant.AccessGrantCriteria;
import org.sourceid.oauth20.token.TokenUtil;

import com.pingidentity.sdk.accessgrant.AccessGrant;
import com.pingidentity.sdk.accessgrant.AccessGrantAttributesHolder;
import com.pingidentity.sdk.accessgrant.AccessGrantManager;
import com.pingidentity.sdk.accessgrant.exception.AccessGrantManagementException;
import com.pingidentity.sdk.oauth20.Scope;

/**
 * This class provides a sample implementation for access grants. It uses a
 * HashMap to store the access grants with the key being the access grant guid. 
 * All modifications, such as updates, revocations, etc. are all done on this HashMap.
 * 
 * To use this implementation, modify ./server/default/conf/service-points.conf and change:
 * 
 *   # Service for storage of access grants
 *   access.grant.manager=org.sourceid.oauth20.token.AccessGrantManagerJdbcImpl
 * 
 *  to become:
 *
 *   # Service for storage of access grants
 *   access.grant.manager=com.pingidentity.accessgrant.SampleAccessGrant
 *   
 *   Follow the instructions in the SDK Developer's Guide for building and deploying.
 *
 *  Production use of this sample is not recommended, since this HashMap
 *  based implementation stores grants in memory local to a given
 *  PingFederate server. Thus, this implementation can not support HA
 *  or DR architectures.
 */

public class SampleAccessGrant implements AccessGrantManager
{
    /**
     * A class to hold the access grant itself, along with the optional access
     * grant attributes.
     */
    static class AccessGrantWithAttributes {
        AccessGrant accessGrant;
        AccessGrantAttributesHolder attributes;
    }
    
    /**
     * The HashMap which stores all access grants using the guid as the key.
     */
    private HashMap<String, AccessGrantWithAttributes> accessGrants = new HashMap<>();
    
    
    /**
     * Saves the access grant to the underlying storage mechanism.
     *
     * @param accessGrant
     *            the access grant that is to be persisted.
     * @param accessGrantAttributesHolder
     *            a list of optional access grant attributes to be stored with the access grant.
     * @throws AccessGrantManagementException
     *            A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void saveGrant(AccessGrant accessGrant, AccessGrantAttributesHolder accessGrantAttributesHolder) throws AccessGrantManagementException
    {
        AccessGrantWithAttributes accessGrantWithAttributes = new AccessGrantWithAttributes();
        accessGrantWithAttributes.accessGrant = accessGrant;
        accessGrantWithAttributes.attributes = accessGrantAttributesHolder;
        
        accessGrants.put(accessGrant.getGuid(), accessGrantWithAttributes);
    }


    /**
     * PingFederate periodically calls this method (once per day by default) to clear out expired access grants.
     * When this method is invoked, it cycles through all access grants and delete any that are expired.
     *
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void deleteExpiredGrants()
    {
        List<String> removeGuids = new ArrayList<>();
        
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getExpires() < System.currentTimeMillis()) {
                removeGuids.add(accessGrantWithAttributes.accessGrant.getGuid());
            }
        }
        
        for (String guid : removeGuids) {
            accessGrants.remove(guid);
        }
    }

    
    /**
     * Retrieves a collection of access grants based on the client id.
     *
     * @param clientId
     *            the client id to retrieve access grants for.
     * @return A collection of access grants which match the client id.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public Collection<AccessGrant> getByClientId(String clientId) throws AccessGrantManagementException
    {
        List<AccessGrant> accessGrantsForReturn = new ArrayList<>();
        
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getClientId().equals(clientId)) {
                accessGrantsForReturn.add(accessGrantWithAttributes.accessGrant);
            }
        }
        
        return accessGrantsForReturn;
    }

    
    /**
     * Retrieves an AccessGrant by its guid.
     *
     * @param guid
     *            the guid to search for.
     * @return An AccessGrant with a guid matching the param.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public AccessGrant getByGuid(String guid) throws AccessGrantManagementException
    {
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getGuid().equals(guid)) {
                return accessGrantWithAttributes.accessGrant;
            }
        }
        
        return null;
    }
    

    /**
     * Retrieves an AccessGrant by its refresh token value.
     *
     * @param refreshToken
     *            the refresh token to search for.
     * @return An AccessGrant with a refresh token value matching the param.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public AccessGrant getByRefreshToken(String refreshToken) throws AccessGrantManagementException
    {
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getHashedRefreshTokenValue() != null &&
                accessGrantWithAttributes.accessGrant.getHashedRefreshTokenValue().equals(TokenUtil.digestToken(refreshToken))) {
                return accessGrantWithAttributes.accessGrant;
            }
        }
        
        return null;
    }

    
    /**
     * Retrieves an access grant based on the user key, which is the unique user identifier.
     *
     * @param userKey
     *            the unique user identifier to retrieve access grants for.
     * @return A collection of access grants which match the user key.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public Collection<AccessGrant> getByUserKey(String userKey) throws AccessGrantManagementException
    {
        List<AccessGrant> accessGrantsForReturn = new ArrayList<>();
        
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getUniqueUserIdentifer().equals(userKey)) {
                accessGrantsForReturn.add(accessGrantWithAttributes.accessGrant);
            }
        }
        
        return accessGrantsForReturn;
    }

    /**
     * Retrieves an access grant based based on several specific criteria specified in the {@link AccessGrantCriteria}.
     *
     * @param accessGrantCriteria
     *            the access grant criteria to retrieve access grants for.
     * @return An access grant that matches the search criteria.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public AccessGrant getByAccessGrantCriteria(AccessGrantCriteria accessGrantCriteria)
            throws AccessGrantManagementException
    {
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getUniqueUserIdentifer().equals(accessGrantCriteria.getUserKey()) &&
                accessGrantWithAttributes.accessGrant.getScope().getScopeStr().equals(accessGrantCriteria.getScope().getScopeStr()) &&
                accessGrantWithAttributes.accessGrant.getClientId().equals(accessGrantCriteria.getClientId()) &&
                accessGrantWithAttributes.accessGrant.getGrantType().equals(accessGrantCriteria.getGrantType()) &&
                accessGrantWithAttributes.accessGrant.getContextualQualifier().equals(accessGrantCriteria.getContextQualifier()) &&
                accessGrantWithAttributes.accessGrant.getAuthorizationDetails().toJson().equals(accessGrantCriteria.getAuthorizationDetails().toJson()))
            {
                return accessGrantWithAttributes.accessGrant;
            }
        }

        return null;
    }

    /**
     * Retrieves an access grant based based on several specific criteria specified in the params.
     *
     * @param userKey
     *            the unique user identifier to retrieve access grants for.
     * @param scope
     *            the scope to retrieve access grants for.
     * @param clientId
     *            the client id to retrieve access grants for.
     * @param grantType
     *            the grant type to retrieve access grants for.
     * @param context
     *            the contextual qualifier to retrieve access grants for.
     * @return An access grant that matches the search criteria in the parameters.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public AccessGrant getByUserKeyScopeClientIdGrantTypeContext(String userKey, Scope scope, String clientId, String grantType, String context) throws AccessGrantManagementException
    {
        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values()) {
            if (accessGrantWithAttributes.accessGrant.getUniqueUserIdentifer().equals(userKey) &&
                accessGrantWithAttributes.accessGrant.getScope().getScopeStr().equals(scope.getScopeStr()) &&    
                accessGrantWithAttributes.accessGrant.getClientId().equals(clientId) &&    
                accessGrantWithAttributes.accessGrant.getGrantType().equals(grantType) &&    
                accessGrantWithAttributes.accessGrant.getContextualQualifier().equals(context)) 
            {
                return accessGrantWithAttributes.accessGrant;
            }
        }
        
        return null;
    }

    
    /**
     * Retrieve the access grant attributes for the specified access grant guid.
     *
     * @param accessGrantGuid
     *      the access grant guid to retrieve attributes for.
     * @return Access grant attributes.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public AccessGrantAttributesHolder getGrantAttributes(String accessGrantGuid) throws AccessGrantManagementException
    {
        return accessGrants.get(accessGrantGuid).attributes;
    }

    
    /**
     * Determines if the specified data source is in use. Unless you are using the built-in JDBC, LDAP or
     * custom data stores within PingFederate, this can simply return false.
     *
     * @param datasourceId
     *            the datasource id in use.
     * @return true if the data source is in use, false otherwise.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public boolean isDataSourceInUse(String datasourceId)
    {
        return false;
    }

    
    /**
     * Revokes an access grant that has been previously issued.
     *
     * @param guid
     *            the guid of the access grant that is to be revoked.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void revokeGrant(String guid) throws AccessGrantManagementException
    {
        accessGrants.remove(guid);
    }

    
    /**
     * Revokes an access grant that has been previously issued.
     *
     * @param userKey
     *            the user identifier of the access grant to be revoked.
     * @param guid
     *            the guid of the access grant that is to be revoked.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void revokeGrant(String userKey, String guid) throws AccessGrantManagementException
    {
        AccessGrantWithAttributes accessGrant = accessGrants.get(guid);
        
        if (accessGrant != null && accessGrant.accessGrant.getUniqueUserIdentifer().equals(userKey)) {
            accessGrants.remove(guid);
        }
    }

    
    /**
     * Updates the access grant attributes with the specified attributes.
     *
     * @param accessGrantGuid
     *            the guid of the access grant that is to be updated.
     * @param attributes
     *            a list of optional access grant attributes that are to be updated within the access grant.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void updateGrantAttributes(String accessGrantGuid, AccessGrantAttributesHolder attributes) throws AccessGrantManagementException
    {
        AccessGrantWithAttributes accessGrantWithAttributes = accessGrants.get(accessGrantGuid);

        if (accessGrantWithAttributes != null) {
            accessGrantWithAttributes.attributes = attributes;
            accessGrants.put(accessGrantGuid, accessGrantWithAttributes);
        }
    }

    
    /**
     * Update the refresh token of the specified access grant. Use the guid specified in the accessGrant param
     * as the key to perform the update. The hashedRefreshTokenValue variable passed in the accessGrant will contain
     * the new refresh value. Implementations are responsible for ensuring the updated timestamp on persisted access
     * grants is current.
     *
     * @param accessGrant
     *            the access grant to update which contains the new refresh token value.
     * @throws AccessGrantManagementException
     *             A general exception for any unexpected runtime errors that might occur during an access grant operation.
     */
    @Override
    public void updateRefreshToken(AccessGrant accessGrant) throws AccessGrantManagementException
    {
        AccessGrantWithAttributes existingAccessGrant = accessGrants.get(accessGrant.getGuid());

        if (existingAccessGrant != null) {
            AccessGrant newAccessGrant = new AccessGrant(
                accessGrant.getHashedRefreshTokenValue(),
                existingAccessGrant.accessGrant.getGuid(),
                existingAccessGrant.accessGrant.getUniqueUserIdentifer(),
                existingAccessGrant.accessGrant.getGrantType(),
                existingAccessGrant.accessGrant.getScope(),
                existingAccessGrant.accessGrant.getClientId(),
                existingAccessGrant.accessGrant.getIssued(),
                System.currentTimeMillis(),
                existingAccessGrant.accessGrant.getExpires(),
                existingAccessGrant.accessGrant.getContextualQualifier());
            newAccessGrant.setAuthorizationDetails(existingAccessGrant.accessGrant.getAuthorizationDetails());

            AccessGrantWithAttributes accessGrantWithAttributes = new AccessGrantWithAttributes();
            accessGrantWithAttributes.accessGrant = newAccessGrant;
            accessGrantWithAttributes.attributes = existingAccessGrant.attributes;
            
            accessGrants.put(existingAccessGrant.accessGrant.getGuid(), accessGrantWithAttributes);
        }
    }

    /**
     * Update the expiry time of the specified access grant. Use the guid specified in the accessGrant param
     * as the key to perform the update. The expires variable passed in the accessGrant will contain
     * the expiry time. Implementations are responsible for ensuring the updated timestamp on persisted access
     * grants is current.
     *
     * @param accessGrant
     *            the access grant to update which contains the new expiry time.
     * @throws AccessGrantManagementException
     *             Runtime exception to indicate the retrieval of the AccessGrant
     *             has failed.
     * @since 12.0
     */
    @Override
    public void updateExpiry(AccessGrant accessGrant) throws AccessGrantManagementException
    {
        AccessGrantWithAttributes existingAccessGrant = accessGrants.get(accessGrant.getGuid());

        if (existingAccessGrant != null) {
            AccessGrant newAccessGrant = new AccessGrant(
                    existingAccessGrant.accessGrant.getHashedRefreshTokenValue(),
                    existingAccessGrant.accessGrant.getGuid(),
                    existingAccessGrant.accessGrant.getUniqueUserIdentifer(),
                    existingAccessGrant.accessGrant.getGrantType(),
                    existingAccessGrant.accessGrant.getScope(),
                    existingAccessGrant.accessGrant.getClientId(),
                    existingAccessGrant.accessGrant.getIssued(),
                    System.currentTimeMillis(),
                    accessGrant.getExpires(),
                    existingAccessGrant.accessGrant.getContextualQualifier());
            newAccessGrant.setAuthorizationDetails(existingAccessGrant.accessGrant.getAuthorizationDetails());

            AccessGrantWithAttributes accessGrantWithAttributes = new AccessGrantWithAttributes();
            accessGrantWithAttributes.accessGrant = newAccessGrant;
            accessGrantWithAttributes.attributes = existingAccessGrant.attributes;

            accessGrants.put(existingAccessGrant.accessGrant.getGuid(), accessGrantWithAttributes);
        }
    }

    /**
     * Retrieves a collection of access grants based on several specific criteria specified in the params.
     *
     * @param userKey
     *            the unique user identifier to retrieve access grants for.
     * @param clientId
     *            the client id to retrieve access grants for.
     * @param grantType
     *            the grant type to retrieve access grants for.
     * @throws AccessGrantManagementException
     *             Runtime exception to indicate the retrieval of the AccessGrant
     *             has failed.
     * @since 12.0
     */
    @Override
    public Collection<AccessGrant> getByUserKeyClientIdGrantType(String userKey, String clientId, String grantType) throws AccessGrantManagementException
    {
        List<AccessGrant> accessGrantsForReturn = new ArrayList<>();

        for (AccessGrantWithAttributes accessGrantWithAttributes : accessGrants.values())
        {
            if (accessGrantWithAttributes.accessGrant.getUniqueUserIdentifer().equals(userKey) &&
                accessGrantWithAttributes.accessGrant.getClientId().equals(clientId) &&
                accessGrantWithAttributes.accessGrant.getGrantType().equals(grantType))
            {
                accessGrantsForReturn.add(accessGrantWithAttributes.accessGrant);
            }
        }

        return accessGrantsForReturn;
    }
}
