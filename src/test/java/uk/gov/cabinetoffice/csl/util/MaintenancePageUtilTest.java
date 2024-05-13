package uk.gov.cabinetoffice.csl.util;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class MaintenancePageUtilTest {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";
    private final String skipMaintenancePageForUsers = "tester1@domain.com,tester2@domain.com";
    private final String skipMaintenancePageForUris = "/health,/info,/maintenance,/error,/cookies,/privacy," +
            "/accessibility-statement,/contact-us,/webjars,/assets,/css,/img,/favicon.ico";

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsDisabled() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(false,
                skipMaintenancePageForUsers, skipMaintenancePageForUris);
        try {
            maintenancePageUtil.skipMaintenancePageCheck("tester1@domain.com");
        } catch (Exception e) {
            fail("No exception is thrown");
        }
    }

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, skipMaintenancePageForUris);
        try {
            maintenancePageUtil.skipMaintenancePageCheck("tester1@domain.com");
        } catch (GenericServerException e) {
            fail("GenericServerException should not be thrown here.");
        }
    }

    @Test
    public void shouldNotSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsNotAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, skipMaintenancePageForUris);

        GenericServerException thrown = assertThrows(GenericServerException.class, () ->
                maintenancePageUtil.skipMaintenancePageCheck("tester3@domain.com"),
                "Expected skipMaintenancePageCheck() to throw GenericServerException, but it didn't");
        assertTrue(thrown.getMessage().contains("User is not allowed to access the website due to maintenance page is enabled."));
    }
}
