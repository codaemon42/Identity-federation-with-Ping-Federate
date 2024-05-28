-- Drop existing tables
--------------------------------------------------------------

DROP TABLE IF EXISTS group_membership;
DROP TABLE IF EXISTS channel_group;
DROP TABLE IF EXISTS channel_user;
DROP TABLE IF EXISTS channel_variable;
DROP TABLE IF EXISTS node_state;


-- Table structure for table channel_variable
--------------------------------------------------------------

CREATE TABLE channel_variable (
   channel INTEGER NOT NULL,
   name VARCHAR(255) NOT NULL,
   value VARCHAR(255) DEFAULT NULL,
   PRIMARY KEY (channel, name)
);


-- Table structure for table channel_user
--------------------------------------------------------------

CREATE TABLE channel_user (
   channel INT NOT NULL,
   dsGuid VARCHAR(255) NOT NULL,
   saasGuid VARCHAR(255) DEFAULT NULL,
   saasUsername VARCHAR(255) DEFAULT NULL,
   valuesHash VARCHAR(32) DEFAULT NULL,
   inGroup INT DEFAULT '0',
   dirty INT DEFAULT '1',
   saasIdentity VARCHAR(4000),
   created TIMESTAMP NOT NULL,
   modified TIMESTAMP NOT NULL,
   PRIMARY KEY (channel, dsGuid),
   CONSTRAINT saasUsernameIndex UNIQUE (channel, saasUsername)
);


-- Table structure for table node_state
--------------------------------------------------------------

CREATE TABLE node_state (
   nodeId INTEGER NOT NULL,
   role VARCHAR(40) NOT NULL,
   heartbeat TIMESTAMP NOT NULL,
   PRIMARY KEY (nodeId)
);


-- Table structure for table channel_group
--------------------------------------------------------------

CREATE TABLE channel_group (
   channel INT NOT NULL,
   dsGuid VARCHAR(255) NOT NULL,
   saasGuid VARCHAR(255) DEFAULT NULL,
   saasGroupName VARCHAR(4000) DEFAULT NULL,
   valuesHash VARCHAR(32) DEFAULT NULL,
   inGroup INT DEFAULT '0',
   dirty INT DEFAULT '1',
   saasGroup VARCHAR(4000),
   created TIMESTAMP NOT NULL,
   modified TIMESTAMP NOT NULL,
   PRIMARY KEY (channel, dsGuid),
   CONSTRAINT saasGroupNameIndex UNIQUE (channel, saasGroupName)
);

-- Table structure for table group_membership
--------------------------------------------------------------

CREATE TABLE group_membership (
   channel INT NOT NULL,
   groupDsGuid VARCHAR(255) NOT NULL,
   userDsGuid VARCHAR(255),
   subGroupDsGuid VARCHAR(255),
   CONSTRAINT membershipunique UNIQUE (channel, groupDsGuid, userDsGuid, subGroupDsGuid),
   CONSTRAINT membershipgroupfk FOREIGN KEY (channel, groupDsGuid) REFERENCES channel_group (channel, dsGuid) ON DELETE CASCADE,
   CONSTRAINT membershipuserfk FOREIGN KEY (channel, userDsGuid) REFERENCES channel_user (channel, dsGuid) ON DELETE CASCADE,
   CONSTRAINT membershipsubgroupfk FOREIGN KEY (channel,subGroupDsGuid) REFERENCES channel_group (channel,dsGuid) ON DELETE CASCADE
);

CREATE INDEX membershipgroupindex ON group_membership (channel, groupDsGuid);
CREATE INDEX membershipuserindex ON group_membership (channel, userDsGuid);
CREATE INDEX membershipsubgroupindex ON group_membership (channel, subGroupDsGuid);
