DELETE FROM identity.flyway_schema_history WHERE script = "V1.10.0__create-oauth2-tables.sql";

DELETE FROM identity.flyway_schema_history WHERE script = "V1.11.0__add-failed-login-attempts-to-identity-table.sql";

DELETE FROM identity.flyway_schema_history WHERE script = "V1.11.1__edit-email-update-table.sql";

COMMIT;
