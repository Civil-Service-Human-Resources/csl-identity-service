package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.service.ReactivationService;

import java.io.IOException;
import java.time.Clock;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;

@SpringBootTest
@ActiveProfiles("no-redis")
public class CustomAuthenticationFailureHandlerTest {

    private final int maxLoginAttempts = 5;

    private final String email = "learner@domain.com";

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @MockBean
    private ReactivationService reactivationService;

    @Autowired
    private Clock clock;

    @Test
    public void shouldSetErrorToFailedOnFailedLogin() throws IOException {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("Some other error", reactivation);
        verify(response).sendRedirect("/login?error=failed&maxLoginAttempts=" + maxLoginAttempts);
    }

    @Test
    public void shouldSetErrorToLockedOnAccountLock() throws IOException {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("User account is locked", reactivation);
        verify(response).sendRedirect("/login?error=locked&maxLoginAttempts=" + maxLoginAttempts);
    }

    @Test
    public void shouldSetErrorToBlockedOnAccountBlocked() throws IOException {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("User account is blocked", reactivation);
        verify(response).sendRedirect("/login?error=blocked");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivation() throws IOException {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("User account is deactivated", reactivation);
        String encryptedEmail = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";
        verify(response).sendRedirect("/login?error=deactivated&username="
                + encode(encryptedEmail, UTF_8));
    }

    @Test
    public void shouldSetErrorToReactivatedExpiredOnAccountReactivationExpired() throws IOException {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("Reactivation request has expired", reactivation);
        String encryptedEmail = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";
        verify(response).sendRedirect("/login?error=reactivation-expired&username="
                + encode(encryptedEmail, UTF_8));
    }

    @Test
    public void shouldSetErrorToPendingReactivationOnAccountDeactivatedAndPendingReactivationExists()
            throws Exception {
        Reactivation reactivation = createPendingReactivation();
        HttpServletResponse response = executeHandler("Pending reactivation exists for user", reactivation);
        verify(response).sendRedirect("/login?error=pending-reactivation");
    }

    private HttpServletResponse executeHandler(String message, Reactivation pendingReactivation) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn(message);
        when(request.getParameter("username")).thenReturn(email);
        when(reactivationService.getPendingReactivationForEmail(email)).thenReturn(pendingReactivation);
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        return response;
    }

    private Reactivation createPendingReactivation() {
        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(email);
        reactivation.setCode("code");
        reactivation.setReactivationStatus(PENDING);
        reactivation.setRequestedAt(now(clock));
        return reactivation;
    }
}
