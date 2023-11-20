package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class CustomAuthenticationFailureHandlerTest {

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Test
    public void shouldSetErrorToFailedOnFailedLogin() throws IOException, ServletException {
        HttpServletResponse response = executeHandler("Some other error");
        verify(response).sendRedirect("/login?error=failed");
    }

    @Test
    public void shouldSetErrorToLockedOnAccountLock() throws IOException, ServletException {
        HttpServletResponse response = executeHandler("User account is locked");
        verify(response).sendRedirect("/login?error=locked");
    }

    @Test
    public void shouldSetErrorToFailedOnAccountBlocked() throws IOException, ServletException {
        HttpServletResponse response = executeHandler("User account is blocked");
        verify(response).sendRedirect("/login?error=blocked");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivatedAndPendingReactivationExists()
            throws IOException, ServletException {
        HttpServletResponse response = executeHandler("Pending reactivation already exists for user");
        verify(response).sendRedirect("/login?error=pending-reactivation");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivated() throws IOException, ServletException {
        HttpServletResponse response = executeHandler("User account is deactivated");
        String encryptedUsername = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";
        verify(response).sendRedirect("/login?error=deactivated&username=" + encode(encryptedUsername, UTF_8));
    }

    private HttpServletResponse executeHandler(String message) throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn(message);
        String username = "learner@domain.com";
        when(request.getParameter("username")).thenReturn(username);
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        return response;
    }
}
