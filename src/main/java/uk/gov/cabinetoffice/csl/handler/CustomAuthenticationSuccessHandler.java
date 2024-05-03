package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.LoginService;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Configuration
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${authenticationSuccess.targetUrl}")
    private String authenticationSuccessTargetUrl;

    @Value("${lpg.uiSignOutUrl}")
    private String lpgUiSignOutUrl;

    @Value("${maintenancePage.enabled}")
    private boolean maintenancePageEnabled;

    @Value("${maintenancePage.skipForUsers}")
    private String skipMaintenancePageForUsers;

    private final LoginService loginService;

    public CustomAuthenticationSuccessHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        this.setDefaultTargetUrl(authenticationSuccessTargetUrl);
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof IdentityDetails identityDetails) {
            Identity identity = identityDetails.getIdentity();
            if(maintenancePageEnabled) {
                String username = identity.getEmail();
                boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                                .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
                if(skipMaintenancePage) {
                    log.info("Maintenance page is skipped for the user: {}", username);
                    loginService.loginSucceeded(identity);
                } else {
                    log.info("Trying to logout the user to Display Maintenance page for the user: {}", username);
                    response.sendRedirect(lpgUiSignOutUrl);
                    log.info("Redirecting the user {} to lpg-ui/sign-out url to logout.", username);
                }
            } else {
                loginService.loginSucceeded(identity);
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
