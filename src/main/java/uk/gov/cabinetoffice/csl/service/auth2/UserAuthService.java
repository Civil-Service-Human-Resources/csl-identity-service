package uk.gov.cabinetoffice.csl.service.auth2;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Role;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.ClientAuthenticationErrorException;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UserAuthService implements IUserAuthService {

    private final SecurityContextService securityContextService;

    @Override
    public Authentication getAuthentication() {
        return securityContextService.getSecurityContext().getAuthentication();
    }

    @Override
    public Jwt getBearerTokenFromUserAuth() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwtPrincipal) {
            return jwtPrincipal;
        } else {
            return null;
        }
    }

    @Override
    public String getUsername() {
        String username = "";
        Jwt jwtPrincipal = getBearerTokenFromUserAuth();
        if (jwtPrincipal != null) {
            username = (String) jwtPrincipal.getClaims().get("user_name");
        }
        if (StringUtils.isBlank(username)) {
            throw new ClientAuthenticationErrorException("Learner Id is missing from authentication token");
        }
        return username;
    }

    @Override
    public IdentityDetails getIdentityDetails() {
        Authentication authentication = getAuthentication();
        return ((IdentityDetails) authentication.getPrincipal());
    }

    @Override
    public Identity getIdentity() {
        return getIdentityDetails().getIdentity();
    }

    @Override
    public IdentityDTO resolveIdentity() {
        String email = getIdentityDetails().getIdentity().getEmail();
        String uid = getUsername();
        Set<String> roles = getIdentityDetails().getIdentity().getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new IdentityDTO(email, uid, roles);
    }
}
