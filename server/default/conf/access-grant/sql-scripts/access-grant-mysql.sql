
# ------------------------------------------------------------
# Create table pingfederate_access_grant
# ------------------------------------------------------------

CREATE TABLE pingfederate_access_grant(
    guid                 VARCHAR(32) NOT NULL,
    hashed_refresh_token VARCHAR(256),
    unique_user_id       VARCHAR(256) NOT NULL,
    scope                VARCHAR(1024),
    client_id            VARCHAR(256) NOT NULL,
    grant_type           VARCHAR(128),
    context_qualifier    VARCHAR(64),
    issued               TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    updated              TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    expires              TIMESTAMP NULL,
    PRIMARY KEY (guid),
    KEY `UNIQUEUSERIDIDX` (`unique_user_id`(128)),
    KEY `HASHEDREFRESHTOKENIDX` (`hashed_refresh_token`(128)),
    KEY `CLIENTIDIDX` (`client_id`(128)),
    KEY `EXPIRESIDX` (`expires`)
    );
CREATE INDEX UNIQUEUSERIDCLIENTIDGRANTTYPEIDX ON pingfederate_access_grant(unique_user_id, client_id, grant_type);