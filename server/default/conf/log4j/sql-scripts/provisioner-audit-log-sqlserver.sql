/* ------------------------------------------------------------
 * Create table provisioner_audit_log
 */------------------------------------------------------------

CREATE TABLE provisioner_audit_log
(
	id         int NOT NULL IDENTITY (1, 1),
	dtime      datetime NULL,
    cycle_id       nvarchar(255) NULL,
    channel_id     nvarchar(255) NULL,
    event_type     nvarchar(25) NULL,
    source_id      nvarchar(255) NULL,
    target_id      nvarchar(255) NULL,
    is_success      nvarchar(8) NULL,
    failure_cause    nvarchar(MAX) NULL
)  ON [PRIMARY]
GO
