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
import uk.gov.cabinetoffice.csl.repository.Oauth2AuthorizationRepository;

@Slf4j
@AllArgsConstructor
@Configuration
public class CustomCookieClearingLogoutHandler implements LogoutHandler {

    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        for (Cookie cookie : request.getCookies()) {
            String cookieName = cookie.getName();
            log.info("CustomCookieClearingLogoutHandler.cookieName: {}", cookieName);
            Cookie cookieToDelete = new Cookie(cookieName, null);
            cookieToDelete.setMaxAge(0);
            response.addCookie(cookieToDelete);
            log.info("CustomCookieClearingLogoutHandler: authentication: {}", authentication);
            if(authentication != null) {
                Jwt principal = (Jwt) authentication.getPrincipal();
                String principalName = principal.getClaim("user_name");
                log.info("CustomCookieClearingLogoutHandler: principalName: {}", principalName);
                Long l = oauth2AuthorizationRepository.deleteByPrincipalName(principalName);
                log.info("CustomCookieClearingLogoutHandler: {} Oauth2Authorization entries are deleted from DB for user {}", l, principalName);
            }
        }
    }
}
