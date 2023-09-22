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

INSERT INTO identity_role
(
    identity_id, role_id
)
VALUES
    ((SELECT id FROM identity WHERE email = 'learner@domain.com'), (SELECT id FROM role WHERE name = 'LEARNER')),
    ((SELECT id FROM identity WHERE email = 'course-manager@domain.com'), (SELECT id FROM role WHERE name = 'COURSE_MANAGER')),
    ((SELECT id FROM identity WHERE email = 'identity-manager@domain.com'), (SELECT id FROM role WHERE name = 'IDENTITY_MANAGER')),
    ((SELECT id FROM identity WHERE email = 'organisation-reporter@domain.com'), (SELECT id FROM role WHERE name = 'ORGANISATION_REPORTER')),
    ((SELECT id FROM identity WHERE email = 'profession-reporter@domain.com'), (SELECT id FROM role WHERE name = 'PROFESSION_REPORTER')),
    ((SELECT id FROM identity WHERE email = 'cshr-reporter@domain.com'), (SELECT id FROM role WHERE name = 'CSHR_REPORTER'));
