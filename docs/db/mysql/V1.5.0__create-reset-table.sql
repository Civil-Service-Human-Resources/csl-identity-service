CREATE TABLE reset (
    id                  mediumint(10) unsigned      NOT NULL AUTO_INCREMENT,
    code                char(40)                    NOT NULL UNIQUE,
    email               varchar(150)                NOT NULL,
    reset_status        varchar(10)                 NOT NULL,
    requested_at        datetime                    NOT NULL,
    reset_at            datetime                    DEFAULT NULL,
    PRIMARY KEY (id)
);
