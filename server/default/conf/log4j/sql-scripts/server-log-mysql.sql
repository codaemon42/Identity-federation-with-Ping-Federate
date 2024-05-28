# ------------------------------------------------------------
# Create table server_log
# ------------------------------------------------------------

CREATE TABLE `server_log` (
  id        INTEGER  AUTO_INCREMENT PRIMARY KEY,
  dtime     datetime,
  trackingid varchar(255),
  loglevel   varchar(8),
  classname  varchar(255),
  partnerid  varchar(255),
  username   varchar(255),
  message    text
);
