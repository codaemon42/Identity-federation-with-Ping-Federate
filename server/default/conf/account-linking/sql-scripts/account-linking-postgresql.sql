/* ------------------------------------------------------------
 * Create table pingfederate_account_link
 */------------------------------------------------------------

CREATE TABLE pingfederate_account_link (
  idp_entityid    VARCHAR(256),
  external_userid VARCHAR(256),
  adapter_id      VARCHAR(32),
  local_userid    VARCHAR(256),
  date_created    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  PRIMARY KEY (idp_entityid, external_userId, adapter_id)
);
