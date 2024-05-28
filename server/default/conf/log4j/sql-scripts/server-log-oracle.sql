# ------------------------------------------------------------
# Create table server_log
# ------------------------------------------------------------

CREATE TABLE server_log (
  id         INTEGER  PRIMARY KEY,
  dtime      TIMESTAMP,
  trackingid VARCHAR2(255),
  loglevel   VARCHAR2(8),
  classname  VARCHAR2(255),
  partnerid  VARCHAR2(255),
  username   VARCHAR2(255),
  message    CLOB
);

CREATE SEQUENCE server_log_sequence
START WITH 1
INCREMENT BY 1;

CREATE OR REPLACE TRIGGER server_log_trigger BEFORE INSERT ON server_log REFERENCING NEW AS NEW FOR EACH ROW BEGIN SELECT server_log_sequence.nextval INTO :NEW.ID FROM dual; END;
.
RUN
