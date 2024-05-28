CREATE TABLE pingfederate_session_groups(
    id                 VARCHAR2(32) NOT NULL, 
    hashed_session_id  VARCHAR2(64) NOT NULL, 
    expiry_time        TIMESTAMP NOT NULL,
    last_activity_time TIMESTAMP NOT NULL, 
    parent_group_id    VARCHAR2(32), 
    metadata           VARCHAR2(4000), 
    PRIMARY KEY (id) 
);
CREATE INDEX IDX_PF_SG_SESSION_ID ON pingfederate_session_groups(hashed_session_id);
CREATE INDEX IDX_PF_SG_EXPIRY_TIME ON pingfederate_session_groups(expiry_time);
CREATE INDEX IDX_PF_SG_LAST_ACTIVITY_TIME ON pingfederate_session_groups(last_activity_time);

CREATE TABLE pingfederate_session_data(
    session_group_id   VARCHAR2(32) NOT NULL, 
    attribute_hash     VARCHAR2(64) NOT NULL, 
    row_index          NUMBER, 
    session_data_1     VARCHAR2(3500), 
    session_data_2     VARCHAR2(3500), 
    PRIMARY KEY (session_group_id, attribute_hash, row_index), 
    CONSTRAINT fk_pf_sd_session_group_id FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);

CREATE TABLE pingfederate_session_user_ids(
    session_group_id   VARCHAR2(32) NOT NULL, 
    unique_user_id     VARCHAR2(256) NOT NULL,
    PRIMARY KEY (session_group_id, unique_user_id), 
    CONSTRAINT fk_pf_sui_session_group_id FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);
CREATE INDEX IDX_PF_SUI_UNIQUE_USER_ID ON pingfederate_session_user_ids(unique_user_id);

