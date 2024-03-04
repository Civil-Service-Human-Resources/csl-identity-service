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

import java.time.LocalDateTime;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
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
        String reactivationValidityMessage = utils.validityMessage(
        "You have %s to click the reactivation link within the email.", reactivationValidityInSeconds);
        String username = request.getParameter("username");
        String encryptedUsername = getEncryptedText(username, encryptionKey);
        String encodedUsername = encode(encryptedUsername, UTF_8);
        String redirect = "/login?error=failed&maxLoginAttempts=" + maxLoginAttempts;
        switch (exception.getMessage()) {
            case ("User account is locked") -> redirect = "/login?error=locked&maxLoginAttempts=" + maxLoginAttempts;
            case ("User account is blocked") -> redirect = "/login?error=blocked";
            case ("User account is deactivated") -> redirect = "/login?error=deactivated&reactivationValidityMessage="
                    + reactivationValidityMessage + "&username=" + encodedUsername;
            case ("Reactivation request has expired") -> redirect = "/login?error=deactivated-expired&" +
                    "reactivationValidityMessage=" + reactivationValidityMessage + "&username=" + encodedUsername;
            case ("Pending reactivation already exists for user") -> {
                Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(username);
                LocalDateTime requestedAt = pendingReactivation.getRequestedAt();
                String pendingReactivationMessage = "We've already sent you an email on " + requestedAt +
                " with a link to reactivate your account. Please check your emails (including the junk/spam folder).";
                redirect = "/login?error=pending-reactivation&pendingReactivationMessage=" + pendingReactivationMessage;
            }
        }
        response.sendRedirect(redirect);
    }
}
