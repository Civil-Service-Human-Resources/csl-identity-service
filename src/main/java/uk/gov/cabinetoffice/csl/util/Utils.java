package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
public class Utils {

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

    public String convertSecondsIntoDaysHoursMinutesSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long days = 0;
        if (hours > 24) {
            days = hours / 24;
            hours = hours - days * 24;
        }
        String result = "";
        if (days != 0) {
            result = result + String.format("%02d", days) + " days ";
        }
        if (hours != 0) {
            result = result + String.format("%02d", hours) + " hours ";
        }
        if (minutes != 0) {
            result = result + String.format("%02d", minutes) + " minutes ";
        }
        if (seconds != 0) {
            result = result + String.format("%02d", seconds) + " seconds";
        }
        return StringUtils.removeEnd(result, " ");
    }

    public String convertDateTimeFormat(String localDateTime) {
        try {
            SimpleDateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat targetDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            Date parsedDate = sourceDateFormat.parse(localDateTime);
            return targetDateFormat.format(parsedDate);
        } catch (Exception e) {
            log.warn("Invalid date string value: {}, Exception: {}", localDateTime, e.toString());
        }
        return "";
    }

    public String convertDateTimeFormat(LocalDateTime localDateTime) {
        return localDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1).toLowerCase(Locale.ROOT);
    }
}
