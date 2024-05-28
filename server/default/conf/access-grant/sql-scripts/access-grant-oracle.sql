
# ------------------------------------------------------------
# Create table pingfederate_access_grant
# ------------------------------------------------------------

CREATE TABLE pingfederate_access_grant(
    guid                 VARCHAR2(32) NOT NULL,
    hashed_refresh_token VARCHAR2(256),
    unique_user_id       VARCHAR2(256) NOT NULL,
    scope                VARCHAR2(1024),
    client_id            VARCHAR2(256) NOT NULL,
    grant_type           VARCHAR2(128),
    context_qualifier    VARCHAR2(64),    
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
