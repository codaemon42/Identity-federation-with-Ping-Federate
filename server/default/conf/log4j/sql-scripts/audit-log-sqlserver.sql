/* ------------------------------------------------------------
 * Create table audit_log
 */------------------------------------------------------------

CREATE TABLE audit_log
(
	id          int NOT NULL IDENTITY (1, 1),
	dtime       datetime NULL,
	event       nvarchar(255) NULL,
	username    nvarchar(255) NULL,
	ip          nvarchar(255) NULL,
	app         nvarchar(2048) NULL,
	host        nvarchar(255) NULL,
	protocol    nvarchar(255) NULL,
	role        nvarchar(255) NULL,
	partnerid   nvarchar(255) NULL,
	status      nvarchar(255) NULL,
	adapterid   nvarchar(255) NULL,
	description nvarchar(MAX) NULL,
	responsetime int NULL,
	trackingid nvarchar(255) NULL
)  ON [PRIMARY]
GO

