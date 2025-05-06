package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.service.ReactivationService;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.Clock;
import java.time.LocalDateTime;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
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

    @Value("${reactivation.durationAfterReactivationAllowedInSeconds}")
    private long durationAfterReactivationAllowedInSeconds;

    private final ReactivationService reactivationService;

    private final Utils utils;

    private final Clock clock;

    public CustomAuthenticationFailureHandler(ReactivationService reactivationService, Utils utils, Clock clock) {
        this.reactivationService = reactivationService;
        this.utils = utils;
        this.clock = clock;
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
            case ("User account is blocked due to a missing token") ->
                    redirect = "/login?error=blocked-missing-token&username=" + encodedUsername;
            case ("User account is deactivated") -> redirect = "/login?error=deactivated&username=" + encodedUsername;
            case ("Reactivation request has expired") ->
                    redirect = "/login?error=reactivation-expired&username=" + encodedUsername;
            case ("Pending reactivation exists for user") -> {
                try {
                    Reactivation pendingReactivation = reactivationService.getPendingReactivationForEmail(username);
                    LocalDateTime requestedAt = pendingReactivation.getRequestedAt();
                    long durationInSecondsSinceReactivationRequested = SECONDS.between(requestedAt, now(clock));
                    if (durationInSecondsSinceReactivationRequested < durationAfterReactivationAllowedInSeconds) {
                        log.info("User with email {} is trying to reactivate before re-reactivate allowed time." +
                        "User is displayed the message for the pending reactivation.", pendingReactivation.getEmail());
                        String requestedAtStr = utils.convertDateTimeFormat(requestedAt);
                        LocalDateTime reactivationLinkExpiry = requestedAt.plusSeconds(reactivationValidityInSeconds);
                        String reactivationExpiryStr = utils.convertDateTimeFormat(reactivationLinkExpiry);
                        log.info("Pending reactivation for the email {} requested at {} and expires on {}",
                                pendingReactivation.getEmail(), requestedAtStr, reactivationExpiryStr);
                        redirect = "/login?error=pending-reactivation";
                    } else {
                        redirect = "/login?error=deactivated&username=" + encodedUsername;
                    }
                } catch (Exception e) {
                    log.warn("Exception while retrieving pending reactivation for email: {}, Exception: {}", username,
                            e.toString());
                }
            }
        }
        log.info("CustomAuthenticationFailureHandler.onAuthenticationFailure.redirect: {}", redirect);
        response.sendRedirect(redirect);
    }
}
