# ------------------------------------------------------------
# Create table provisioner_log
# ------------------------------------------------------------

CREATE TABLE `provisioner_log` (
  id        INTEGER  AUTO_INCREMENT PRIMARY KEY,
  dtime     datetime,
  loglevel   varchar(8),
  classname  varchar(255),
  message    text,
  channelcode varchar(255)
);
