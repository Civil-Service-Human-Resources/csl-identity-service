package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.security.IdentityService;

import java.io.IOException;
import java.time.Instant;

@AllArgsConstructor
@Configuration
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final IdentityService identityService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
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
