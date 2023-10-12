CREATE TABLE invite (
    id                      int                     NOT NULL AUTO_INCREMENT,
    inviter_id              int                     NOT NULL,
    code                    char(40)                NOT NULL UNIQUE,
    status                  varchar(10)             NOT NULL,
    for_email               varchar(150)            NOT NULL,
    invited_at              datetime                NOT NULL,
    accepted_at             datetime                DEFAULT NULL,
    is_authorised_invite    boolean                 DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT FK_invite_identity FOREIGN KEY (inviter_id) REFERENCES identity (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX invite_status_idx ON invite (status);
CREATE INDEX invite_for_email_idx ON invite (for_email);

CREATE TABLE invite_role (
    invite_id               int                     NOT NULL,
    role_id                 int                     NOT NULL,
    PRIMARY KEY (invite_id,role_id),
    CONSTRAINT FK_invite_role_invite FOREIGN KEY (invite_id) REFERENCES invite (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_invite_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE ON UPDATE CASCADE
);
