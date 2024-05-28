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

package com.pingidentity.clientstorage;

import com.google.common.collect.ComparisonChain;
import com.pingidentity.sdk.SearchCriteria;
import com.pingidentity.sdk.oauth20.ClientData;
import com.pingidentity.sdk.oauth20.ClientStorageManagementException;
import com.pingidentity.sdk.oauth20.ClientStorageManagerBase;
import com.pingidentity.sdk.oauth20.ClientStorageManagerV2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * This class provides a sample implementation for OAuth client storage. It uses a HashMap to store client data with
 * the key being the client ID. All CRUD operations (create, read, update and delete) are performed on this HashMap.
 * A real implementation should persist data to an external store. Additional indexes and fields to filter the
 * the data would be required to optimize the search method invoked by the administrative console.
 *
 * To use this implementation, modify the file ./server/default/conf/service-points.conf as follows:
 *
 * Two service points "client.manager" and "client.storage.manager" to become:
 *
 *        client.storage.manager=com.pingidentity.clientstorage.SampleClientStorage
 *
 *        client.manager=org.sourceid.oauth20.domain.ClientManagerGenericImpl
 *
 * Follow the instructions in the SDK Developer's Guide for building and deploying.
 *
 * Production use of this sample is not recommended as this implementation stores clients in memory local to a given
 * PingFederate server. As such this implementation can not support HA or DR architectures.
 */
public class SampleClientStorage extends ClientStorageManagerBase
{
    /**
     * Inner class representing the data to be persisted per client entry. Note that client name and last modified
     * fields only need to be persisted to implement an optimized search method.
     */
    private static class StoredClientData
    {
        private final String clientData;
        private final String clientName;
        private final Date clientLastModified;
        private final Date clientCreationTime;
        
        public StoredClientData(String data, String name, Date lastModified, Date creationTime)
        {
            clientData = data;
            clientName = name;
            clientLastModified = lastModified;
            clientCreationTime = creationTime;
        }
    }
    
    /**
     * The HashMap to simulate persisting client data to a store. Client ID is used as the index key. The schema in a
     * real data store should be designed to also offer searching on client name and last modified fields.
     */
    private HashMap<String, StoredClientData> storedClientDataHashMap = new HashMap<>();

    /**
     * Retrieves a client record by client ID.
     *
     * @param clientId The client ID.
     * @return A matching ClientData object. Returns null if the clientId is not found.
     * @throws com.pingidentity.sdk.oauth20.ClientStorageManagementException
     *              Checked exception to indicate the retrieval of client record has failed.
     */
    @Override
    public ClientData getClient(String clientId) throws ClientStorageManagementException
    {
        ClientData clientData = null;
    
        StoredClientData storedClientData = storedClientDataHashMap.get(clientId);
    
        if (storedClientData != null)
        {
            clientData = new ClientData();
            clientData.setData(storedClientData.clientData);
        }
        
        return clientData;
    }

    /**
     * Retrieves all client records.
     *
     * @return A collection of all client records.
     * @throws com.pingidentity.sdk.oauth20.ClientStorageManagementException
     *              Checked exception to indicate the retrieval of client records has failed.
     */
    @Override
    public Collection<ClientData> getClients() throws ClientStorageManagementException
    {
        HashMap<String, ClientData> fullClientDataMap = new HashMap<>();
    
        for (Map.Entry<String, StoredClientData> entry : storedClientDataHashMap.entrySet())
        {
            StoredClientData storedClientData = entry.getValue();
            ClientData clientData = new ClientData();
            clientData.setData(storedClientData.clientData);
            
            fullClientDataMap.put(entry.getKey(), clientData);
        }
        
        return fullClientDataMap.values();
    }

    /**
     * Add a client record.
     *
     * @param client The client object.
     * @throws com.pingidentity.sdk.oauth20.ClientStorageManagementException
     *              Checked exception to indicate that the operation of adding a client record has failed.
     */
    @Override
    public void addClient(ClientData client) throws ClientStorageManagementException
    {
        StoredClientData storedClientData = new StoredClientData(client.getData(), client.getName(),
                                                                 client.getLastModified(), client.getCreationTime());
        
        storedClientDataHashMap.put(client.getId(), storedClientData);
    }

    /**
     * Delete a client record.
     *
     * @param clientId The client ID.
     * @throws com.pingidentity.sdk.oauth20.ClientStorageManagementException
     *              Checked exception to indicate that the operation of removing a client record has failed.
     */
    @Override
    public void deleteClient(String clientId) throws ClientStorageManagementException
    {
        storedClientDataHashMap.remove(clientId);
    }

    /**
     * Updating a client record.
     *
     * @param client
     *              The client object.
     * @throws com.pingidentity.sdk.oauth20.ClientStorageManagementException
     *              Checked exception to indicate that the operation of updating a client record has failed.
     */
    @Override
    public void updateClient(ClientData client) throws ClientStorageManagementException
    {
        StoredClientData storedClientData = new StoredClientData(client.getData(), client.getName(),
                                                                 client.getLastModified(), client.getCreationTime());
    
        storedClientDataHashMap.put(client.getId(), storedClientData);
    }

    /**
     * Search for a client based on SearchCriteria. Implementation of this method is optional - it is only needed
     * for optimized administrative console performance on large scale client stores. This method is not invoked at
     * runtime by engine nodes.
     *
     * @param searchCriteria
     * @return
     * @throws ClientStorageManagementException
     */
    @Override
    public Collection<ClientData> search(SearchCriteria searchCriteria)
            throws ClientStorageManagementException

    {
        // Note: This search implementation is for illustrative purposes only - it is not optimized for performance.
        // It could have been omitted to rely on the method provided by the ClientStorageManagerBase parent class,
        // which would offer comparable performance. A real implementation should send queries with filtering and
        // sorting options to the connected data store.
        
        Collection<ClientData> searchResults = new ArrayList<>();
    
        for (Map.Entry<String, StoredClientData> entry : storedClientDataHashMap.entrySet())
        {
            StoredClientData storedClientData = entry.getValue();
            
            String searchCriteriaString = searchCriteria.getQuery();
            
            if (StringUtils.isBlank(searchCriteria.getQuery()) ||
                    entry.getKey().toLowerCase().contains(searchCriteriaString.toLowerCase()) ||
                    storedClientData.clientName.toLowerCase().contains(searchCriteriaString.toLowerCase()))
            {
                ClientData clientData = new ClientData();
                clientData.setData(storedClientData.clientData);
                
                // Note: For convenience in this example we set the values in the ClientData structure for ID, name
                // and last modified. Normally this would be part of the query sent to the connected data store.
                clientData.setId(entry.getKey());
                clientData.setName(storedClientData.clientName);
                clientData.setLastModified(storedClientData.clientLastModified);
                clientData.setCreationTime(storedClientData.clientCreationTime);
                
                searchResults.add(clientData);
            }
        }

        if (searchCriteria.getStartIndex() >= searchResults.size())
        {
            return Collections.emptyList();
        }

        List<SearchCriteria.OrderByItem> orderByList = searchCriteria.getOrderBy();
        SearchCriteria.OrderByItem orderBy = orderByList.get(0);    //order by 1 field. possible to order by more if needed

        List<ClientData> resultsArray = new ArrayList<>(searchResults);
        Collections.sort(resultsArray, new ClientComparator(orderBy.getSortFieldName(), orderBy.getOrder()));

        int fromIndex = searchCriteria.getStartIndex();
        int toIndex = searchCriteria.getStartIndex() + searchCriteria.getItemsRequested() >= resultsArray.size()
                ? resultsArray.size()
                : searchCriteria.getStartIndex() + searchCriteria.getItemsRequested();
        return resultsArray.subList(fromIndex, toIndex);
    }

    /**
     * Comparator class used for sorting {@link ClientData}.
     * <p>
     * Clients can be sorted by
     * <ul>
     *  <li>{@link ClientStorageManagerV2#CLIENT_ID}</li>
     *  <li>{@link ClientStorageManagerV2#CLIENT_NAME}</li>
     *  <li>{@link ClientStorageManagerV2#LAST_MODIFIED_DATE}</li>
     *  <li>{@link ClientStorageManagerV2#CREATION_DATE}</li>
     *  </ul>
     *  </p>
     */
    private static class ClientComparator implements Comparator<ClientData>, Serializable
    {
        private final String sortFieldName;
        private final SearchCriteria.Order order;
    
        public ClientComparator(String sortFieldName, SearchCriteria.Order order)
        {
            this.sortFieldName = sortFieldName;
            this.order = order;
        }
    
        @Override
        public int compare(ClientData c1, ClientData c2)
        {
            int reverse = order == SearchCriteria.Order.ASC ? 1 : -1;
            switch (sortFieldName)
            {
                case CLIENT_ID:
                    return c1.getId().compareToIgnoreCase(c2.getId()) * reverse;
                case CLIENT_NAME:
                    return c1.getName().compareToIgnoreCase(c2.getName()) * reverse;
                case LAST_MODIFIED_DATE:
                    return ObjectUtils.compare(c1.getLastModified(), c2.getLastModified()) * reverse;
                case CREATION_DATE:
                    return ComparisonChain.start()
                              .compare(c1.getCreationTime(), c2.getCreationTime(), (t1, t2) -> ObjectUtils.compare(t1, t2) * reverse)
                              .compare(c1.getName(), c2.getName(), String::compareToIgnoreCase)
                              .result();
                default:
                    return 0;
            }
        }
    }
}