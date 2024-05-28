CREATE TABLE pingfederate_oauth_clients(
    client_id 		NVARCHAR(256) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL,
    name 		NVARCHAR(128) NOT NULL,
    refresh_rolling 	SMALLINT,
    logo 		NVARCHAR(1024),
    hashed_secret 	NVARCHAR(64),
    description 	NVARCHAR(2048),
    persistent_grant_exp_time BIGINT,
    persistent_grant_exp_time_unit NVARCHAR(1),
    bypass_approval_page 	SMALLINT,
    CONSTRAINT pk_client_id PRIMARY KEY NONCLUSTERED (client_id)
);

CREATE TABLE pingfederate_oauth_clients_ext(
	param_id uniqueidentifier NOT NULL DEFAULT newsequentialid(),
    client_id NVARCHAR(256) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL,
    name NVARCHAR(128) NOT NULL,
    value NVARCHAR(1024),
    CONSTRAINT fk_client_id
        FOREIGN KEY (client_id)
            REFERENCES pingfederate_oauth_clients(client_id)
            ON DELETE CASCADE,
    CONSTRAINT pk_param_id PRIMARY KEY NONCLUSTERED (param_id)
);

CREATE INDEX IDX_CLIENT_ID ON pingfederate_oauth_clients_ext(client_id);
CREATE INDEX IDX_FIELD_NAME ON pingfederate_oauth_clients_ext(name) INCLUDE (value);
