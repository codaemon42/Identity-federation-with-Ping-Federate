/* ------------------------------------------------------------
 * Create table server_log
 */------------------------------------------------------------

CREATE TABLE server_log
(
    id         int NOT NULL IDENTITY (1, 1),
    dtime      datetime NULL,
    trackingid nvarchar(255) NULL,
    loglevel   nvarchar(8) NULL,
    classname  nvarchar(255) NULL,
    partnerid  nvarchar(255) NULL,
    username   nvarchar(255) NULL,
    message    nvarchar(MAX) NULL
)  ON [PRIMARY]
GO

