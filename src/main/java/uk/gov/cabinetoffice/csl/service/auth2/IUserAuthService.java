package uk.gov.cabinetoffice.csl.service.auth2;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

public interface IUserAuthService {

    Authentication getAuthentication();

    String getUsername();

    Jwt getBearerTokenFromUserAuth();

    IdentityDetails getIdentityDetails();

    Identity getIdentity();

    IdentityDTO resolveIdentity();
}
