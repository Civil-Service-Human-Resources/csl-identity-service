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

    public MaintenancePageUtil(@Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
                               @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers,
                               @Value("${maintenancePage.skipForUris}") String skipMaintenancePageForUris) {
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
        this.skipMaintenancePageForUris = skipMaintenancePageForUris;
    }

    public boolean skipMaintenancePageForUser(HttpServletRequest request) {
        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        log.info("MaintenancePageFilter.doFilterInternal: username request param: {}", username);
        String method = request.getMethod();
        log.info("MaintenancePageFilter.doFilterInternal: method: {}", method);

        if(!maintenancePageEnabled) {
            return true;
        }

        if(!"GET".equalsIgnoreCase(method)) {
            return true;
        }

        boolean skipMaintenancePage = isNotBlank(username) &&
                Arrays.stream(skipMaintenancePageForUsers.split(","))
                        .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
        if(skipMaintenancePage) {
            log.info("MaintenancePageFilter.doFilterInternal: Maintenance page is skipped for the user: {}", username);
            return true;
        }

        return false;
    }

    public void skipMaintenancePageCheck(String email) {
        if (maintenancePageEnabled) {
            boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                    .anyMatch(u -> u.trim().equalsIgnoreCase(email.trim()));
            if(skipMaintenancePage) {
                log.info("MaintenancePageUtil.skipMaintenancePageCheck:Maintenance page is skipped for the user: {}", email);
            } else {
                log.warn("MaintenancePageUtil.skipMaintenancePageCheck:User is not allowed to access the website due to maintenance page is enabled. Showing error page for the user: {}", email);
                throw new GenericServerException("User is not allowed to access the website due to maintenance page is enabled.");
            }
        }
    }

    public boolean shouldNotApplyFilterForURI(HttpServletRequest request) {
        log.info("MaintenancePageFilter.shouldNotFilter: servletPath: {}", request.getServletPath());
        String requestURI = request.getRequestURI();
        log.info("MaintenancePageFilter.shouldNotFilter: requestURI: {}", requestURI);
        return Arrays.stream(skipMaintenancePageForUris.split(","))
                .anyMatch(u -> u.trim().equalsIgnoreCase(requestURI.trim()));
    }
}
