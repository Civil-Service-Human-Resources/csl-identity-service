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

    @Value("${maintenancePage.enabled}")
    private boolean maintenancePageEnabled;

    @Value("${maintenancePage.contentLine1}")
    private String maintenancePageContentLine1;

    @Value("${maintenancePage.contentLine2}")
    private String maintenancePageContentLine2;

    @Value("${maintenancePage.contentLine3}")
    private String maintenancePageContentLine3;

    @Value("${maintenancePage.contentLine4}")
    private String maintenancePageContentLine4;

    @Value("${maintenancePage.skipForUsers}")
    private String skipMaintenancePageForUsers;

    public boolean displayMaintenancePage(HttpServletRequest request, Model model) {
        boolean displayMaintenancePage = false;

        if(maintenancePageEnabled) {
            displayMaintenancePage = true;

            model.addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
            model.addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
            model.addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
            model.addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);

            String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
            boolean skipMaintenancePage = isNotBlank(username) &&
                    Arrays.stream(skipMaintenancePageForUsers.split(","))
                            .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
            if (skipMaintenancePage) {
                displayMaintenancePage = false;
                log.info("Maintenance page is skipped for the user: {}", username);
            }
        }
        return displayMaintenancePage;
    }

    public void skipMaintenancePageCheck(String email) {
        if (maintenancePageEnabled) {
            boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                    .anyMatch(u -> u.trim().equalsIgnoreCase(email.trim()));
            if(skipMaintenancePage) {
                log.info("Maintenance page is skipped for the user: {}", email);
            } else {
                log.warn("User is not allowed to access the website due to maintenance page is enabled. Showing error page for the user: {}", email);
                throw new GenericServerException("User is not allowed to access the website due to maintenance page is enabled.");
            }
        }
    }
}
