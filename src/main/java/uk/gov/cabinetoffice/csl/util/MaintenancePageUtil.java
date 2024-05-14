package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import java.security.Principal;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Component
public class MaintenancePageUtil {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    private final boolean maintenancePageEnabled;

    private final String skipMaintenancePageForUsers;

    private final String skipMaintenancePageForUris;

    private final IUserAuthService userAuthService;

    public MaintenancePageUtil(
            @Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
            @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers,
            @Value("${maintenancePage.skipForUris}") String skipMaintenancePageForUris,
            IUserAuthService userAuthService) {
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
        this.skipMaintenancePageForUris = skipMaintenancePageForUris;
        this.userAuthService = userAuthService;
    }

    public boolean skipMaintenancePageForUser(HttpServletRequest request) {

        if(!maintenancePageEnabled) {
            return true;
        }

        String method = request.getMethod();
        if(!"GET".equalsIgnoreCase(method)) {
            log.info("MaintenancePageUtil.skipMaintenancePageForUser.method is not GET returning true.");
            return true;
        }

        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);

        if(isBlank(username)) {
            Principal principal = request.getUserPrincipal();
            if (principal instanceof Jwt jwt) {
                username = jwt.getClaim("email");
                log.info("MaintenancePageUtil.skipMaintenancePageForUser.username from request user principal: {}",
                        username);
            }
        }

        if(isBlank(username)) {
            try {
                username = userAuthService.getUsername();
                log.info("MaintenancePageUtil.skipMaintenancePageForUser.username from userAuthService: {}", username);
            } catch (Exception e) {
                log.info("MaintenancePageUtil.skipMaintenancePageForUser.username is not available. No action to be taken.");
            }
        }

        boolean skipMaintenancePageForUser = false;

        if(isNotBlank(username)) {
            final String trimmedUsername = username.trim();
            log.info("MaintenancePageUtil.skipMaintenancePageForUser.trimmedUsername: {}", trimmedUsername);

            skipMaintenancePageForUser = Arrays.stream(skipMaintenancePageForUsers.split(","))
                            .anyMatch(u -> u.trim().equalsIgnoreCase(trimmedUsername));

            log.info("MaintenancePageUtil.skipMaintenancePageForUser.returning skipMaintenancePageForUser: {}",
                    skipMaintenancePageForUser);

            if(skipMaintenancePageForUser) {
                log.info("MaintenancePageUtil.skipMaintenancePageForUser.Maintenance page is skipped for the user: {}",
                        username);
            }
        }

        return skipMaintenancePageForUser;
    }

    public void skipMaintenancePageCheck(String email) {
        if (maintenancePageEnabled) {
            boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                    .anyMatch(u -> u.trim().equalsIgnoreCase(email.trim()));
            log.info("MaintenancePageUtil.skipMaintenancePageCheck.skipMaintenancePage: {}", skipMaintenancePage);
            if(!skipMaintenancePage) {
                log.warn("MaintenancePageUtil.skipMaintenancePageCheck." +
                        "User is not allowed to access the website due to maintenance page is enabled. " +
                        "Showing error page to the user: {}", email);
                throw new GenericServerException("User is not allowed to access the website due to maintenance page is enabled.");
            }
        }
    }

    public boolean shouldNotApplyMaintenancePageFilterForURI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        boolean shouldNotApplyMaintenancePageFilterForURI = isNotBlank(requestURI)
                && Arrays.stream(skipMaintenancePageForUris.split(","))
                .anyMatch(u -> u.trim().equalsIgnoreCase(requestURI.trim()));
        log.info("MaintenancePageUtil.shouldNotApplyMaintenancePageFilterForURI." +
                        "shouldNotApplyMaintenancePageFilterForURI is: {} for requestURI: {}",
                shouldNotApplyMaintenancePageFilterForURI, requestURI);
        return shouldNotApplyMaintenancePageFilterForURI;
    }
}
