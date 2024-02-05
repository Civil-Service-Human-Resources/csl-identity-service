package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.repository.Oauth2AuthorizationRepository;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@AllArgsConstructor
@Configuration
public class CustomCookieAndAuth2TokenClearingLogoutHandler implements LogoutHandler {

    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                String cookieName = cookie.getName();
                log.debug("CustomCookieAndAuth2TokenClearingLogoutHandler.cookieName: {}", cookieName);
                log.debug("CustomCookieAndAuth2TokenClearingLogoutHandler.Cookie: {}", cookie);
                Cookie cookieToDelete = new Cookie(cookieName, null);
                cookieToDelete.setMaxAge(0);
                response.addCookie(cookieToDelete);
            }
        }
        log.debug("CustomCookieAndAuth2TokenClearingLogoutHandler: authentication: {}", authentication);
        if (authentication != null) {
            String principalName = null;
            if (authentication.getPrincipal() instanceof IdentityDetails principal) {
                principal = (IdentityDetails) authentication.getPrincipal();
                principalName = principal.getUsername();
            } else if (authentication.getPrincipal() instanceof Jwt principal) {
                principal = (Jwt) authentication.getPrincipal();
                principalName = principal.getClaim("user_name");
            }
            log.debug("CustomCookieAndAuth2TokenClearingLogoutHandler: principalName: {}", principalName);
            if (isNotBlank(principalName)) {
                Long l = oauth2AuthorizationRepository.deleteByPrincipalName(principalName);
                log.debug("CustomCookieAndAuth2TokenClearingLogoutHandler: " +
                        "{} Oauth2Authorization entries deleted from DB for user {}", l, principalName);
            }
        }
    }
}
