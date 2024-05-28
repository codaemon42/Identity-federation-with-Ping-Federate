CREATE TABLE pingfederate_session_groups(
    id                 NVARCHAR(32) NOT NULL, 
    hashed_session_id  NVARCHAR(64) NOT NULL, 
    expiry_time        DATETIME NOT NULL,
    last_activity_time DATETIME NOT NULL, 
    parent_group_id    NVARCHAR(32), 
    metadata           NVARCHAR(4000), 
    CONSTRAINT pk_pf_sg_id PRIMARY KEY NONCLUSTERED (id) 
);
CREATE INDEX IDX_PF_SG_SESSION_ID ON pingfederate_session_groups(hashed_session_id);
CREATE INDEX IDX_PF_SG_EXPIRY_TIME ON pingfederate_session_groups(expiry_time);
CREATE INDEX IDX_PF_SG_LAST_ACTIVITY_TIME ON pingfederate_session_groups(last_activity_time);

CREATE TABLE pingfederate_session_data(
    session_group_id   NVARCHAR(32) NOT NULL, 
    attribute_hash     NVARCHAR(64) NOT NULL, 
    row_index          SMALLINT, 
    session_data_1     NVARCHAR(3500), 
    session_data_2     NVARCHAR(3500), 
    CONSTRAINT pk_pf_sd_session_group_id PRIMARY KEY NONCLUSTERED (session_group_id, attribute_hash, row_index), 
    CONSTRAINT fk_pf_sd_session_group_id FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);

CREATE TABLE pingfederate_session_user_ids(
    session_group_id   NVARCHAR(32) NOT NULL, 
    unique_user_id     NVARCHAR(256) NOT NULL,
    CONSTRAINT pk_pf_sui_session_group_id PRIMARY KEY NONCLUSTERED (session_group_id, unique_user_id), 
    CONSTRAINT fk_pf_sui_session_group_id FOREIGN KEY (session_group_id) REFERENCES pingfederate_session_groups(id) ON DELETE CASCADE
);
CREATE INDEX IDX_PF_SUI_UNIQUE_USER_ID ON pingfederate_session_user_ids(unique_user_id);

