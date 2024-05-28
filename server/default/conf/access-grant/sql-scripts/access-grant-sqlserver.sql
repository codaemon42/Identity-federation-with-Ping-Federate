/*------------------------------------------------------------
 * Create table pingfederate_access_grant
 */-----------------------------------------------------------

CREATE TABLE pingfederate_access_grant(
    guid                 NVARCHAR(32) NOT NULL,
    hashed_refresh_token NVARCHAR(256),
    unique_user_id       NVARCHAR(256) NOT NULL,
    scope                NVARCHAR(1024),
    client_id            NVARCHAR(256) NOT NULL,
    grant_type           NVARCHAR(128),
    context_qualifier    NVARCHAR(64),  
    issued               DATETIME NOT NULL,
    updated              DATETIME NOT NULL,
    expires              DATETIME NULL,
    CONSTRAINT pk_guid PRIMARY KEY NONCLUSTERED (guid)
);
CREATE INDEX UNIQUEUSERIDIDX ON pingfederate_access_grant(unique_user_id);
CREATE INDEX HASHEDREFRESHTOKENIDX ON pingfederate_access_grant(hashed_refresh_token);
CREATE INDEX CLIENTIDIDX ON pingfederate_access_grant(client_id);
CREATE INDEX EXPIRESIDX ON pingfederate_access_grant(expires);
CREATE INDEX UNIQUEUSERIDCLIENTIDGRANTTYPEIDX ON pingfederate_access_grant(unique_user_id, client_id, grant_type);

GO
