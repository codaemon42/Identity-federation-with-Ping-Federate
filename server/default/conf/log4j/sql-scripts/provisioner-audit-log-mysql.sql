# ------------------------------------------------------------
# Create table provisioner_audit_log
# ------------------------------------------------------------

CREATE TABLE `provisioner_audit_log` ( 
id INTEGER AUTO_INCREMENT PRIMARY KEY,
dtime          datetime,
cycle_id       varchar(255),
channel_id     varchar(255),
event_type     varchar(25),
source_id      varchar(255),
target_id      varchar(255),
is_success      varchar(8),
failure_cause  text
);