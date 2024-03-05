package uk.gov.cabinetoffice.csl.service.auth2;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.ClientAuthenticationErrorException;

import static org.apache.commons.lang3.StringUtils.isBlank;

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
    public String getUsername() {
        String username = "";
        Jwt jwtPrincipal = getBearerTokenFromUserAuth();
        if (jwtPrincipal != null) {
            username = (String) jwtPrincipal.getClaims().get("user_name");
        }
        if (isBlank(username)) {
            throw new ClientAuthenticationErrorException("Learner Id is missing from authentication token");
        }
        return username;
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
