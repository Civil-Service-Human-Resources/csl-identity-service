CREATE TABLE identity
(
    id                          int                     NOT NULL AUTO_INCREMENT,
    active                      boolean                 NOT NULL,
    locked                      boolean                 NOT NULL,
    uid                         char(36)                NOT NULL UNIQUE,
    email                       varchar(150)            NOT NULL UNIQUE,
    password                    varchar(100)            NOT NULL,
    last_logged_in              datetime                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deletion_notification_sent  boolean                 DEFAULT FALSE,
    agency_token_uid            char(36)                DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE role
(
    id              smallint                NOT NULL AUTO_INCREMENT,
    name            varchar(100)            NOT NULL UNIQUE,
    description     varchar(255)            DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE identity_role
(
    identity_id    int          NOT NULL,
    role_id        smallint     NOT NULL,
    PRIMARY KEY (identity_id, role_id),
    CONSTRAINT FK_identity_role_identity FOREIGN KEY (identity_id) REFERENCES identity (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_identity_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);
