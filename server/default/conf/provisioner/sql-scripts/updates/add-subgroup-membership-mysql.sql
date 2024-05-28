ALTER TABLE `group_membership`
  MODIFY COLUMN `userDsGuid` varchar(255),
  ADD COLUMN `subGroupDsGuid` varchar(255),
  DROP INDEX `membershipunique`,
  ADD UNIQUE KEY `membershipunique` (`channel`,`groupDsGuid`,`userDsGuid`,`subGroupDsGuid`),
  ADD FOREIGN KEY `membershipsubgroupfk` (`channel`,`subGroupDsGuid`) REFERENCES `channel_group` (`channel`,`dsGuid`) ON DELETE CASCADE;
