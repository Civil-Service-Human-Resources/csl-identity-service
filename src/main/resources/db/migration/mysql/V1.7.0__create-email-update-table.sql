CREATE TABLE email_update (
  id                mediumint(10) unsigned      NOT NULL AUTO_INCREMENT,
  code              char(40)                    UNIQUE NOT NULL,
  email             varchar(255)                NOT NULL,
  identity_id       mediumint(8) unsigned       NOT NULL,
  timestamp         datetime                    NOT NULL,
  PRIMARY KEY (id),
  KEY fk_email_update_identity_id (identity_id),
  CONSTRAINT fk_email_update_identity_id FOREIGN KEY (identity_id) REFERENCES identity (id) ON DELETE CASCADE ON UPDATE CASCADE
);
