INSERT INTO role
(
    name
)
VALUES
    ('LEARNER'),
    ('LEARNING_MANAGER'),
    ('IDENTITY_MANAGER'),
    ('ORGANISATION_REPORTER'),
    ('PROFESSION_REPORTER'),
    ('CSHR_REPORTER'),
    ('DOWNLOAD_BOOKING_FEED'),
    ('MANAGE_CALL_OFF_PO'),
    ('ORGANISATION_MANAGER'),
    ('PROFESSION_MANAGER'),
    ('LEARNING_CREATE'),
    ('LEARNING_PUBLISH'),
    ('LEARNING_EDIT'),
    ('LEARNING_ARCHIVE'),
    ('LEARNING_DELETE'),
    ('CSL_AUTHOR'),
    ('ORGANISATION_AUTHOR'),
    ('PROFESSION_AUTHOR'),
    ('KPMG_SUPPLIER_AUTHOR'),
    ('SUPPLIER_REPORTER'),
    ('KORNFERRY_SUPPLIER_AUTHOR'),
    ('KNOWLEDGEPOOL_SUPPLIER_AUTHOR'),
    ('KPMG_SUPPLIER_REPORTER'),
    ('KORNFERRY_SUPPLIER_REPORTER'),
    ('IDENTITY_DELETE');


INSERT INTO identity
(
    active, locked, email, uid, password
)
VALUES
    (true, false, 'learner@domain.com', '3c706a70-3fff-4e7b-ae7f-102c1d46f569', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC'),
    (true, false, 'course-manager@domain.com', '8dc80f78-9a52-4c31-ac54-d280a70c18eb', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC'),
    (true, false, 'identity-manager@domain.com', '65313ea4-59ea-4802-a521-71f9a92c85cd', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC'),
    (true, false, 'organisation-reporter@domain.com', 'ef422d43-53f1-492a-a159-54b8c5348df8', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC'),
    (true, false, 'profession-reporter@domain.com', '5b1a0e11-12f5-47a8-9fe2-e272184defc9', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC'),
    (true, false, 'cshr-reporter@domain.com', 'c4cb1208-eca7-46a6-b496-0f6f354c6eac', '$2a$10$eEvWOZ/wXNwL11SaQNV2q.CjE3AoIaRCle2984yYEB8Ul7UbreZNC');
