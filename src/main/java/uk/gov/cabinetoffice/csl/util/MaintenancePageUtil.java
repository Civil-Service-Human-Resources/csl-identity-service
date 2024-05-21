package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import java.util.Arrays;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Component
public class MaintenancePageUtil {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    private final IUserAuthService userAuthService;

    private final boolean maintenancePageEnabled;

    private final String skipMaintenancePageForUsers;

    private final String skipMaintenancePageForUris;

    public MaintenancePageUtil(
            IUserAuthService userAuthService,
            @Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
            @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers,
            @Value("${maintenancePage.skipForUris}") String skipMaintenancePageForUris) {
        this.userAuthService = userAuthService;
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
        this.skipMaintenancePageForUris = skipMaintenancePageForUris;
    }

    public boolean skipMaintenancePageForUser(HttpServletRequest request) {
        if(!maintenancePageEnabled) {
            return true;
        }

        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        log.info("MaintenancePageUtil: username from request param: {}", username);

        if(isBlank(username)) {
            username = userAuthService.getUsername();
        }

        String requestURI = request.getRequestURI();

        if(isBlank(username)) {
            if("GET".equalsIgnoreCase(request.getMethod())) {
                log.info("MaintenancePageUtil: username is missing and HTTP Method is GET. " +
                        "Returning false for skipMaintenancePageForUser for requestURI {}", requestURI);
                return false;
            } else {
                log.info("MaintenancePageUtil: username is missing and HTTP Method is not GET. " +
                        "Returning true for skipMaintenancePageForUser for requestURI {}", requestURI);
                return true;
            }
        }

        final String trimmedUsername = username.trim();

        boolean skipMaintenancePageForUser = Arrays.stream(skipMaintenancePageForUsers.split(","))
                .anyMatch(u -> u.trim().equalsIgnoreCase(trimmedUsername));

        if(skipMaintenancePageForUser) {
            log.info("MaintenancePageUtil: Maintenance page is skipped for the username {} for requestURI {}",
                    username, requestURI);
        } else {
            log.info("MaintenancePageUtil: username {} is not allowed to skip the Maintenance page for requestURI {}",
                    username, requestURI);
        }

        return skipMaintenancePageForUser;
    }

    public boolean shouldNotApplyMaintenancePageFilterForURI(HttpServletRequest request) {
        if(!maintenancePageEnabled) {
            return true;
        }

        String requestURI = request.getRequestURI();
        boolean shouldNotApplyMaintenancePageFilterForURI = isNotBlank(requestURI)
                && Arrays.stream(skipMaintenancePageForUris.split(","))
                .anyMatch(u -> requestURI.trim().toLowerCase(ROOT)
                        .contains(u.toLowerCase(ROOT)));
        log.debug("MaintenancePageUtil: shouldNotApplyMaintenancePageFilterForURI is {} for requestURI {}",
                shouldNotApplyMaintenancePageFilterForURI, requestURI);
        return shouldNotApplyMaintenancePageFilterForURI;
    }
}
