package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import uk.gov.cabinetoffice.csl.repository.Oauth2AuthorizationRepository;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
@Configuration
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler {

    private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        log.info("CustomLogoutSuccessHandler: authentication: {}", authentication);
        Jwt principal = (Jwt) authentication.getPrincipal();
        String principalName = principal.getClaim("user_name");
        log.info("CustomLogoutSuccessHandler: principalName: {}", principalName);
        Long l = oauth2AuthorizationRepository.deleteByPrincipalName(principalName);
        log.info("{} Oauth2Authorization entries are deleted from DB for user {}", l, principalName);
        String redirectUrl = request.getParameter("returnTo");
        response.sendRedirect(Objects.requireNonNullElse(redirectUrl, "/login"));
        super.onLogoutSuccess(request, response, authentication);
    }
}
