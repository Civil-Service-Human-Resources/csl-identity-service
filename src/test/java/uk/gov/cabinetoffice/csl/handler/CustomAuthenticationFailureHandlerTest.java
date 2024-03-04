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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;

@SpringBootTest
@ActiveProfiles("no-redis")
public class CustomAuthenticationFailureHandlerTest {

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @MockBean
    private ReactivationService reactivationService;

    @Test
    public void shouldSetErrorToFailedOnFailedLogin() throws Exception {
        HttpServletResponse response = executeHandler("Some other error");
        verify(response).sendRedirect("/login?error=failed&maxLoginAttempts=5");
    }

    @Test
    public void shouldSetErrorToLockedOnAccountLock() throws Exception {
        HttpServletResponse response = executeHandler("User account is locked");
        verify(response).sendRedirect("/login?error=locked&maxLoginAttempts=5");
    }

    @Test
    public void shouldSetErrorToFailedOnAccountBlocked() throws Exception {
        HttpServletResponse response = executeHandler("User account is blocked");
        verify(response).sendRedirect("/login?error=blocked");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivatedAndPendingReactivationExists()
            throws Exception {
        HttpServletResponse response = executeHandler("Pending reactivation already exists for user");
        verify(response).sendRedirect("/login?error=pending-reactivation&pendingReactivationMessage=" +
                "We've already sent you an email on Thu Feb 01 00:00:00 GMT 2024 with a link to reactivate your account. " +
                "Please check your emails (including the junk/spam folder)");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivated() throws Exception {
        HttpServletResponse response = executeHandler("User account is deactivated");
        String encryptedUsername = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";
        verify(response).sendRedirect("/login?error=deactivated" +
                "&reactivationValidityMessage=You have 24 hours to click the reactivation link within the email." +
                "&username=" + encode(encryptedUsername, UTF_8));
    }

    private HttpServletResponse executeHandler(String message) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn(message);
        String username = "learner@domain.com";
        when(request.getParameter("username")).thenReturn(username);
        Reactivation pendingReactivation = createPendingReactivation(username);
        when(reactivationService.getPendingReactivationForEmail(username)).thenReturn(pendingReactivation);
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        return response;
    }

    private Reactivation createPendingReactivation(String email) throws Exception {
        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(email);
        reactivation.setCode("code");
        reactivation.setReactivationStatus(PENDING);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("01-Feb-2024");
        reactivation.setRequestedAt(dateOfReactivationRequest);
        return reactivation;
    }
}
