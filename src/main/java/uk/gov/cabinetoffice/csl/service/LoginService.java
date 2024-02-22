package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

import java.time.Instant;

@Slf4j
@Service
public class LoginService {

    private final int maxLoginAttempts;
    private final IdentityRepository identityRepository;

    public LoginService(@Value("${account.lockout.maxLoginAttempts}") int maxLoginAttempts,
                        IdentityRepository identityRepository) {
        this.maxLoginAttempts = maxLoginAttempts;
        this.identityRepository = identityRepository;
    }

    public void loginSucceeded(Identity identity) {
        log.debug("LoginService.loginSucceeded: {}", identity);
        identity.setLastLoggedIn(Instant.now());
        identity.setFailedLoginAttempts(0);
        identityRepository.save(identity);
    }

    public void loginFailed(String email) {
        log.debug("LoginService:loginFailed: {}", email);
        Identity identity = identityRepository.findFirstByEmailEqualsIgnoreCase(email);
        if(identity != null && email.equalsIgnoreCase(identity.getEmail())) {
            incrementFailedLoginAttempt(identity);
            if(identity.getFailedLoginAttempts() >= maxLoginAttempts) {
                lockIdentity(identity);
            }
            identityRepository.save(identity);
        }
    }

    private void incrementFailedLoginAttempt(Identity identity) {
        Integer currentFailedLoginAttempts = identity.getFailedLoginAttempts();
        identity.setFailedLoginAttempts(currentFailedLoginAttempts + 1);
    }

    private void lockIdentity(Identity identity) {
        identity.setLocked(true);
        identityRepository.save(identity);
        log.info("LoginService.loginFailed:User account is locked for email: {}", identity.getEmail());
        throw new AuthenticationException("User account is locked") {};
    }
}
