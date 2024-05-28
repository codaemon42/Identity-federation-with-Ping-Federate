-- Drop existing tables.
-- Note: if these tables do not exist. To avoid errors please skip the DROP TABLE commands in this section.
--------------------------------------------------------------

DROP TABLE group_membership;
DROP TABLE channel_group;
DROP TABLE channel_user;
DROP TABLE channel_variable;
DROP TABLE node_state;


-- Table structure for table channel_variable
--------------------------------------------------------------

CREATE TABLE channel_variable (
  channel INTEGER NOT NULL,
  name VARCHAR2(255) NOT NULL,
  value VARCHAR2(255) DEFAULT NULL,
  PRIMARY KEY (channel, name)
);


-- Table structure for table channel_user
--------------------------------------------------------------

CREATE TABLE channel_user (
  channel INT NOT NULL,
  dsGuid VARCHAR2(255) NOT NULL,
  saasGuid VARCHAR2(255) DEFAULT NULL,
  saasUsername VARCHAR2(255) DEFAULT NULL,
  valuesHash VARCHAR2(32) DEFAULT NULL,
  inGroup INT DEFAULT '0',
  dirty INT DEFAULT '1',
  saasIdentity VARCHAR2(4000),
  created TIMESTAMP NOT NULL,
  modified TIMESTAMP NOT NULL,
  PRIMARY KEY (channel, dsGuid),
  CONSTRAINT saasUsernameIndex UNIQUE (channel, saasUsername)
);


-- Table structure for table node_state
--------------------------------------------------------------

CREATE TABLE node_state (
  nodeId INTEGER NOT NULL,
  role VARCHAR2(40) NOT NULL,
  heartbeat TIMESTAMP NOT NULL,
  PRIMARY KEY (nodeId)
);


-- Table structure for table channel_group
--------------------------------------------------------------

CREATE TABLE channel_group (
  channel INT NOT NULL,
  dsGuid VARCHAR2(255) NOT NULL,
  saasGuid VARCHAR2(255) DEFAULT NULL,
  saasGroupName VARCHAR2(4000) DEFAULT NULL,
  valuesHash VARCHAR2(32) DEFAULT NULL,
  inGroup INT DEFAULT '0',
  dirty INT DEFAULT '1',
  saasGroup VARCHAR2(4000),
  created TIMESTAMP NOT NULL,
  modified TIMESTAMP NOT NULL,
  PRIMARY KEY (channel, dsGuid),
  CONSTRAINT saasGroupNameIndex UNIQUE (channel, saasGroupName)
);

-- Table structure for table group_membership
--------------------------------------------------------------

CREATE TABLE group_membership (
  channel INT NOT NULL,
  groupDsGuid VARCHAR2(255) NOT NULL,
  userDsGuid VARCHAR2(255),
  subGroupDsGuid VARCHAR2(255),
  CONSTRAINT membershipunique UNIQUE (channel, groupDsGuid, userDsGuid, subGroupDsGuid),
  CONSTRAINT membershipgroupfk FOREIGN KEY (channel, groupDsGuid) REFERENCES channel_group (channel, dsGuid) ON DELETE CASCADE,
  CONSTRAINT membershipuserfk FOREIGN KEY (channel, userDsGuid) REFERENCES channel_user (channel, dsGuid) ON DELETE CASCADE,
  CONSTRAINT membershipsubgroupfk FOREIGN KEY (channel,subGroupDsGuid) REFERENCES channel_group (channel,dsGuid) ON DELETE CASCADE
);
CREATE INDEX membershipgroupindex ON group_membership (channel, groupDsGuid);
CREATE INDEX membershipuserindex ON group_membership (channel, userDsGuid);
CREATE INDEX membershipsubgroupindex ON group_membership (channel, subGroupDsGuid);
