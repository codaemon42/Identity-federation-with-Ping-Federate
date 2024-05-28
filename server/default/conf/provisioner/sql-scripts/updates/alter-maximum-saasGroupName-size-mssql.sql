-- The maximum key length for a nonclustered index is 1700 bytes. A previous update had set
-- this value to 4000 which exceeded this maximum key length.
ALTER TABLE channel_group ALTER COLUMN saasGroupName varchar(1200);
