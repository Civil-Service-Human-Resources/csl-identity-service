package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.repository.Oauth2AuthorizationRepository;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@AllArgsConstructor
@Component
public class LogoutUtil {

    private final IUserAuthService userAuthService;
    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
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
        String username = userAuthService.getUsername();
        log.debug("LogoutUtil.username: {}", username);
        if (isNotBlank(username)) {
            Long n = oauth2AuthorizationRepository.deleteByPrincipalName(username);
            log.debug("LogoutUtil: {} Oauth2Authorization entries deleted from DB for user {}", n, username);
        }
    }
}
