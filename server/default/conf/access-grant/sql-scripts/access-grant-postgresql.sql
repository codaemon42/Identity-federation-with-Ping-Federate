/* ------------------------------------------------------------
 * Create table pingfederate_access_grant
 */------------------------------------------------------------

CREATE TABLE pingfederate_access_grant(
    guid                 VARCHAR(32) NOT NULL,
    hashed_refresh_token VARCHAR(256),
    unique_user_id       VARCHAR(256) NOT NULL,
    scope                VARCHAR(1024),
    client_id            VARCHAR(256) NOT NULL,
    grant_type           VARCHAR(128),
    context_qualifier    VARCHAR(64),
    issued               TIMESTAMP NOT NULL,
    updated              TIMESTAMP NOT NULL,
    expires              TIMESTAMP NULL,
    PRIMARY KEY (guid)
    );
CREATE INDEX UNIQUEUSERIDIDX ON pingfederate_access_grant(unique_user_id);
CREATE INDEX HASHEDREFRESHTOKENIDX ON pingfederate_access_grant(hashed_refresh_token);
CREATE INDEX CLIENTIDIDX ON pingfederate_access_grant(client_id);
CREATE INDEX EXPIRESIDX ON pingfederate_access_grant(expires);
CREATE INDEX UNIQUEUSERIDCLIENTIDGRANTTYPEIDX ON pingfederate_access_grant(unique_user_id, client_id, grant_type);