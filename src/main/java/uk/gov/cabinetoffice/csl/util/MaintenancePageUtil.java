package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
public class MaintenancePageUtil {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    private final boolean maintenancePageEnabled;

    private final String skipMaintenancePageForUsers;

    private final String maintenancePageContentLine1;

    private final String maintenancePageContentLine2;

    private final String maintenancePageContentLine3;

    private final String maintenancePageContentLine4;

    public MaintenancePageUtil(@Value("${maintenancePage.enabled}") boolean maintenancePageEnabled,
                               @Value("${maintenancePage.skipForUsers}") String skipMaintenancePageForUsers,
                               @Value("${maintenancePage.contentLine1}") String maintenancePageContentLine1,
                               @Value("${maintenancePage.contentLine2}") String maintenancePageContentLine2,
                               @Value("${maintenancePage.contentLine3}") String maintenancePageContentLine3,
                               @Value("${maintenancePage.contentLine4}") String maintenancePageContentLine4) {
        this.maintenancePageEnabled = maintenancePageEnabled;
        this.skipMaintenancePageForUsers = skipMaintenancePageForUsers;
        this.maintenancePageContentLine1 = maintenancePageContentLine1;
        this.maintenancePageContentLine2 = maintenancePageContentLine2;
        this.maintenancePageContentLine3 = maintenancePageContentLine3;
        this.maintenancePageContentLine4 = maintenancePageContentLine4;
    }

    public boolean displayMaintenancePage(HttpServletRequest request, Model model) {
        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        return displayMaintenancePageForUser(username, model);
    }

    public boolean displayMaintenancePageForUser(String username, Model model) {
        if(maintenancePageEnabled) {
            boolean skipMaintenancePage = isNotBlank(username) &&
                    Arrays.stream(skipMaintenancePageForUsers.split(","))
                            .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
            if (skipMaintenancePage) {
                log.info("MaintenancePageUtil.displayMaintenancePageForUser:Maintenance page is skipped for the user: {}", username);
                return false;
            }
            model.addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
            model.addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
            model.addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
            model.addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
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
}
