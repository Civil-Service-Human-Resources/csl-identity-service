package uk.gov.cabinetoffice.csl.service.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uk.gov.cabinetoffice.csl.domain.Identity;

import java.util.Collection;

import static java.util.stream.Collectors.toSet;

@AllArgsConstructor
@Getter
public class IdentityDetails implements UserDetails {

    private Identity identity;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return identity.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(toSet());
    }

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
