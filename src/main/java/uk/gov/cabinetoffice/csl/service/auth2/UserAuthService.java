package uk.gov.cabinetoffice.csl.service.auth2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.ClientAuthenticationErrorException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
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
        Authentication authentication = getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        String username = null;
        if (principal instanceof IdentityDetails) {
            username = getIdentityDetails().getIdentity().getEmail();
        } else if (principal instanceof Jwt jwt) {
            username = jwt.getClaim("user_name");
        }
        if (isBlank(username)) {
            log.error("Learner Id is missing from authentication token");
            throw new ClientAuthenticationErrorException("System error");
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
