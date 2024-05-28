ALTER TABLE group_membership MODIFY userDsGuid VARCHAR2(255) NULL;
ALTER TABLE group_membership ADD subGroupDsGuid VARCHAR2(255);
ALTER TABLE group_membership DROP CONSTRAINT membershipunique;
ALTER TABLE group_membership ADD CONSTRAINT membershipunique UNIQUE (channel, groupDsGuid, userDsGuid, subGroupDsGuid);
ALTER TABLE group_membership ADD CONSTRAINT membershipsubgroupfk FOREIGN KEY (channel,subGroupDsGuid) REFERENCES channel_group (channel,dsGuid) ON DELETE CASCADE;
CREATE INDEX membershipsubgroupindex ON group_membership (channel, subGroupDsGuid);
