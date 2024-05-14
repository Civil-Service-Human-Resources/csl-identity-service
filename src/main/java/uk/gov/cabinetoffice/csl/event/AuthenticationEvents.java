package uk.gov.cabinetoffice.csl.event;

import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.LoginService;

@AllArgsConstructor
@Component
public class AuthenticationEvents {

    private final LoginService loginService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        Authentication authentication = failure.getAuthentication();
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof String email) {
            loginService.loginFailed(email);
        }
    }
}
