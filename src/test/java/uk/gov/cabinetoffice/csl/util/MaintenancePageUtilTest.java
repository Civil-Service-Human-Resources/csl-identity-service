package uk.gov.cabinetoffice.csl.util;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.Model;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class MaintenancePageUtilTest {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";
    private final String skipMaintenancePageForUsers = "tester1@domain.com,tester2@domain.com";
    private final String maintenancePageContentLine1 = "The learning website is undergoing scheduled maintenance.";
    private final String maintenancePageContentLine2 = "It will be unavailable between the hours of 6pm to 8pm on Friday 10th May 2024.";
    private final String maintenancePageContentLine3 = "Apologies for the inconvenience.";
    private final String maintenancePageContentLine4 = "If the maintenance period is extended, further information will be provided here.";

    @Mock
    private Model model;

    @Mock
    private HttpServletRequest request;

    @Test
    public void shouldNotDisplayMaintenancePageIfMaintenancePageIsDisabled() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(false,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        assertFalse(maintenancePageUtil.displayMaintenancePage(request, model));
        verify(request, times(1)).getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        verify(model, times(0)).addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
        verify(model, times(0)).addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
        verify(model, times(0)).addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
        verify(model, times(0)).addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
    }

    @Test
    public void shouldDisplayMaintenancePageIfMaintenancePageIsEnabledAndUsernameIsNotPassedInRequestParam() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        when(request.getParameter("username")).thenReturn(null);

        assertTrue(maintenancePageUtil.displayMaintenancePage(request, model));
        verify(request, times(1)).getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        verify(model, times(1)).addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
        verify(model, times(1)).addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
        verify(model, times(1)).addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
        verify(model, times(1)).addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
    }

    @Test
    public void shouldDisplayMaintenancePageIfMaintenancePageIsEnabledAndUsernamePassedInRequestParamIsNotAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        when(request.getParameter("username")).thenReturn("tester3@domain.com");

        assertTrue(maintenancePageUtil.displayMaintenancePage(request, model));
        verify(request, times(1)).getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        verify(model, times(1)).addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
        verify(model, times(1)).addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
        verify(model, times(1)).addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
        verify(model, times(1)).addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
    }

    @Test
    public void shouldNotDisplayMaintenancePageIfMaintenancePageIsEnabledAndUsernamePassedInRequestParamIsAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        when(request.getParameter("username")).thenReturn("tester1@domain.com");

        assertFalse(maintenancePageUtil.displayMaintenancePage(request, model));
        verify(request, times(1)).getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        verify(model, times(0)).addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
        verify(model, times(0)).addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
        verify(model, times(0)).addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
        verify(model, times(0)).addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
    }

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsDisabled() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(false,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);
        try {
            maintenancePageUtil.skipMaintenancePageCheck("tester1@domain.com");
        } catch (Exception e) {
            fail("No exception is thrown");
        }
    }

    @Test
    public void shouldSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        try {
            maintenancePageUtil.skipMaintenancePageCheck("tester1@domain.com");
        } catch (GenericServerException e) {
            fail("GenericServerException should not be thrown here.");
        }
    }

    @Test
    public void shouldNotSkipMaintenancePageOnAuthenticationIfMaintenancePageIsEnabledAndUserIsNotAllowedToSkipMaintenancePage() {
        MaintenancePageUtil maintenancePageUtil = new MaintenancePageUtil(true,
                skipMaintenancePageForUsers, maintenancePageContentLine1, maintenancePageContentLine2,
                maintenancePageContentLine3,maintenancePageContentLine4);

        GenericServerException thrown = assertThrows(GenericServerException.class, () ->
                maintenancePageUtil.skipMaintenancePageCheck("tester3@domain.com"),
                "Expected skipMaintenancePageCheck() to throw GenericServerException, but it didn't");
        assertTrue(thrown.getMessage().contains("User is not allowed to access the website due to maintenance page is enabled."));
    }
}
