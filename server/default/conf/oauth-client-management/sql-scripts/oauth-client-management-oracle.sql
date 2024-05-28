CREATE TABLE pingfederate_oauth_clients(
    client_id 		VARCHAR2(256) NOT NULL,
    name 		VARCHAR2(128) NOT NULL,
    refresh_rolling 	NUMBER,
    logo 		VARCHAR2(1024),
    hashed_secret 	VARCHAR2(64),
    description 	VARCHAR2(2048),
    persistent_grant_exp_time NUMBER,
    persistent_grant_exp_time_unit VARCHAR2(1),
    bypass_approval_page	NUMBER,
    PRIMARY KEY(client_id),
    CHECK (refresh_rolling in(0,1))
);

CREATE TABLE pingfederate_oauth_clients_ext(
    client_id VARCHAR2(256) NOT NULL, 
    name VARCHAR2(128) NOT NULL, 
    value VARCHAR2(1024),
    CONSTRAINT fk_client_id
        FOREIGN KEY (client_id)
            REFERENCES pingfederate_oauth_clients(client_id)
            ON DELETE CASCADE
);

CREATE INDEX IDX_CLIENT_ID ON pingfederate_oauth_clients_ext(client_id);
CREATE INDEX IDX_FIELD_NAME ON pingfederate_oauth_clients_ext(name, value);

