package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
@SpringBootTest
public class CustomAuthenticationFailureHandlerTest {

    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Test
    public void shouldSetErrorToFailedOnFailedLogin() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("Some other error");
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        verify(response).sendRedirect("/login?error=failed");
    }

    @Test
    public void shouldSetErrorToLockedOnAccountLock() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("User account is locked");
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        verify(response).sendRedirect("/login?error=locked");
    }

    @Test
    public void shouldSetErrorToFailedOnAccountBlocked() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("User account is blocked");
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        verify(response).sendRedirect("/login?error=blocked");
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivated() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        String username = "learner@domain.com";
        String encryptedUsername = "W+tehauG4VaW9RRQXwc/8e1ETIr28UKG0eQYbPX2oLY=";
        when(exception.getMessage()).thenReturn("User account is deactivated");
        when(request.getParameter("username")).thenReturn(username);
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        verify(response).sendRedirect("/login?error=deactivated&username=" + URLEncoder.encode(encryptedUsername, UTF_8));
    }

    @Test
    public void shouldSetErrorToDeactivatedOnAccountDeactivatedAndPendingReactivationExists() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("Pending reactivation already exists for user");
        authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
        verify(response).sendRedirect("/login?error=pending-reactivation");
    }
}
