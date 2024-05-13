package uk.gov.cabinetoffice.csl.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import java.util.Arrays;

@Slf4j
@Component
public class MaintenancePageUtil {

    private final boolean maintenancePageEnabled;

    private final String skipMaintenancePageForUsers;

    public MaintenancePageUtil(@Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
                               @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers) {
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
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
}
