# ------------------------------------------------------------
# Create table pingfederate_access_grant_attr
# ------------------------------------------------------------

CREATE TABLE pingfederate_access_grant_attr(
    grant_guid           VARCHAR(32) NOT NULL,
    source_type          SMALLINT,
    name                 VARCHAR(256) NOT NULL,
    value                VARCHAR(2048),
    masked               SMALLINT,
    encrypted            SMALLINT,
    FOREIGN KEY (`grant_guid`)    
        REFERENCES pingfederate_access_grant(`guid`)
        ON DELETE CASCADE   
);

CREATE INDEX IDX_GRANT_GUID ON pingfederate_access_grant_attr(grant_guid);
