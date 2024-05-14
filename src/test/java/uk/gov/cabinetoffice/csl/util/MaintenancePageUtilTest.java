package uk.gov.cabinetoffice.csl.util;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("no-redis")
public class MaintenancePageUtilTest {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    @Mock
    private HttpServletRequest request;

    private MaintenancePageUtil createMaintenancePageUtil(boolean maintenancePageEnabled) {
        String skipMaintenancePageForUsers = "tester1@domain.com,tester2@domain.com";
        String skipMaintenancePageForUris = "/health,/info,/maintenance,/error,/cookies,/privacy," +
                "/accessibility-statement,/contact-us,/webjars,/assets,/css,/img,/favicon.ico";
        return new MaintenancePageUtil(maintenancePageEnabled,
                skipMaintenancePageForUsers, skipMaintenancePageForUris);
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsDisabled() {
        assertTrue(createMaintenancePageUtil(false)
                .skipMaintenancePageForUser(request));
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsEnabledAndHttpMethodIsOtherThanGET() {
        when(request.getMethod()).thenReturn("POST");
        assertTrue(createMaintenancePageUtil(true)
                .skipMaintenancePageForUser(request));
    }

    @Test
    public void shouldNotSkipMaintenancePageIfMaintenancePageIsEnabledAndHttpMethodIsGETAndUsernameIsNotPassedInRequestParam() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME)).thenReturn(null);
        assertFalse(createMaintenancePageUtil(true)
                .skipMaintenancePageForUser(request));
    }

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsDisabled() {
        try {
            createMaintenancePageUtil(false)
                    .skipMaintenancePageCheck("tester1@domain.com");
        } catch (Exception e) {
            fail("No exception is thrown");
        }
    }

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsAllowedToSkipMaintenancePage() {
        try {
            createMaintenancePageUtil(true)
                    .skipMaintenancePageCheck("tester1@domain.com");
        } catch (GenericServerException e) {
            fail("GenericServerException should not be thrown here.");
        }
    }

    @Test
    public void shouldNotSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsNotAllowedToSkipMaintenancePage() {
        GenericServerException thrown = assertThrows(GenericServerException.class, () ->
                createMaintenancePageUtil(true)
                        .skipMaintenancePageCheck("tester3@domain.com"),
                "Expected skipMaintenancePageCheck() to throw GenericServerException, but it didn't");
        assertTrue(thrown.getMessage().contains("User is not allowed to access the website due to maintenance page is enabled."));
    }
}
