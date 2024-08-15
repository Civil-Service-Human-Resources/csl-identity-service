package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Role;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityDto {
    private String username;
    private String uid;
    private Set<String> roles = new HashSet<>();

    public IdentityDto(Identity identity) {
        this.username = identity.getEmail();
        this.uid = identity.getUid();
        Set<Role> identityRoles = identity.getRoles();
        if (identityRoles != null) {
            identityRoles.forEach(role -> this.roles.add(role.getName()));
        }
    }

    public IdentityDto(String username, String uid) {
        this.username = username;
        this.uid = uid;
    }
}
