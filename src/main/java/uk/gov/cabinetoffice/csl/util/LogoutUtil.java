package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.repository.Oauth2AuthorizationRepository;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@AllArgsConstructor
@Component
public class LogoutUtil {

    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                log.debug("LogoutUtil.Cookie: {}", cookie);
                String cookieName = cookie.getName();
                log.debug("LogoutUtil.cookieName: {}", cookieName);
                Cookie cookieToDelete = new Cookie(cookieName, null);
                cookieToDelete.setMaxAge(0);
                response.addCookie(cookieToDelete);
            }
        }
        if (authentication != null) {
            String username = getUsernameFromPrincipal(authentication);
            log.debug("LogoutUtil.username: {}", username);
            if (isNotBlank(username)) {
                Long n = oauth2AuthorizationRepository.deleteByPrincipalName(username);
                log.debug("LogoutUtil: {} Oauth2Authorization entries deleted from DB for user {}", n, username);
            }
        }
    }

    private String getUsernameFromPrincipal(Authentication authentication) {
        String username = null;
        if (authentication.getPrincipal() instanceof IdentityDetails principal) {
            principal = (IdentityDetails) authentication.getPrincipal();
            username = principal.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt principal) {
            principal = (Jwt) authentication.getPrincipal();
            username = principal.getClaim("user_name");
        }
        return username;
    }
}
