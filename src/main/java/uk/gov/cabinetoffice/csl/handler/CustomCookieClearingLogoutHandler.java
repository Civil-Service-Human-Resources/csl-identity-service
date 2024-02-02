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
public class CustomCookieClearingLogoutHandler implements LogoutHandler {

    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                String cookieName = cookie.getName();
                log.info("CustomCookieClearingLogoutHandler.cookieName: {}", cookieName);
                Cookie cookieToDelete = new Cookie(cookieName, null);
                cookieToDelete.setMaxAge(0);
                response.addCookie(cookieToDelete);
            }
        }
        log.info("CustomCookieClearingLogoutHandler: authentication: {}", authentication);
        if (authentication != null) {
            String principalName = null;
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt principal = (Jwt) authentication.getPrincipal();
                principalName = principal.getClaim("user_name");
            }
            if (authentication.getPrincipal() instanceof IdentityDetails) {
                IdentityDetails principal = (IdentityDetails) authentication.getPrincipal();
                principalName = principal.getUsername();
            }
            log.info("CustomCookieClearingLogoutHandler: principalName: {}", principalName);
            if (isNotBlank(principalName)) {
                Long l = oauth2AuthorizationRepository.deleteByPrincipalName(principalName);
                log.info("CustomCookieClearingLogoutHandler: {} Oauth2Authorization entries are deleted from DB for user {}", l, principalName);
            }
        }
    }
}
