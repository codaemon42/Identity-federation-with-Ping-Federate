/* ------------------------------------------------------------
 * Create table provisioner_log
 */------------------------------------------------------------

CREATE TABLE provisioner_log (
  id          SERIAL PRIMARY KEY,
  dtime       TIMESTAMP WITHOUT TIME ZONE,
  loglevel    VARCHAR(8),
  classname   VARCHAR(255),
  message     TEXT,
  channelcode VARCHAR(255)
);
