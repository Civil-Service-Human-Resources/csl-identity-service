package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
public class MaintenancePageUtil {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    private final boolean maintenancePageEnabled;

    private final String skipMaintenancePageForUsers;

    private final String skipMaintenancePageForUris;

    public MaintenancePageUtil(
            @Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
            @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers,
            @Value("${maintenancePage.skipForUris}") String skipMaintenancePageForUris) {
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
        this.skipMaintenancePageForUris = skipMaintenancePageForUris;
    }

    public boolean skipMaintenancePageForUser(HttpServletRequest request) {
        if(!maintenancePageEnabled) {
            return true;
        }

        String method = request.getMethod();
        if(!"GET".equalsIgnoreCase(method)) {
            return true;
        }

        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        boolean skipMaintenancePageForUser = isNotBlank(username) &&
                Arrays.stream(skipMaintenancePageForUsers.split(","))
                        .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));

        if(skipMaintenancePageForUser) {
            log.info("Maintenance page is skipped for the user: {}", username);
        }

        return skipMaintenancePageForUser;
    }

    public void skipMaintenancePageCheck(String email) {
        if (maintenancePageEnabled) {
            boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                    .anyMatch(u -> u.trim().equalsIgnoreCase(email.trim()));
            if(!skipMaintenancePage) {
                log.warn("User is not allowed to access the website due to maintenance page is enabled. " +
                        "Showing error page to the user: {}", email);
                throw new GenericServerException("User is not allowed to access the website due to maintenance page is enabled.");
            }
        }
    }

    public boolean shouldNotApplyMaintenancePageFilterForURI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return isNotBlank(requestURI) && Arrays.stream(skipMaintenancePageForUris.split(","))
                .anyMatch(u -> u.trim().equalsIgnoreCase(requestURI.trim()));
    }
}
