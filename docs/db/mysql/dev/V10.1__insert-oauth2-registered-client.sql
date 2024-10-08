INSERT INTO oauth2_registered_client
(
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
VALUES
(
    "116852c8-f968-4dd1-9495-c0edf96f42e4",
    "9fbd4ae2-2db3-44c7-9544-88e80255b56e",
    CURRENT_TIMESTAMP,
    "$2a$10$IQ7.gaYFuZ9Ta9lRb6Ft1OeTSqylNncskKx.Wj9/wxji8Zpxu5fBi",
    NULL,
    "lpg-ui",
    "client_secret_jwt,client_secret_basic,client_secret_post",
    "refresh_token,client_credentials,password,authorization_code,urn:ietf:params:oauth:grant-type:jwt-bearer",
    "http://localhost:3001/authenticate",
    "http://localhost:3001/authenticate",
    "read,openid,write",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":false}",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",43200.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",86400.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}"
);

INSERT INTO oauth2_registered_client
(
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
VALUES
(
    "216852c8-f968-4dd1-9495-c0edf96f42e4",
    "8fbd4ae2-2db3-44c7-9544-88e80255b56e",
    CURRENT_TIMESTAMP,
    "$2a$10$IQ7.gaYFuZ9Ta9lRb6Ft1OeTSqylNncskKx.Wj9/wxji8Zpxu5fBi",
    NULL,
    "lpg-management",
    "client_secret_jwt,client_secret_basic,client_secret_post",
    "refresh_token,client_credentials,password,authorization_code,urn:ietf:params:oauth:grant-type:jwt-bearer",
    "http://localhost:3005/authenticate",
    "http://localhost:3005/authenticate",
    "read,openid,write",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":false}",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",43200.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",86400.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}"
);

INSERT INTO oauth2_registered_client
(
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
VALUES
(
    "316852c8-f968-4dd1-9495-c0edf96f42e4",
    "7fbd4ae2-2db3-44c7-9544-88e80255b56e",
    CURRENT_TIMESTAMP,
    "$2a$10$IQ7.gaYFuZ9Ta9lRb6Ft1OeTSqylNncskKx.Wj9/wxji8Zpxu5fBi",
    NULL,
    "identity-management",
    "client_secret_jwt,client_secret_basic,client_secret_post",
    "refresh_token,client_credentials,password,authorization_code,urn:ietf:params:oauth:grant-type:jwt-bearer",
    "http://localhost:8081/mgmt/login",
    "http://localhost:8081/mgmt/login",
    "read,openid,write",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":false}",
    "{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",43200.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",86400.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}"
);
