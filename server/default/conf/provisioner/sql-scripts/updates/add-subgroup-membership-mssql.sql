ALTER TABLE group_membership ALTER COLUMN userDsGuid varchar(255);
ALTER TABLE group_membership ADD subGroupDsGuid varchar(255);
ALTER TABLE group_membership DROP CONSTRAINT membershipunique;
ALTER TABLE group_membership ADD CONSTRAINT membershipunique UNIQUE (channel,groupDsGuid,userDsGuid,subGroupDsGuid);
CREATE NONCLUSTERED INDEX membershipsubgroupindex ON group_membership (channel,subGroupDsGuid);
