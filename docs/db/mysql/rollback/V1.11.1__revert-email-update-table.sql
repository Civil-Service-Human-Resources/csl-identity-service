ALTER TABLE identity.email_update RENAME COLUMN  new_email               TO email;
ALTER TABLE identity.email_update RENAME COLUMN  requested_at            TO timestamp;
ALTER TABLE identity.email_update DROP           updated_at;
ALTER TABLE identity.email_update DROP           previous_email;
ALTER TABLE identity.email_update DROP           email_update_status;
