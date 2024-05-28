# Drop existing tables
# ------------------------------------------------------------

DROP TABLE IF EXISTS `group_membership`;
DROP TABLE IF EXISTS `channel_group`;
DROP TABLE IF EXISTS `channel_user`;
DROP TABLE IF EXISTS `channel_variable`;
DROP TABLE IF EXISTS `node_state`;


# Table structure for table channel_user
# ------------------------------------------------------------

CREATE TABLE `channel_user` (
  `channel` int(11) unsigned NOT NULL default '0',
  `dsGuid` varchar(255) NOT NULL default ' ',
  `saasGuid` varchar(255) default NULL,
  `saasUsername` varchar(255) default NULL,
  `valuesHash` varchar(32) default NULL,
  `inGroup` tinyint(1) unsigned NOT NULL default '0',
  `dirty` tinyint(1) unsigned NOT NULL default '1',
  `saasIdentity` text,
  `created` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `modified` timestamp NOT NULL default '1970-01-01 00:00:01',
  PRIMARY KEY  (`channel`,`dsGuid`),
  UNIQUE KEY `saasUsername` (`channel`,`saasUsername`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Table structure for table channel_variable
# ------------------------------------------------------------

CREATE TABLE `channel_variable` (
  `channel` int(11) unsigned NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  `value` varchar(255) default NULL,
  PRIMARY KEY  (`channel`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Table structure for table node_state
# ------------------------------------------------------------

CREATE TABLE node_state (
  nodeid    int,
  role      varchar(40) NOT NULL default 'backup',
  heartbeat timestamp NOT NULL default '1970-01-01 00:00:01',
  PRIMARY KEY  (nodeid)
);


# Table structure for table channel_group
# ------------------------------------------------------------

CREATE TABLE `channel_group` (
  `channel` int(11) unsigned NOT NULL default '0',
  `dsGuid` varchar(255) NOT NULL default ' ',
  `saasGuid` varchar(255) default NULL,
  `saasGroupName` varchar(4000) default NULL,
  `valuesHash` varchar(32) default NULL,
  `inGroup` tinyint(1) unsigned NOT NULL default '0',
  `dirty` tinyint(1) unsigned NOT NULL default '1',
  `saasGroup` text,
  `created` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `modified` timestamp NOT NULL default '1970-01-01 00:00:01',
  PRIMARY KEY  (`channel`,`dsGuid`),
  UNIQUE KEY `saasGroupName` (`channel`,saasGroupName(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# Table structure for table group_membership
# ------------------------------------------------------------

CREATE TABLE `group_membership` (
  `channel` int(11) unsigned NOT NULL default '0',
  `groupDsGuid` varchar(255) NOT NULL,
  `userDsGuid` varchar(255),
  `subGroupDsGuid` varchar(255),
  UNIQUE KEY `membershipunique` (`channel`,`groupDsGuid`,`userDsGuid`,`subGroupDsGuid`),
  FOREIGN KEY `membershipgroupfk` (`channel`,`groupDsGuid`) REFERENCES `channel_group` (`channel`,`dsGuid`) ON DELETE CASCADE,
  FOREIGN KEY `membershipuserfk` (`channel`,`userDsGuid`) REFERENCES `channel_user` (`channel`,`dsGuid`) ON DELETE CASCADE,
  FOREIGN KEY `membershipsubgroupfk` (`channel`,`subGroupDsGuid`) REFERENCES `channel_group` (`channel`,`dsGuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
