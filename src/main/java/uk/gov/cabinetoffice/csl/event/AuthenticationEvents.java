package uk.gov.cabinetoffice.csl.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.LoginService;

@Slf4j
@AllArgsConstructor
@Component
public class AuthenticationEvents {

    private final LoginService loginService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        Authentication authentication = success.getAuthentication();
        log.info("AuthenticationEvents:onSuccess:authentication: {}", authentication);
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof IdentityDetails identityDetails){
            loginService.loginSucceeded(identityDetails.getIdentity());
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        Authentication authentication = failure.getAuthentication();
        log.debug("AuthenticationEvents.onFailure:authentication: {}", authentication);
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof String email) {
            log.info("AuthenticationEvents.onFailure: authentication failed for email: {}", email);
            loginService.loginFailed(email);
        }
    }
}
