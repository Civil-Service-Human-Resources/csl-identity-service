package uk.gov.cabinetoffice.csl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LoginAttemptService {

    private final Map<String, Integer> loginAttemptCache;
    private final int maxAttempt;
    private final UserService userService;

    public LoginAttemptService(@Value("${account.lockout.maxAttempt}") int maxAttempt,
            UserService userService,
            @Qualifier("loginAttemptCache") Map<String, Integer> loginAttemptCache) {
        this.maxAttempt = maxAttempt;
        this.userService = userService;
        this.loginAttemptCache = loginAttemptCache;
    }

    public void loginSucceeded(String email) {
        loginAttemptCache.replace(email, 0);
    }

    public void loginFailed(String email) {
        if (identityExists(email)) {
            incrementAttempts(email);
            if (areAttemptsMoreThanAllowedLimit(email)) {
                lockIdentity(email);
                throw new AuthenticationException("User account is locked") {
                };
            }
        }
    }

    private void lockIdentity(String email) {
        userService.lockIdentity(email);
    }

    private void incrementAttempts(String email) {
        loginAttemptCache.merge(email, 1, Integer::sum);
    }

    private boolean areAttemptsMoreThanAllowedLimit(String email) {
        return loginAttemptCache.get(email) >= maxAttempt;
    }

    private boolean identityExists(String email) {
        return userService.existsByEmail(email);
    }
}
