/*------------------------------------------------------------
 * Create table pingfederate_access_grant_attr
 */-----------------------------------------------------------

CREATE TABLE pingfederate_access_grant_attr(
    attr_id uniqueidentifier NOT NULL DEFAULT newsequentialid(),
    grant_guid           NVARCHAR(32) NOT NULL,
    source_type          SMALLINT,
    name                 NVARCHAR(256) NOT NULL,
    value                NVARCHAR(2048),
    masked               SMALLINT,
    encrypted            SMALLINT,
    CONSTRAINT fk_grant_guid
        FOREIGN KEY (grant_guid)
            REFERENCES pingfederate_access_grant(guid)
            ON DELETE CASCADE,
    CONSTRAINT pk_attr_id PRIMARY KEY NONCLUSTERED (attr_id)
);

CREATE INDEX IDX_GRANT_GUID ON pingfederate_access_grant_attr(grant_guid);

GO

