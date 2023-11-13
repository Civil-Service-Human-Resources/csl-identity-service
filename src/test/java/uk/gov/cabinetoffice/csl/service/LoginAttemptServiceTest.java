package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class LoginAttemptServiceTest {

    private Map<String, Integer> loginAttemptCache = new HashMap<>();
    private int maxLoginAttempts = 3;

    private UserService userService = mock(UserService.class);

    private LoginAttemptService loginAttemptService =
            new LoginAttemptService(maxLoginAttempts, userService, loginAttemptCache);

    @Test
    public void loginSucceedSetAttemptsToZero() {
        String email = "test@domain.com";

        loginAttemptCache.put(email, 3);

        loginAttemptService.loginSucceeded(email);

        assertEquals(Integer.valueOf(0), loginAttemptCache.get(email));
    }

    @Test
    public void loginFailedIncrementsFailedAttempts() {
        String email = "test@domain.com";

        loginAttemptCache.clear();

        when(userService.existsByEmail(email)).thenReturn(true);

        loginAttemptService.loginFailed(email);

        assertEquals(Integer.valueOf(1), loginAttemptCache.get(email));

        verify(userService, times(0)).lockIdentity(email);
    }

    @Test
    public void loginFailedDoesNotIncrementCacheIfIdentityDoesNotExist() {
        String email = "test@domain.com";

        loginAttemptCache.clear();

        when(userService.existsByEmail(email)).thenReturn(false);

        loginAttemptService.loginFailed(email);

        assertNull(loginAttemptCache.get(email));
    }


    @Test
    public void loginFailedLocksIdentityWhenMaxAttemptsIsExceeded() {
        String email = "test@domain.com";

        loginAttemptCache.put(email, 3);

        when(userService.existsByEmail(email)).thenReturn(true);

        try {
            loginAttemptService.loginFailed(email);
        } catch (AuthenticationException e) {
            assertEquals("User account is locked", e.getMessage());
        }

        verify(userService).lockIdentity(email);
    }
}
