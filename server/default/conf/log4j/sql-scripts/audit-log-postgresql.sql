/* ------------------------------------------------------------
 * Create table audit_log
 */------------------------------------------------------------

CREATE TABLE audit_log (
  id            SERIAL  PRIMARY KEY,
  dtime         TIMESTAMP WITHOUT TIME ZONE,
  event         TEXT,
  username      VARCHAR(255),
  ip            VARCHAR(255),
  app           VARCHAR(2048),
  host          VARCHAR(255),
  protocol      VARCHAR(255),
  role          VARCHAR(255),
  partnerid     VARCHAR(255),
  status        VARCHAR(255),
  adapterid     VARCHAR(255),
  description   TEXT,
  responsetime  INTEGER,
  trackingid    VARCHAR(255)
);
