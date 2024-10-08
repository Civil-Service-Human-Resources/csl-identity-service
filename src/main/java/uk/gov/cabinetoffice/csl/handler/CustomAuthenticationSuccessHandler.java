package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.LoginService;
import uk.gov.cabinetoffice.csl.util.MaintenancePageUtil;

import java.io.IOException;

@Configuration
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${authenticationSuccess.targetUrl}")
    private String authenticationSuccessTargetUrl;

    private final LoginService loginService;

    private final MaintenancePageUtil maintenancePageUtil;

    public CustomAuthenticationSuccessHandler(LoginService loginService,
                                              MaintenancePageUtil maintenancePageUtil) {
        this.loginService = loginService;
        this.maintenancePageUtil = maintenancePageUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        this.setDefaultTargetUrl(authenticationSuccessTargetUrl);
        if(!maintenancePageUtil.skipMaintenancePageForUser(request)) {
            response.sendRedirect("/maintenance");
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof IdentityDetails identityDetails) {
            Identity identity = identityDetails.getIdentity();
            loginService.loginSucceeded(identity);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
