/*------------------------------------------------------------
 * Create table provisioner_log
 */-----------------------------------------------------------

CREATE TABLE provisioner_log
(
	id         int NOT NULL IDENTITY (1, 1),
	dtime      datetime NULL,
    loglevel   nvarchar(8) NULL,
    classname  nvarchar(255) NULL,
    message    nvarchar(MAX) NULL,
	channelcode nvarchar(255) NULL
)  ON [PRIMARY]
GO
