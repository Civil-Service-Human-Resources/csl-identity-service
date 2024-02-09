package uk.gov.cabinetoffice.csl.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.service.UserService;

@Slf4j
@AllArgsConstructor
@Component
public class AuthenticationEvents {

    private final UserService userService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        log.info("AuthenticationEvents:onSuccess:success: {}", success);
        Authentication authentication = success.getAuthentication();
        log.info("AuthenticationEvents:onSuccess:authentication: {}", authentication);
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof IdentityDetails identityDetails){
            userService.loginSucceeded(identityDetails.getIdentity());
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        log.info("AuthenticationEvents:onFailure:failure: {}", failure);
        Authentication authentication = failure.getAuthentication();
        log.info("AuthenticationFailureListener: authentication: {}", authentication);
        if (authentication.getPrincipal() != null
                && authentication.getPrincipal() instanceof String email) {
            log.info("AuthenticationFailureListener: email: {}", email);
            userService.loginFailed(email);
        }
    }
}