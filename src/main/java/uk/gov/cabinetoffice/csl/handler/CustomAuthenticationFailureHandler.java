package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.nio.charset.StandardCharsets;

import static java.net.URLEncoder.encode;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@Configuration
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${textEncryption.encryptionKey}")
    private String encryptionKey;

    @SneakyThrows
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) {
        String exceptionMessage = exception.getMessage();
        switch (exceptionMessage) {
            case ("User account is locked") -> response.sendRedirect("/login?error=locked");
            case ("User account is blocked") -> response.sendRedirect("/login?error=blocked");
            case ("User account is deactivated") -> {
                String username = request.getParameter("username");
                String encryptedUsername = getEncryptedText(username, encryptionKey);
                response.sendRedirect("/login?error=deactivated&username=" +
                        encode(encryptedUsername, StandardCharsets.UTF_8));
            }
            case ("Pending reactivation already exists for user") ->
                    response.sendRedirect("/login?error=pending-reactivation");
            case ("Reactivation request has expired") ->
                    response.sendRedirect("/login?error=deactivated-expired&username=" +
                            getEncryptedText(request.getParameter("username"), encryptionKey));
            default -> {
                //TODO:
                // Call UserAuthService.getIdentity() and debug to see if it returns Identity
                // Increase number of failed attempt in the identity DB table
                // If number of failed attempt reached the threshold then show the locked error message to the user
                // response.sendRedirect("/login?error=locked");
                // else show failed login message
                response.sendRedirect("/login?error=failed");
            }
        }
    }
}
