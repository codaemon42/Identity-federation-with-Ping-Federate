# ------------------------------------------------------------
# Create table audit_log
# ------------------------------------------------------------

CREATE TABLE audit_log (
  id        INTEGER  PRIMARY KEY,
  dtime     TIMESTAMP,
  event     VARCHAR2(255),
  username  VARCHAR2(255),
  ip        VARCHAR2(255),
  app       VARCHAR2(2048),
  host      VARCHAR2(255),
  protocol  VARCHAR2(255),
  role      VARCHAR2(255),
  partnerid VARCHAR2(255),
  status    VARCHAR2(255),
  adapterid VARCHAR2(255),
  description CLOB,
  responsetime INTEGER,
  trackingid VARCHAR2(255)
);

CREATE SEQUENCE audit_log_sequence
START WITH 1
INCREMENT BY 1;

CREATE OR REPLACE TRIGGER audit_log_trigger BEFORE INSERT ON audit_log REFERENCING NEW AS NEW FOR EACH ROW BEGIN SELECT audit_log_sequence.nextval INTO :NEW.ID FROM dual; END;
.
RUN
 
