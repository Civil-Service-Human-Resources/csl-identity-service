ALTER TABLE email_update RENAME COLUMN  email                   TO new_email;
ALTER TABLE email_update RENAME COLUMN  timestamp               TO requested_at;
ALTER TABLE email_update ADD            updated_at              datetime DEFAULT NULL;
ALTER TABLE email_update ADD            previous_email          varchar(255) DEFAULT NULL;
ALTER TABLE email_update ADD            email_update_status     varchar(10) DEFAULT NULL;
