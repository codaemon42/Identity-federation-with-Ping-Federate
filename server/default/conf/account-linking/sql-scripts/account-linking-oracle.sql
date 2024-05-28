
# ------------------------------------------------------------
# Create table pingfederate_account_link
# ------------------------------------------------------------

CREATE TABLE pingfederate_account_link(
    idp_entityid    VARCHAR2(256),
    external_userid VARCHAR2(256),
    adapter_id      VARCHAR2(32),
    local_userid    VARCHAR2(256),
    date_created    TIMESTAMP NOT NULL,
    PRIMARY KEY (idp_entityid, external_userId, adapter_id)
    );
