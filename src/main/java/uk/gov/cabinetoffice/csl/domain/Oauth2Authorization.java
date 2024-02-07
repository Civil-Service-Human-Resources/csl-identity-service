package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Entity
@Table(name = "OAUTH2_AUTHORIZATION")
public class Oauth2Authorization implements Serializable {

    @Id
    @Size(max = 100)
    @Column(name = "ID", nullable = false, length = 100)
    private String id;

    @Size(max = 100)
    @NotNull
    @Column(name = "REGISTERED_CLIENT_ID", nullable = false, length = 100)
    private String registeredClientId;

    @Size(max = 200)
    @NotNull
    @Column(name = "PRINCIPAL_NAME", nullable = false, length = 200)
    private String principalName;

    @Size(max = 100)
    @NotNull
    @Column(name = "AUTHORIZATION_GRANT_TYPE", nullable = false, length = 100)
    private String authorizationGrantType;

    @Size(max = 1000)
    @Column(name = "AUTHORIZED_SCOPES", length = 1000)
    private String authorizedScopes;

    @Size(max = 500)
    @Column(name = "STATE", length = 500)
    private String state;

    @Column(name = "AUTHORIZATION_CODE_ISSUED_AT")
    private Instant authorizationCodeIssuedAt;

    @Column(name = "AUTHORIZATION_CODE_EXPIRES_AT")
    private Instant authorizationCodeExpiresAt;

    @Column(name = "ACCESS_TOKEN_ISSUED_AT")
    private Instant accessTokenIssuedAt;

    @Column(name = "ACCESS_TOKEN_EXPIRES_AT")
    private Instant accessTokenExpiresAt;

    @Column(name = "REFRESH_TOKEN_ISSUED_AT")
    private Instant refreshTokenIssuedAt;

    @Size(max = 100)
    @Column(name = "ACCESS_TOKEN_TYPE", length = 100)
    private String accessTokenType;

    @Column(name = "REFRESH_TOKEN_EXPIRES_AT")
    private Instant refreshTokenExpiresAt;

    @Size(max = 1000)
    @Column(name = "ACCESS_TOKEN_SCOPES", length = 1000)
    private String accessTokenScopes;

    @Column(name = "USER_CODE_ISSUED_AT")
    private Instant userCodeIssuedAt;

    @Column(name = "USER_CODE_EXPIRES_AT")
    private Instant userCodeExpiresAt;

    @Column(name = "OIDC_ID_TOKEN_ISSUED_AT")
    private Instant oidcIdTokenIssuedAt;

    @Column(name = "DEVICE_CODE_ISSUED_AT")
    private Instant deviceCodeIssuedAt;

    @Column(name = "OIDC_ID_TOKEN_EXPIRES_AT")
    private Instant oidcIdTokenExpiresAt;

    @Column(name = "DEVICE_CODE_EXPIRES_AT")
    private Instant deviceCodeExpiresAt;

/*
    TODO [JPA Buddy] create field to map the 'ATTRIBUTES' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "ATTRIBUTES", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object attributes;
*/
/*
    TODO [JPA Buddy] create field to map the 'AUTHORIZATION_CODE_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "AUTHORIZATION_CODE_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object authorizationCodeValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'AUTHORIZATION_CODE_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "AUTHORIZATION_CODE_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object authorizationCodeMetadata;
*/
/*
    TODO [JPA Buddy] create field to map the 'ACCESS_TOKEN_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "ACCESS_TOKEN_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object accessTokenValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'ACCESS_TOKEN_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "ACCESS_TOKEN_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object accessTokenMetadata;
*/
/*
    TODO [JPA Buddy] create field to map the 'OIDC_ID_TOKEN_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "OIDC_ID_TOKEN_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object oidcIdTokenValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'OIDC_ID_TOKEN_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "OIDC_ID_TOKEN_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object oidcIdTokenMetadata;
*/
/*
    TODO [JPA Buddy] create field to map the 'REFRESH_TOKEN_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "REFRESH_TOKEN_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object refreshTokenValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'REFRESH_TOKEN_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "REFRESH_TOKEN_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object refreshTokenMetadata;
*/
/*
    TODO [JPA Buddy] create field to map the 'USER_CODE_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "USER_CODE_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object userCodeValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'USER_CODE_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "USER_CODE_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object userCodeMetadata;
*/
/*
    TODO [JPA Buddy] create field to map the 'DEVICE_CODE_VALUE' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "DEVICE_CODE_VALUE", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object deviceCodeValue;
*/
/*
    TODO [JPA Buddy] create field to map the 'DEVICE_CODE_METADATA' column
     Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "DEVICE_CODE_METADATA", columnDefinition = "BINARY LARGE OBJECT(0, 0)")
    private Object deviceCodeMetadata;
*/
}
