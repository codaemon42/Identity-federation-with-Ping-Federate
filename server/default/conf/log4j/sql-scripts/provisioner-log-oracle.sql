# ------------------------------------------------------------
# Create table provisioner_log
# ------------------------------------------------------------

CREATE TABLE provisioner_log (
  id         INTEGER  PRIMARY KEY,
  dtime      TIMESTAMP,
  loglevel   VARCHAR2(8),
  classname  VARCHAR2(255),
  message    CLOB,
  channelcode VARCHAR2(255)
);

CREATE SEQUENCE provisioner_log_sequence
START WITH 1
INCREMENT BY 1;

CREATE OR REPLACE TRIGGER provisioner_log_trigger BEFORE INSERT ON provisioner_log REFERENCING NEW AS NEW FOR EACH ROW BEGIN SELECT provisioner_log_sequence.nextval INTO :NEW.ID FROM dual; END; 
. 
RUN