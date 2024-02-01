package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@Slf4j
@Configuration
public class CustomCookieClearingLogoutHandler implements LogoutHandler {

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        for (Cookie cookie : request.getCookies()) {
            String cookieName = cookie.getName();
            log.info("CustomCookieClearingLogoutHandler.cookieName: {}", cookieName);
            Cookie cookieToDelete = new Cookie(cookieName, null);
            cookieToDelete.setMaxAge(0);
            response.addCookie(cookieToDelete);
        }
    }
}
