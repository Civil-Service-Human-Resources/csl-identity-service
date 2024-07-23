package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.LocalDateTime;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getEncryptedText;

@Slf4j
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
        String username = request.getParameter("username");
        String encryptedUsername = getEncryptedText(username, encryptionKey);
        String encodedUsername = encode(encryptedUsername, UTF_8);
        String redirect = "/login?error=failed&maxLoginAttempts=" + maxLoginAttempts;
        switch (exception.getMessage()) {
            case ("System error") -> redirect = "/error";
            case ("User account is locked") -> redirect = "/login?error=locked&maxLoginAttempts=" + maxLoginAttempts;
            case ("User account is blocked") -> redirect = "/login?error=blocked";
            case ("User account is deactivated") -> redirect = "/login?error=deactivated&username=" + encodedUsername;
            case ("Reactivation request has expired") -> redirect = "/login?error=reactivation-expired&username=" + encodedUsername;
            case ("Pending reactivation exists for user") -> {
                String redirectStr = "/login?error=pending-reactivation";
                try {
                    Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(username);
                    LocalDateTime requestedAt = pendingReactivation.getRequestedAt();
                    String requestedAtStr = utils.convertDateTimeFormat(requestedAt);
                    LocalDateTime reactivationLinkExpiry = requestedAt.plusSeconds(reactivationValidityInSeconds);
                    String reactivationExpiryStr = utils.convertDateTimeFormat(reactivationLinkExpiry);
                    redirectStr = redirectStr + "&requestedAt="+requestedAtStr+"&expiryAt="+reactivationExpiryStr;
                    log.info("Pending reactivation for the email {} requested at {} and expires on {}",
                            pendingReactivation.getEmail(), requestedAtStr, reactivationExpiryStr);
                } catch (Exception e) {
                    log.warn("Exception while retrieving pending reactivation for email: {}, Exception: {}", username, e.toString());
                }
                redirect = redirectStr;
            }
        }
        log.info("CustomAuthenticationFailureHandler.onAuthenticationFailure.redirect: {}", redirect);
        response.sendRedirect(redirect);
    }
}
