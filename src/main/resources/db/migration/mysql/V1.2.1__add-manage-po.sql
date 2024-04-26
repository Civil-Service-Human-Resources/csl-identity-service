
INSERT INTO `role` (name) VALUES ('MANAGE_CALL_OFF_PO');

INSERT INTO `identity` (active, locked, email, uid, password) VALUES
(true, false, 'manage-po@domain.com', 'f7cb1208-eca7-46a6-b496-0f6f354c6eac', '$2a$10$sGfnyPnJ8a0b9R.vqIphKu5vjetS3.Bvi6ISv39bOphq5On0U2m36')
;

INSERT INTO `identity_role` (identity_id, role_id) VALUES
((SELECT id FROM identity WHERE email = 'manage-po@domain.com'), (SELECT id FROM role WHERE name = 'MANAGE_CALL_OFF_PO'))
;
