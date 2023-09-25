package uk.gov.cabinetoffice.csl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.cabinetoffice.csl.domain.Identity;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class IdentityDTO {
    private String username;
    private String uid;
    private Set<String> roles = new HashSet<>();

    public IdentityDTO(Identity identity) {
        this.username = identity.getEmail();
        this.uid = identity.getUid();
        identity.getRoles().forEach(role -> this.roles.add(role.getName()));
    }

    public IdentityDTO(String username, String uid) {
        this.username = username;
        this.uid = uid;
    }
}
