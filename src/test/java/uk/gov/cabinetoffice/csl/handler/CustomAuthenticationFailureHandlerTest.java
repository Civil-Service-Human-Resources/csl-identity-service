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

    private final String encryptedEmail = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @MockBean
    private ReactivationService reactivationService;

    @Autowired
    private Clock clock;

    @Test
    public void shouldSetErrorToErrorOnSystemError() throws IOException {
        HttpServletResponse response = executeHandler("System error");
        verify(response).sendRedirect("/error");
    }

    @Test
    public void shouldSetErrorToFailedOnFailedLogin() throws IOException {
        HttpServletResponse response = executeHandler("Some other error");
        verify(response).sendRedirect("/login?error=failed&maxLoginAttempts=" + maxLoginAttempts);
    }

    @Test
    public void shouldSetErrorToLockedOnAccountLock() throws IOException {
        HttpServletResponse response = executeHandler("User account is locked");
        verify(response).sendRedirect("/login?error=locked&maxLoginAttempts=" + maxLoginAttempts);
    }

    @Test
    public void shouldSetErrorToBlockedOnAccountBlocked() throws IOException {
        HttpServletResponse response = executeHandler("User account is blocked");
        verify(response).sendRedirect("/login?error=blocked");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivation() throws IOException {
        HttpServletResponse response = executeHandler("User account is deactivated");
        verify(response).sendRedirect("/login?error=deactivated&username="
                + encode(encryptedEmail, UTF_8));
    }

    @Test
    public void shouldSetErrorToReactivatedExpiredOnAccountReactivationExpired() throws IOException {
        HttpServletResponse response = executeHandler("Reactivation request has expired");
        verify(response).sendRedirect("/login?error=reactivation-expired&username="
                + encode(encryptedEmail, UTF_8));
    }

    @Test
    public void shouldSetErrorToPendingReactivationOnAccountDeactivatedAndPendingReactivationExists()
            throws Exception {
        when(reactivationService.getPendingReactivationForEmail(email)).thenReturn(createPendingReactivation());
        HttpServletResponse response = executeHandler("Pending reactivation exists for user");
        verify(response).sendRedirect("/login?error=pending-reactivation");
    }

    private HttpServletResponse executeHandler(String message) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn(message);
        when(request.getParameter("username")).thenReturn(email);
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
