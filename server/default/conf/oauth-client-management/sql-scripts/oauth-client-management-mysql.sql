CREATE TABLE pingfederate_oauth_clients(
    client_id                       VARCHAR(256) COLLATE latin1_general_cs NOT NULL,
    name                            VARCHAR(128) NOT NULL,
    refresh_rolling                 SMALLINT,
    logo                            VARCHAR(1024),
    hashed_secret                   VARCHAR(64),
    description                     VARCHAR(2048),
    persistent_grant_exp_time       BIGINT,
    persistent_grant_exp_time_unit  VARCHAR(1),
    bypass_approval_page            SMALLINT,
    PRIMARY KEY(`client_id`)
) DEFAULT CHARSET=latin1;

CREATE TABLE pingfederate_oauth_clients_ext(
    client_id       VARCHAR(256) COLLATE latin1_general_cs NOT NULL,
    name            VARCHAR(128) NOT NULL,
    value           VARCHAR(1024),
    FOREIGN KEY (`client_id`)
        REFERENCES pingfederate_oauth_clients(`client_id`)
        ON DELETE CASCADE
) DEFAULT CHARSET=latin1;

CREATE INDEX IDX_CLIENT_ID ON pingfederate_oauth_clients_ext(client_id);
CREATE INDEX IDX_FIELD_NAME ON pingfederate_oauth_clients_ext(name, value);

