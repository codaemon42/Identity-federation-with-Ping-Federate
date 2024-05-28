-- Drop existing tables
-- ------------------------------------------------------------

IF OBJECT_ID('group_membership') IS NOT NULL DROP TABLE group_membership;
IF OBJECT_ID('channel_group') IS NOT NULL DROP TABLE channel_group;
IF OBJECT_ID('channel_user') IS NOT NULL DROP TABLE channel_user;
IF OBJECT_ID('channel_variable') IS NOT NULL DROP TABLE channel_variable;
IF OBJECT_ID('node_state') IS NOT NULL DROP TABLE node_state;

-- Table structure for table channel_user
-- ------------------------------------------------------------

CREATE TABLE channel_user (
   channel int NOT NULL default '0',
   dsGuid varchar(255) NOT NULL default ' ',
   saasGuid varchar(255) default NULL,
   saasUsername varchar(255) default NULL,
   valuesHash varchar(32) default NULL,
   inGroup int NOT NULL default '0',
   dirty int NOT NULL default '1',
   saasIdentity text,
   created datetime NOT NULL default (getdate()),
   modified datetime NOT NULL default '0000-00-00 00:00:00',
   PRIMARY KEY (channel,dsGuid),
   CONSTRAINT saasUsername UNIQUE (channel,saasUsername)
);

-- Table structure for table channel_variable
-- ------------------------------------------------------------

CREATE TABLE channel_variable (
   channel int NOT NULL default '0',
   name varchar(255) NOT NULL default '',
   value varchar(255) default NULL,
   PRIMARY KEY (channel,name)
);

-- Table structure for table node_state
-- ------------------------------------------------------------

CREATE TABLE node_state (
   nodeid int,
   role varchar(40) NOT NULL default 'backup',
   heartbeat datetime NOT NULL default '0000-00-00 00:00:00',
   channel int NOT NULL default '0',
   PRIMARY KEY (nodeid)
);

-- Table structure for table channel_group
-- ------------------------------------------------------------

CREATE TABLE channel_group (
   channel int NOT NULL default '0',
   dsGuid VARCHAR(255) NOT NULL,
   saasGuid varchar(255) default NULL,
   saasGroupName varchar(1200) default NULL,
   valuesHash varchar(32) default NULL,
   inGroup int NOT NULL default '0',
   dirty int NOT NULL default '1',
   saasGroup text,
   created datetime NOT NULL default (getdate()),
   modified datetime NOT NULL default '0000-00-00 00:00:00',
   PRIMARY KEY (channel,dsGuid),
   CONSTRAINT saasGroupName UNIQUE (channel,saasGroupName)
);

-- Table structure for table group_membership
-- ------------------------------------------------------------

CREATE TABLE group_membership (
   channel int NOT NULL default '0',
   groupDsGuid varchar(255) NOT NULL,
   userDsGuid varchar(255),
   subGroupDsGuid varchar(255),
   CONSTRAINT membershipunique UNIQUE (channel,groupDsGuid,userDsGuid,subGroupDsGuid),
   CONSTRAINT membershipgroupfk FOREIGN KEY (channel,groupDsGuid) REFERENCES channel_group (channel,dsGuid) ON DELETE CASCADE,
   CONSTRAINT membershipuserfk FOREIGN KEY (channel,userDsGuid) REFERENCES channel_user (channel,dsGuid) ON DELETE CASCADE
);
CREATE NONCLUSTERED INDEX membershipgroupindex ON group_membership (channel,groupDsGuid);
CREATE NONCLUSTERED INDEX membershipuserindex ON group_membership (channel,userDsGuid);
CREATE NONCLUSTERED INDEX membershipsubgroupindex ON group_membership (channel,subGroupDsGuid);
