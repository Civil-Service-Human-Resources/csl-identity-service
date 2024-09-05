package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import uk.gov.cabinetoffice.csl.util.LogoutUtil;

@AllArgsConstructor
@Configuration
public class CustomCookieAndAuth2TokenClearingLogoutHandler implements LogoutHandler {

    private final LogoutUtil logoutUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        logoutUtil.logout(request, response);
    }
}
