CREATE TABLE invite (
    id                      mediumint(8) unsigned   NOT NULL AUTO_INCREMENT,
    inviter_id              mediumint(8) unsigned,
    code                    char(40)                NOT NULL,
    status                  varchar(10)             NOT NULL,
    for_email               varchar(150)            NOT NULL,
    invited_at              datetime                NOT NULL,
    accepted_at             datetime                DEFAULT NULL,
    is_authorised_invite    bit(1)                  DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY code (code),
    KEY KEY_inviter_id (inviter_id),
    CONSTRAINT FK_invite_identity FOREIGN KEY (inviter_id) REFERENCES identity (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX invite_status_idx ON invite (status);
CREATE INDEX invite_for_email_idx ON invite (for_email);

CREATE TABLE invite_role (
    invite_id               mediumint(8) unsigned   NOT NULL,
    role_id                 smallint(5) unsigned    NOT NULL,
    PRIMARY KEY (invite_id,role_id),
    KEY KEY_role_id (role_id),
    CONSTRAINT FK_invite_role_invite FOREIGN KEY (invite_id) REFERENCES invite (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_invite_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);
