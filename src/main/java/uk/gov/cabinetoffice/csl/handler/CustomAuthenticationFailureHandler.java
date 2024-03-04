package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static java.net.URLEncoder.encode;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@Configuration
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${textEncryption.encryptionKey}")
    private String encryptionKey;

    @Value("${account.lockout.maxLoginAttempts}")
    private int maxLoginAttempts;

    @Value("${reactivation.validityInSeconds}")
    private int reactivationValidityInSeconds;

    private final ReactivationService reactivationService;

    private final Utils utils;

    public CustomAuthenticationFailureHandler(ReactivationService reactivationService, Utils utils) {
        this.reactivationService = reactivationService;
        this.utils = utils;
    }

    @SneakyThrows
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) {
        String reactivationValidityMessage =
                utils.validityMessage("You have %s to click the reactivation link within the email.",
                        reactivationValidityInSeconds);
        String exceptionMessage = exception.getMessage();
        switch (exceptionMessage) {
            case ("User account is locked") -> response.sendRedirect("/login?error=locked&maxLoginAttempts=" +
                    maxLoginAttempts);
            case ("User account is blocked") -> response.sendRedirect("/login?error=blocked");
            case ("User account is deactivated") -> {
                String username = request.getParameter("username");
                String encryptedUsername = getEncryptedText(username, encryptionKey);
                response.sendRedirect("/login?error=deactivated" +
                        "&reactivationValidityMessage=" + reactivationValidityMessage +
                        "&username=" + encode(encryptedUsername, StandardCharsets.UTF_8));
            }
            case ("Pending reactivation already exists for user") -> {
                String username = request.getParameter("username");
                Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(username);
                LocalDateTime requestedAt = pendingReactivation.getRequestedAt();
                String pendingReactivationMessage = "We've already sent you an email on " + requestedAt +
                " with a link to reactivate your account. Please check your emails (including the junk/spam folder).";
                response.sendRedirect("/login?error=pending-reactivation&pendingReactivationMessage="
                        + pendingReactivationMessage);
            }
            case ("Reactivation request has expired") -> {
                String username = request.getParameter("username");
                String encryptedUsername = getEncryptedText(username, encryptionKey);
                response.sendRedirect("/login?error=deactivated-expired" +
                            "&reactivationValidityMessage=" + reactivationValidityMessage +
                            "&username=" + encode(encryptedUsername, StandardCharsets.UTF_8));
            }
            default -> response.sendRedirect("/login?error=failed&maxLoginAttempts=" + maxLoginAttempts);
        }
    }
}
