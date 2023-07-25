package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import uk.gov.cabinetoffice.csl.util.TextEncryptionUtils;

import java.io.IOException;
import java.net.URLEncoder;

@Configuration
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private String encryptionKey;

    public CustomAuthenticationFailureHandler(@Value("${textEncryption.encryptionKey}") String encryptionKey){
        this.encryptionKey = encryptionKey;
    }

    @SneakyThrows
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String exceptionMessage = exception.getMessage();

        switch (exceptionMessage) {
            case ("User account is locked"):
                response.sendRedirect("/login?error=locked");
                break;
            case ("User account is blocked"):
                response.sendRedirect("/login?error=blocked");
                break;
            case ("User account is deactivated"):
                String username = request.getParameter("username");
                String encryptedUsername = TextEncryptionUtils.getEncryptedText(username, encryptionKey);
                response.sendRedirect("/login?error=deactivated&username=" + URLEncoder.encode(encryptedUsername, "UTF-8"));
                break;
            case("Pending reactivation already exists for user"):
                response.sendRedirect("/login?error=pending-reactivation");
                break;
            case("Reactivation request has expired"):
                response.sendRedirect("/login?error=deactivated-expired&username=" + TextEncryptionUtils.getEncryptedText(request.getParameter("username"), encryptionKey));
                break;
            default:
                response.sendRedirect("/login?error=failed");
        }
    }
}
