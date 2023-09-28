package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uk.gov.cabinetoffice.csl.domain.Identity;

import java.util.Collection;

import static java.util.stream.Collectors.toSet;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityDetails implements UserDetails {

    private Identity identity;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return identity.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(toSet());
    }

//    private void setAuthorities(Collection<? extends GrantedAuthority> authorities) {}

    @Override
    public String getPassword() {
        return identity.getPassword();
    }

    @Override
    public String getUsername() {
        return identity.getUid();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !identity.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
