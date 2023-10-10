package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.security.IdentityService;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${authenticationSuccess.targetUrl}")
    private String authenticationSuccessTargetUrl;

    private final IdentityService identityService;

    public CustomAuthenticationSuccessHandler(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        this.setDefaultTargetUrl(authenticationSuccessTargetUrl);
        if (authentication != null) {
            if (authentication.getPrincipal() != null) {
                if (authentication.getPrincipal() instanceof IdentityDetails identityDetails){
                    identityService.setLastLoggedIn(Instant.now(), identityDetails.getIdentity());
                }
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
