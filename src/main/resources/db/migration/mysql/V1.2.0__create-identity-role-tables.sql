CREATE TABLE identity
(
    id                          mediumint(8) unsigned   NOT NULL AUTO_INCREMENT,
    active                      bit(1)                  NOT NULL,
    locked                      bit(1)                  NOT NULL,
    uid                         char(36)                NOT NULL,
    email                       varchar(150)            NOT NULL,
    password                    varchar(100)            NOT NULL,
    last_logged_in              datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletion_notification_sent  bit(1)                  DEFAULT FALSE,
    agency_token_uid            char(36)                DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_email (email),
    UNIQUE KEY UK_uid (uid)
);

CREATE TABLE role
(
    id              smallint(5) unsigned    NOT NULL AUTO_INCREMENT,
    name            varchar(100)            NOT NULL,
    description     varchar(255)            DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_name (name)
);

CREATE TABLE identity_role
(
     identity_id    mediumint(8)    unsigned NOT NULL,
     role_id        smallint(5)     unsigned NOT NULL,
     PRIMARY KEY (identity_id, role_id),
     KEY KEY_role_id (role_id),
     CONSTRAINT FK_identity_role_identity FOREIGN KEY (identity_id) REFERENCES identity (id) ON DELETE CASCADE ON UPDATE CASCADE,
     CONSTRAINT FK_identity_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);
