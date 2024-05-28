/* ------------------------------------------------------------
 * Create table provisioner_audit_log
*/ ------------------------------------------------------------

CREATE TABLE provisioner_audit_log (
  id            SERIAL PRIMARY KEY,
  dtime         TIMESTAMP WITHOUT TIME ZONE,
  cycle_id      VARCHAR(255),
  channel_id    VARCHAR(255),
  event_type    VARCHAR(25),
  source_id     VARCHAR(255),
  target_id     VARCHAR(255),
  is_success    VARCHAR(8),
  failure_cause TEXT
);