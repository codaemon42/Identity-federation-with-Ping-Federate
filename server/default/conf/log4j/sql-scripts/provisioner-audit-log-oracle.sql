# ------------------------------------------------------------
# Create table provisioner_audit_log
# ------------------------------------------------------------

CREATE TABLE provisioner_audit_log (
  id              INTEGER  PRIMARY KEY,
  dtime           TIMESTAMP,
  cycle_id        VARCHAR2(255),
  channel_id      VARCHAR2(255),
  event_type      VARCHAR2(25),
  source_id       VARCHAR2(255),
  target_id       VARCHAR2(255),
  is_success       VARCHAR2(8),
  failure_cause   CLOB
);

CREATE SEQUENCE provisioner_audit_log_sequence
START WITH 1
INCREMENT BY 1;

CREATE OR REPLACE TRIGGER provisioner_audit_log_trigger BEFORE INSERT ON provisioner_audit_log REFERENCING NEW AS NEW FOR EACH ROW BEGIN SELECT provisioner_audit_log_sequence.nextval INTO :NEW.ID FROM dual; END;
.
RUN