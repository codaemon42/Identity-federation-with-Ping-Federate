CREATE TABLE pingfederate_session_groups(
    id                 VARCHAR(32) NOT NULL, 
    hashed_session_id  VARCHAR(64) NOT NULL, 
    expiry_time        TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    last_activity_time TIMESTAMP NOT NULL default CURRENT_TIMESTAMP, 
    parent_group_id    VARCHAR(32), 
    metadata           VARCHAR(4000), 
    PRIMARY KEY (id) 
);
CREATE INDEX IDX_PF_SG_SESSION_ID ON pingfederate_session_groups(hashed_session_id);
CREATE INDEX IDX_PF_SG_EXPIRY_TIME ON pingfederate_session_groups(expiry_time);
CREATE INDEX IDX_PF_SG_LAST_ACTIVITY_TIME ON pingfederate_session_groups(last_activity_time);

CREATE TABLE pingfederate_session_data(
    session_group_id   VARCHAR(32) NOT NULL, 
    attribute_hash     VARCHAR(64) NOT NULL, 
    row_index          SMALLINT, 
    session_data_1     VARCHAR(3500), 
    session_data_2     VARCHAR(3500), 
    PRIMARY KEY (session_group_id, attribute_hash, row_index), 
    FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);

CREATE TABLE pingfederate_session_user_ids(
    session_group_id   VARCHAR(32) NOT NULL, 
    unique_user_id     VARCHAR(256) NOT NULL,
    PRIMARY KEY (session_group_id, unique_user_id), 
    FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);
CREATE INDEX IDX_PF_SUI_UNIQUE_USER_ID ON pingfederate_session_user_ids(unique_user_id);
 
