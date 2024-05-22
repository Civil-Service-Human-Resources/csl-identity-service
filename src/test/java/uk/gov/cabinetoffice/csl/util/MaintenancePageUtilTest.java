package uk.gov.cabinetoffice.csl.util;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("no-redis")
public class MaintenancePageUtilTest {

    private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

    @Mock
    private IUserAuthService userAuthService;

    @Mock
    private HttpServletRequest request;

    private MaintenancePageUtil createMaintenancePageUtil(boolean maintenancePageEnabled) {
        String skipMaintenancePageForUsers = "tester1@domain.com,tester2@domain.com";
        String skipMaintenancePageForUris = "/health,/info,/maintenance,/error,/logout," +
                "/cookies,/privacy,/accessibility-statement,/contact-us," +
                "/webjars,/css,/img,/js,/favicon.ico," +
                "/oauth2/token,/oauth2/introspect,/oauth2/revoke,/identity/resolve," +
                "/reset-cache,/api/identities," +
                "/signup/chooseOrganisation,/signup/enterToken," +
                "/account/verify/agency,/account/reactivate/updated,/account/email/updated";
        return new MaintenancePageUtil(userAuthService, maintenancePageEnabled,
                skipMaintenancePageForUsers, skipMaintenancePageForUris);
    }

    private boolean executeSkipMaintenancePageForUser(boolean maintenancePageEnabled,
                                                      String httpMethod, String username) {
        when(request.getMethod()).thenReturn(httpMethod);
        when(request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME)).thenReturn(username);
        return createMaintenancePageUtil(maintenancePageEnabled).skipMaintenancePageForUser(request);
    }

    private boolean executeShouldNotApplyMaintenancePageFilterForURI(boolean maintenancePageEnabled, String requestUri) {
        when(request.getRequestURI()).thenReturn(requestUri);
        return createMaintenancePageUtil(maintenancePageEnabled).shouldNotApplyMaintenancePageFilterForURI(request);
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsDisabled() {
        assertTrue(executeSkipMaintenancePageForUser(false, "GET", null));
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsEnabledAndHttpMethodIsOtherThanGET() {
        assertTrue(executeSkipMaintenancePageForUser(true, "POST", null));
    }

    @Test
    public void shouldNotSkipMaintenancePageIfMaintenancePageIsEnabledAndHttpMethodIsGETAndUsernameIsPassedInRequestParamIsNotAllowedToSkipMaintenancePage() {
        assertFalse(executeSkipMaintenancePageForUser(true, "GET", "tester3@domain.com"));
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsEnabledAndHttpMethodIsGETAndUsernameIsPassedInRequestParamIsAllowedToSkipMaintenancePage() {
        assertTrue(executeSkipMaintenancePageForUser(true, "GET", "tester1@domain.com"));
    }

    @Test
    public void shouldNotApplyMaintenancePageFilterForURIfMaintenancePageIsDisabled() {
        assertTrue(executeShouldNotApplyMaintenancePageFilterForURI(false, "/health"));
    }

    @Test
    public void shouldSkipMaintenancePageIfMaintenancePageIsEnabledAndRequestURIIsAllowedToSkipMaintenancePage() {
        assertTrue(executeShouldNotApplyMaintenancePageFilterForURI(true, "/assets/css/main.css"));
    }

    @Test
    public void shouldNotSkipMaintenancePageIfMaintenancePageIsEnabledAndRequestURIIsNotAllowedToSkipMaintenancePage() {
        assertFalse(executeShouldNotApplyMaintenancePageFilterForURI(true, "/create"));
    }
}
