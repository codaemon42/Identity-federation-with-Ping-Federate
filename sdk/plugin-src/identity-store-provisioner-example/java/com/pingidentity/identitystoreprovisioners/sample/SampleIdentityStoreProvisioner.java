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
package com.pingidentity.identitystoreprovisioners.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sourceid.saml20.adapter.attribute.AttrValueSupport;
import org.sourceid.saml20.adapter.attribute.AttributeValue;
import org.sourceid.saml20.adapter.conf.Configuration;
import org.sourceid.saml20.adapter.gui.RadioGroupFieldDescriptor;
import org.sourceid.util.log.AttributeMap;

import com.pingidentity.sdk.GuiConfigDescriptor;
import com.pingidentity.sdk.IdentityStoreProvisionerDescriptor;
import com.pingidentity.sdk.PluginDescriptor;
import com.pingidentity.sdk.provision.Constants;
import com.pingidentity.sdk.provision.IdentityStoreProvisionerWithFiltering;
import com.pingidentity.sdk.provision.exception.ConflictException;
import com.pingidentity.sdk.provision.exception.IdentityStoreException;
import com.pingidentity.sdk.provision.exception.NotFoundException;
import com.pingidentity.sdk.provision.groups.request.CreateGroupRequestContext;
import com.pingidentity.sdk.provision.groups.request.DeleteGroupRequestContext;
import com.pingidentity.sdk.provision.groups.request.MemberAttribute;
import com.pingidentity.sdk.provision.groups.request.ReadGroupRequestContext;
import com.pingidentity.sdk.provision.groups.request.ReadGroupsRequestContext;
import com.pingidentity.sdk.provision.groups.request.UpdateGroupRequestContext;
import com.pingidentity.sdk.provision.groups.response.GroupResponseContext;
import com.pingidentity.sdk.provision.groups.response.GroupResponseContextImpl;
import com.pingidentity.sdk.provision.groups.response.GroupsResponseContext;
import com.pingidentity.sdk.provision.groups.response.GroupsResponseContextImpl;
import com.pingidentity.sdk.provision.users.request.CreateUserRequestContext;
import com.pingidentity.sdk.provision.users.request.DeleteUserRequestContext;
import com.pingidentity.sdk.provision.users.request.ReadUserRequestContext;
import com.pingidentity.sdk.provision.users.request.ReadUsersRequestContext;
import com.pingidentity.sdk.provision.users.request.UpdateUserRequestContext;
import com.pingidentity.sdk.provision.users.response.UserResponseContextImpl;
import com.pingidentity.sdk.provision.users.response.UsersResponseContextImpl;

/**
 * This class is an example of an identity store provisioner used to provision and deprovision groups and users to an
 * external store. It uses an in-memory cache to store users and groups that get created. And it supports the option of
 * deleting or disabling users on delete.
 */
public class SampleIdentityStoreProvisioner implements IdentityStoreProvisionerWithFiltering
{
    private static final Log log = LogFactory.getLog(SampleIdentityStoreProvisioner.class);

    // Plugin static String values.
    private static final String PLUGIN_TYPE = "Sample Identity Store Provisioner";
    private static final String PLUGIN_VERSION = "1.0";
    private static final String PLUGIN_DESCRIPTION = "This Identity Store Provisioner provides a means of persisting users and groups to a custom data source.";

    private static final String RADIO_BUTTON_NAME = "Delete user behavior";
    private static final String RADIO_BUTTON_DESCRIPTION = "Select whether a user should be disabled or permanently deleted when a delete request is sent to the plugin";

    // Constants for delete/disable radio options
    private static final String DELETE_USER = "Permanently Delete User";
    private static final String DISABLE_USER = "Disable User";

    // The active attribute defines whether the user in this implementation is enabled or disabled
    private static final String ACTIVE = "active";

    // The username is a required core contract attribute that must be fulfilled at runtime.
    private static final String USERNAME = "username";

    // The groupname is a required core group contract attribute that must be fulfilled at runtime.
    private static final String GROUPNAME = "groupname";

    // Static "User not found" exception message.
    private static final String USER_NOT_FOUND = "User not found";

    // Static "Group not found" exception message.
    private static final String GROUP_NOT_FOUND = "Group not found";

    // The PluginDescriptor that defines this plugin.
    private final IdentityStoreProvisionerDescriptor descriptor;

    // Runtime value of the delete/disable radio option in the plugin Admin UI.
    private boolean permanentlyDeleteUser;

    // For this sample implementation, the userCache models a user Identity Store in memory.
    private final ConcurrentHashMap<String, AttributeMap> userCache = new ConcurrentHashMap<>();

    // The groupCache models a group Identity Store in memory.
    private final ConcurrentHashMap<String, AttributeMap> groupCache = new ConcurrentHashMap<>();

    /**
     * Creates a new sample identity store provisioner and initialize its GUI descriptor.
     */
    public SampleIdentityStoreProvisioner()
    {
        super();

        // Construct a GuiConfigDescriptor to hold custom gui web controls
        GuiConfigDescriptor guiDescriptor = new GuiConfigDescriptor();

        // Add a description.
        guiDescriptor.setDescription(PLUGIN_DESCRIPTION);

        // Define a radio option for delete/disable.
        String[] options = { DISABLE_USER, DELETE_USER };

        // Define the disable/delete radio button controls displayed
        // in the Identity Store Plugin Admin UI for this plugin.
        RadioGroupFieldDescriptor disableOrDeleteRadioButtonDescriptor = new RadioGroupFieldDescriptor(
                RADIO_BUTTON_NAME, RADIO_BUTTON_DESCRIPTION, options);

        // Set the default value for the radio option.
        disableOrDeleteRadioButtonDescriptor.setDefaultValue(DISABLE_USER);

        // Add the field to the gui descriptor object.
        guiDescriptor.addField(disableOrDeleteRadioButtonDescriptor);

        // Load the guiDescriptor into the PluginDescriptor.
        descriptor = new IdentityStoreProvisionerDescriptor(PLUGIN_TYPE, this, guiDescriptor, new HashSet<String>(),
                PLUGIN_VERSION);

        // Add a collection of Strings here to define the Core Contract in this Identity Store Provisioner instance.
        descriptor.setAttributeContractSet(Collections.singleton(USERNAME));

        // Add a collection of Strings here to define the Core Group Contract in this Identity Store Provisioner instance.
        descriptor.setGroupAttributeContractSet(Collections.singleton(GROUPNAME));

        // Allow the contracts to be extended.
        descriptor.setSupportsExtendedContract(true);
        descriptor.setSupportsExtendedGroupContract(true);
    }

    @Override
    public void configure(Configuration configuration)
    {
        // Use the RadioGroupFieldDescriptor name to get the correct fieldValue.
        String fieldValue = configuration.getFieldValue(RADIO_BUTTON_NAME);

        // Register the user's selection from the Identity Store Admin UI so it can be used at runtime.
        permanentlyDeleteUser = DELETE_USER.equals(fieldValue);
    }

    @Override
    public PluginDescriptor getPluginDescriptor()
    {
        return descriptor;
    }

    @Override
    public IdentityStoreProvisionerDescriptor getIdentityStoreProvisionerDescriptor()
    {
        return descriptor;
    }

    @Override
    public UserResponseContextImpl createUser(CreateUserRequestContext createRequestCtx) throws IdentityStoreException
    {
        AttributeMap attributeMap = createRequestCtx.getUserAttributes();

        // verify we don't already have this user
        checkForConflict(attributeMap);

        // generate a random id so we can simulate a provisioning event
        String id = getRandomIntAsString();
        attributeMap.put(Constants.ID, id);

        // set the required timestamps
        AttributeValue now = AttrValueSupport.make(new Date());
        attributeMap.put(Constants.WHEN_CREATED, now);
        attributeMap.put(Constants.WHEN_CHANGED, now);

        // store the user in memory
        userCache.put(id, attributeMap);
        log.info("Created User: " + id);
        log.info("CreateUser Entity ID: " + createRequestCtx.getEntityId());
        log.info("Attributes:");
        for (Map.Entry<String, AttributeValue> e : attributeMap.entrySet())
        {
        	if (e.getValue().getValue() != null) 
        	{
        		log.info(String.format("%s => %s", e.getKey(), e.getValue().getValue()));
        	}
        	else  if (e.getValue().getObjectValue() != null) 
        	{
    			log.info(String.format("%s => (Object)", e.getKey()));
        		for (Object o : e.getValue().getAllObjectValues()) {
        			log.info(String.format("%s", o.toString()));
        		}
        	}
        }
        
        // Send back the response
        return new UserResponseContextImpl(attributeMap);
    }

    @Override
    public UsersResponseContextImpl readUsers(ReadUsersRequestContext readRequestCtx) throws IdentityStoreException
    {
    	log.info("SCIM Filter: " + readRequestCtx.getFilter());
    	log.info("Sort By: " + readRequestCtx.getSortBy());
    	log.info("Sort Order: " + readRequestCtx.getSortOrder());
    	log.info("Pagination Start Index: " + readRequestCtx.getStartIndex());
    	log.info("Pagination Count: " + readRequestCtx.getCount());

    	/*
    	 * readRequestCtx can optionally contain a filter. This filter specifies the search criteria for resources
    	 * to be returned. If the filter is empty, it is assumed that the request is for a list of all resources.
    	 */
    	
    	/*
    	 * Generate a list of all users
    	 */
        AttributeMap attributeMap = null;
    	List<AttributeMap> responseMap = new ArrayList<>();

        for (String id : userCache.keySet()) {
            if (permanentlyDeleteUser || isActive(id))
            {
                attributeMap = userCache.get(id);
    
                responseMap.add(attributeMap);
                
                // Print out some info to show the user attributes for testing.
                log.info("Read User: " + id);
                log.info("Entity ID: " + readRequestCtx.getEntityId());
                log.info("Attributes:");
                for (Map.Entry<String, AttributeValue> e : attributeMap.entrySet())
                {
                	if (e.getValue().getValue() != null) 
                	{
                		log.info(String.format("%s => %s", e.getKey(), e.getValue().getValue()));
                	}
                	else  if (e.getValue().getObjectValue() != null) 
                	{
                		log.info(String.format("%s => (Object) %s", e.getKey(), e.getValue().getObjectValue().toString()));
                	}
                }
                
            }
            else
            {
                // Since we're in "disable user on delete" mode and the user is inactive (disabled)
                // the SCIM spec says to return a 404 in this case as though the user doesn't exist.
        		log.info(String.format("%s: %s", id, USER_NOT_FOUND));
            }
        }        
    	
    	/*
    	 * Parse the filter, and filter the results accordingly
    	 * 
         * There are many ways to parse the SCIM filter, but the easiest way is to use 3rd party
         * tools. PingDirectory provides one solution for parsing the filter into an object structure
         * as follows. Add the PingDirectory SCIM dependency and uncomment the following code.
         */
        //		if (! StringUtils.isEmpty(readRequestCtx.getFilter())) {
        //    		FilterParser filterParser = new FilterParser(scimFilter, "urn:scim:schemas:core:1.0");
        //    		SCIMFilter scimFilter = filterParser.parse();
        //		}		
        
        // Sorting
        
        if (StringUtils.isNotEmpty(readRequestCtx.getSortBy()))
        {
            String sortByParam = readRequestCtx.getSortBy();
            String sortOrder = readRequestCtx.getSortOrder();
            
            // Get the target to source attribute mapping
            AttributeValue sortByAttrValue = readRequestCtx.getSCIMTargetToSourceAttributeMapping().get(sortByParam);
            String sortBy = "";
            if (sortByAttrValue != null)
            {
                sortBy = sortByAttrValue.getValue();
            }
            
            sortAttributeMaps(responseMap, sortBy, sortOrder);
        }
        
        // Pagination
        
        if (readRequestCtx.getStartIndex() > 0 && readRequestCtx.getCount() > 0)
        {
            int startIndex = readRequestCtx.getStartIndex();
            int count = readRequestCtx.getCount();
            
            List<AttributeMap> paginatedList = paginateList(responseMap, startIndex, count);
                       
            return new UsersResponseContextImpl(paginatedList);
        }
                
        return new UsersResponseContextImpl(responseMap);
    }

    private List<AttributeMap> paginateList(List<AttributeMap> responseMap, int startIndex, int count)
    {
        List<AttributeMap> paginatedList = new ArrayList<>();
        startIndex = startIndex < 0 ? 1 : startIndex;
        count = count < 1 ? 1 : count;
        
        if (startIndex > responseMap.size())
        {
            return new ArrayList<AttributeMap>();
        }
        
        ListIterator<AttributeMap> iter = responseMap.listIterator(startIndex - 1);
        
        int counter = 1;
        while (iter.hasNext() && counter <= count)
        {
            paginatedList.add(iter.next());
            counter++;
        }
        return paginatedList;
    }

    private void sortAttributeMaps(List<AttributeMap> responseMap, String sortBy, String sortOrder)
    {
        Comparator<AttributeMap> comparatorAscending = new Comparator<AttributeMap>() 
        {
            public int compare(AttributeMap c1, AttributeMap c2) 
            {
                String c1Value = c1.get(sortBy).getValue();
                String c2Value = c2.get(sortBy).getValue();
                return c1Value.compareToIgnoreCase(c2Value);
            }
        };
        
        Comparator<AttributeMap> comparatorDescending = new Comparator<AttributeMap>() 
        {
            public int compare(AttributeMap c1, AttributeMap c2) 
            {
                String c1Value = c1.get(sortBy).getValue();
                String c2Value = c2.get(sortBy).getValue();
                return c1Value.compareToIgnoreCase(c2Value) * -1;
            }
        };

        Comparator<AttributeMap> comparator = "descending".equals(sortOrder) ? comparatorDescending : comparatorAscending ;
        Collections.sort(responseMap, comparator);
    }
    
    @Override
    public UserResponseContextImpl readUser(ReadUserRequestContext readRequestCtx) throws IdentityStoreException
    {
        AttributeMap attributeMap = null;

        String id = readRequestCtx.getUserId();
        if (userCache.containsKey(id))
        {
            if (permanentlyDeleteUser || isActive(id))
            {
                attributeMap = userCache.get(id);

                // Print out some info to show the user attributes for testing.
                log.info("Read User: " + id);
                log.info("Entity ID: " + readRequestCtx.getEntityId());
                log.info("Attributes:");
                for (Map.Entry<String, AttributeValue> e : attributeMap.entrySet())
                {
                	if (e.getValue().getValue() != null) 
                	{
                		log.info(String.format("%s => %s", e.getKey(), e.getValue().getValue()));
                	}
                	else  if (e.getValue().getObjectValue() != null) 
                	{
                		log.info(String.format("%s => (Object) %s", e.getKey(), e.getValue().getObjectValue().toString()));
                	}
                }
            }
            else
            {
                // Since we're in "disable user on delete" mode and the user is inactive (disabled)
                // the SCIM spec says to return a 404 in this case as though the user doesn't exist.
                throw new NotFoundException(USER_NOT_FOUND);
            }
        }
        else
        {
            // couldn't find the user in memory
            throw new NotFoundException(USER_NOT_FOUND + ": " + id);
        }

        // Send back the response
        return new UserResponseContextImpl(attributeMap);
    }

    @Override
    public UserResponseContextImpl updateUser(UpdateUserRequestContext updateRequestCtx) throws IdentityStoreException
    {
        AttributeMap updatedAttributeMap = null;
        String id = updateRequestCtx.getUserId();
        if (userCache.containsKey(id))
        {
            if (permanentlyDeleteUser || isActive(id))
            {
                // Print out some info.
                log.info("Update User: " + id);
                log.info("Entity ID: " + updateRequestCtx.getEntityId());

                AttributeMap existingAttributeMap = userCache.get(id);
                AttributeMap attributeMap = updateRequestCtx.getUserAttributes();

                // set whenChanged to now
                AttributeValue now = AttrValueSupport.make(new Date());
                attributeMap.put(Constants.WHEN_CHANGED, now);

                // set whenCreated to the original value in the map
                attributeMap.put(Constants.WHEN_CREATED, existingAttributeMap.get(Constants.WHEN_CREATED));
                attributeMap.put(Constants.ID, id);

                // set the new one
                userCache.replace(id, attributeMap);
                updatedAttributeMap = attributeMap;
            }
            else
            {
                // Since we're in "disable user on delete" mode and the user is inactive (disabled)
                // the SCIM spec says to return a 404 in this case as though the user doesn't exist.
                throw new NotFoundException(USER_NOT_FOUND);
            }
        }
        else
        {
            // couldn't find the user in memory
            throw new NotFoundException(USER_NOT_FOUND + ": " + id);
        }

        // Send back the response
        return new UserResponseContextImpl(updatedAttributeMap);
    }

    @Override
    public void deleteUser(DeleteUserRequestContext deleteRequestCtx) throws IdentityStoreException
    {
        String id = deleteRequestCtx.getUserId();
        if (userCache.containsKey(id))
        {
            // Found an existing user - do we disable or delete?
            if (permanentlyDeleteUser)
            {
                userCache.remove(id);
                log.info("Deleted User: " + id);
                log.info("Entity ID: " + deleteRequestCtx.getEntityId());
            }
            else
            {
                if (isActive(id))
                {
                    // we're not in permanentlyDeleteUser mode and they're active so just disable them
                    AttributeMap attributeMap = userCache.get(id);
                    attributeMap.put(ACTIVE, new AttributeValue(Boolean.toString(false)));
                    log.info("Disabled User: " + id);
                    log.info("Entity ID: " + deleteRequestCtx.getEntityId());
                }
                else
                {
                    // Since we're in "disable user on delete" mode and the user is inactive (disabled)
                    // the SCIM spec says to return a 404 in this case as though the user doesn't exist.
                    throw new NotFoundException(USER_NOT_FOUND);
                }
            }
        }
        else
        {
            // couldn't find the user in memory
            throw new NotFoundException(USER_NOT_FOUND + ": " + id);
        }
    }

    @Override
    public GroupResponseContext readGroup(ReadGroupRequestContext readRequestCtx) throws IdentityStoreException
    {
        AttributeMap attributeMap = null;

        String id = readRequestCtx.getGroupId();
        if (groupCache.containsKey(id))
        {
            attributeMap = groupCache.get(id);

            // Print out some info to show the group attributes for testing.
            log.info("Read Group: " + id);
            log.info("Entity ID: " + readRequestCtx.getEntityId());
            log.info("Attributes:");
            for (Map.Entry<String, AttributeValue> e : attributeMap.entrySet())
            {
                log.info(String.format("%s => %s", e.getKey(), e.getValue().getValue()));
            }
        }
        else
        {
            // couldn't find the group in memory
            throw new NotFoundException(GROUP_NOT_FOUND + ": " + id);
        }

        // Send back the response
        return new GroupResponseContextImpl(attributeMap);
    }

    @Override
    public GroupsResponseContext readGroups(ReadGroupsRequestContext readRequestCtx) throws IdentityStoreException
    {
    	log.info("SCIM Filter: " + readRequestCtx.getFilter());
        log.info("Sort By: " + readRequestCtx.getSortBy());
        log.info("Sort Order: " + readRequestCtx.getSortOrder());
        log.info("Pagination Start Index: " + readRequestCtx.getStartIndex());
        log.info("Pagination Count: " + readRequestCtx.getCount());


    	/*
    	 * readRequestCtx can optionally contain a filter. This filter specifies the search criteria for resources
    	 * to be returned. If the filter is empty, it is assumed that the request is for a list of all resources.
    	 */
    	
    	/*
    	 * Generate a list of all groups
    	 */
        AttributeMap attributeMap = null;
    	List<AttributeMap> responseMap = new ArrayList<>();

        for (String id : groupCache.keySet()) {
            attributeMap = groupCache.get(id);

            responseMap.add(attributeMap);

            // Print out some info to show the group attributes for testing.
            log.info("Read Group: " + id);
            log.info("Entity ID: " + readRequestCtx.getEntityId());
            log.info("Attributes:");
            for (Map.Entry<String, AttributeValue> e : attributeMap.entrySet())
            {
                log.info(String.format("%s => %s", e.getKey(), e.getValue().getValue()));
            }
        }        
    	
    	/*
    	 * Parse the filter, and filter the results accordingly
    	 * 
         * There are many ways to parse the SCIM filter, but the easiest way is to use 3rd party
         * tools. PingDirectory provides one solution for parsing the filter into an object structure
         * as follows. 
         */
        //		if (! StringUtils.isEmpty(readRequestCtx.getFilter())) {
        //    		FilterParser filterParser = new FilterParser(scimFilter, coreSchema);
        //    		SCIMFilter scimFilter = filterParser.parse();
        //		}		
        
        // Sorting
        
        if (StringUtils.isNotEmpty(readRequestCtx.getSortBy()))
        {
            String sortByParam = readRequestCtx.getSortBy();
            String sortOrder = readRequestCtx.getSortOrder();
            
            // Get the target to source attribute mapping
            AttributeValue sortByAttrValue = readRequestCtx.getSCIMTargetToSourceAttributeMapping().get(sortByParam);
            String sortBy = "";
            if (sortByAttrValue != null)
            {
                sortBy = sortByAttrValue.getValue();
            }
            
            sortAttributeMaps(responseMap, sortBy, sortOrder);
        }        
        
        // Pagination
        
        if (readRequestCtx.getStartIndex() > 0 && readRequestCtx.getCount() > 0)
        {
            int startIndex = readRequestCtx.getStartIndex();
            int count = readRequestCtx.getCount();
            
            List<AttributeMap> paginatedList = paginateList(responseMap, startIndex, count);
                       
            return new GroupsResponseContextImpl(paginatedList);
        }
    	
        return new GroupsResponseContextImpl(responseMap);
    }

    
    @Override
    public void deleteGroup(DeleteGroupRequestContext deleteRequestCtx) throws IdentityStoreException
    {
        String id = deleteRequestCtx.getGroupId();
        if (groupCache.containsKey(id))
        {
            groupCache.remove(id);
            log.info("Deleted Group: " + id);
            log.info("Entity ID: " + deleteRequestCtx.getEntityId());
        }
        else
        {
            // couldn't find the group in memory
            throw new NotFoundException(GROUP_NOT_FOUND + ": " + id);
        }
    }

    @Override
    public GroupResponseContext updateGroup(UpdateGroupRequestContext updateRequestContext)
            throws IdentityStoreException
    {
        AttributeMap updatedAttributeMap = null;
        String id = updateRequestContext.getGroupId();
        if (groupCache.containsKey(id))
        {
            AttributeMap existingAttributeMap = groupCache.get(id);
            AttributeMap attributeMap = updateRequestContext.getGroupAttributes();
            String name = getGroupName(attributeMap);

            // Print out some info.
            log.info("Update Group: " + id);
            log.info("Group Name: " + name);
            log.info("Entity ID: " + updateRequestContext.getEntityId());


            // set whenChanged to now
            AttributeValue now = AttrValueSupport.make(new Date());
            attributeMap.put(Constants.WHEN_CHANGED, now);

            // set whenCreated to the original value in the map
            attributeMap.put(Constants.WHEN_CREATED, existingAttributeMap.get(Constants.WHEN_CREATED));
            attributeMap.put(Constants.ID, id);

            // set the new one
            groupCache.replace(id, attributeMap);
            updatedAttributeMap = attributeMap;
        }
        else
        {
            // couldn't find the group in memory
            throw new NotFoundException(GROUP_NOT_FOUND + ": " + id);
        }

        if (updateRequestContext.getGroupAttributes() != null)
        {
            AttributeValue membersAttribute = updateRequestContext.getGroupAttributes().get(IdentityStoreProvisionerDescriptor.DEFAULT_MEMBERS_ATTR_NAME);
            // handle members
            if (membersAttribute != null && membersAttribute.getAllObjectValues() != null)
            {
                for (Object o : membersAttribute.getAllObjectValues())
                {
                    MemberAttribute member = (MemberAttribute) o;
                    log.info("Update Group '" + id + "' with member: " + member.getId());
                }
            }
        }

        return new GroupResponseContextImpl(updatedAttributeMap);
    }

    @Override
    public GroupResponseContext createGroup(CreateGroupRequestContext createRequestContext)
            throws IdentityStoreException
    {
        AttributeMap attributeMap = createRequestContext.getGroupAttributes();
        String id = getRandomIntAsString();
        String name = getGroupName(attributeMap);

        attributeMap.put(Constants.ID, id);

        // set the required timestamps
        AttributeValue now = AttrValueSupport.make(new Date());
        attributeMap.put(Constants.WHEN_CREATED, now);
        attributeMap.put(Constants.WHEN_CHANGED, now);

        // store the group in memory
        groupCache.put(id, attributeMap);

        // print out some info
        log.info("Created Group: " + id);
        log.info("Group Name: " + name);
        log.info("Entity ID: " + createRequestContext.getEntityId());

        AttributeValue membersAttribute = createRequestContext.getGroupAttributes().get(IdentityStoreProvisionerDescriptor.DEFAULT_MEMBERS_ATTR_NAME);
        if (membersAttribute != null && membersAttribute.getAllObjectValues() != null)
        {
            // handle members
            if (membersAttribute != null)
            {
                for (Object o : membersAttribute.getAllObjectValues())
                {
                    MemberAttribute member = (MemberAttribute) o;
                    log.info("Create Group '" + id + "' with member: " + member.getId());
                }
            }
        }

        return new GroupResponseContextImpl(attributeMap);
    }

    private String getGroupName(AttributeMap attributeMap)
    {
        String name = null;
        AttributeValue nameAttr = attributeMap.get(GROUPNAME);
        if (nameAttr != null)
        {
            name = nameAttr.getValue();
        }

        return name;
    }

    @Override
    public boolean isGroupProvisioningSupported()
    {
        return true;
    }

    public boolean isPermanentlyDeleteUser()
    {
        return permanentlyDeleteUser;
    }

    public void setPermanentlyDeleteUser(boolean permanentlyDeleteUser)
    {
        this.permanentlyDeleteUser = permanentlyDeleteUser;
    }

    private void checkForConflict(AttributeMap attributeMap) throws ConflictException
    {
        // Retrieve the username from the attributes sent in the create request since it's a required attribute for
        // SCIM. We know if we got to this portion of the code then it's included.
        String newUserNameValue = attributeMap.getSingleValue(USERNAME);

        // loop through the existing Identity Store (in memory for this sample)
        for (AttributeMap existingAttributeMap : userCache.values())
        {
            if (existingAttributeMap.get(USERNAME) != null
            		&& newUserNameValue != null
                    && newUserNameValue.equals(existingAttributeMap.get(USERNAME).getValue()))
            {
                // The username matches an existing one in memory. If we're in "permanentlyDeleteUser mode"
                // or if the existing user is active then throw a ConflictException. However, if we're in
                // "disable mode" and the user is inactive then allow them to create another user with the
                // same username but a different id.
                if (permanentlyDeleteUser || "true".equalsIgnoreCase(existingAttributeMap.getSingleValue(ACTIVE)))
                {
                    // if we find a match and they're active then throw a ConflictException
                    throw new ConflictException("User already exists: " + newUserNameValue);
                }
            }
        }
    }

    private boolean isActive(String id)
    {
        // Determine whether the user is active.
        AttributeMap attributeMap = userCache.get(id);
        return "true".equalsIgnoreCase(attributeMap.getSingleValue(ACTIVE));
    }

    private static String getRandomIntAsString()
    {
        Random rand = new Random();
        int randomNumber = rand.nextInt(100000) + 1;
        return Integer.toString(randomNumber);
    }
}
