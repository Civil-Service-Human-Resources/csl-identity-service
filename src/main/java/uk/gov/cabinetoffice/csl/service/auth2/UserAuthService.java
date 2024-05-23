package uk.gov.cabinetoffice.csl.service.auth2;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

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
        Authentication authentication = getAuthentication();
        if(authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwtPrincipal) {
                return jwtPrincipal;
            }
        }
        return null;
    }

    @Override
    public String getUid() {
        Authentication authentication = getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        String uid = null;
        if (principal instanceof IdentityDetails) {
            uid = getIdentityDetails().getUsername();
        } else if (principal instanceof Jwt jwt) {
            uid = jwt.getClaim("user_name");
        }
        return uid;
    }

    @Override
    public String getEmail() {
        Authentication authentication = getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        String email = null;
        if (principal instanceof IdentityDetails) {
            email = getIdentityDetails().getIdentity().getEmail();
        } else if (principal instanceof Jwt jwt) {
            email = jwt.getClaim("email");
        }
        return email;
    }

    @Override
    public IdentityDetails getIdentityDetails() {
        return (IdentityDetails)getAuthentication().getPrincipal();
    }

    @Override
    public Identity getIdentity() {
        return getIdentityDetails().getIdentity();
    }
}
