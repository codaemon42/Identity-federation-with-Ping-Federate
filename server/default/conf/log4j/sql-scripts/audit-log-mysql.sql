# ------------------------------------------------------------
# Create table audit_log
# ------------------------------------------------------------

CREATE TABLE `audit_log` (
  id        INTEGER  AUTO_INCREMENT PRIMARY KEY,
  dtime     datetime,
  event     varchar(255),
  username  varchar(255),
  ip        varchar(255),
  app       varchar(2048),
  host      varchar(255),
  protocol  varchar(255),
  role      varchar(255),
  partnerid varchar(255),
  status varchar(255),
  adapterid varchar(255),
  description text,
  responsetime INTEGER,
  trackingid varchar(255)
);
