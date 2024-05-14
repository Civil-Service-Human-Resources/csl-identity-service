package uk.gov.cabinetoffice.csl.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import java.security.Principal;
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

        debug(request);

        String method = request.getMethod();
        if(!"GET".equalsIgnoreCase(method)) {
            log.info("skipMaintenancePageForUser.method is not GET returning true.");
            return true;
        }

        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        boolean skipMaintenancePageForUser = isNotBlank(username) &&
                Arrays.stream(skipMaintenancePageForUsers.split(","))
                        .anyMatch(u -> u.trim().equalsIgnoreCase(username.trim()));
        log.info("skipMaintenancePageForUser.returning skipMaintenancePageForUser: {}", skipMaintenancePageForUser);

        if(skipMaintenancePageForUser) {
            log.info("skipMaintenancePageForUser.Maintenance page is skipped for the user: {}", username);
        }

        return skipMaintenancePageForUser;
    }

    public void skipMaintenancePageCheck(String email) {
        if (maintenancePageEnabled) {
            boolean skipMaintenancePage = Arrays.stream(skipMaintenancePageForUsers.split(","))
                    .anyMatch(u -> u.trim().equalsIgnoreCase(email.trim()));
            log.info("skipMaintenancePageCheck.skipMaintenancePage: {}", skipMaintenancePage);
            if(!skipMaintenancePage) {
                log.warn("skipMaintenancePageCheck.User is not allowed to access the website due to maintenance page is enabled. " +
                        "Showing error page to the user: {}", email);
                throw new GenericServerException("User is not allowed to access the website due to maintenance page is enabled.");
            }
        }
    }

    public boolean shouldNotApplyMaintenancePageFilterForURI(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.info("shouldNotApplyMaintenancePageFilterForURI.requestURI {}", requestURI);
        boolean shouldNotApplyMaintenancePageFilterForURI = isNotBlank(requestURI) && Arrays.stream(skipMaintenancePageForUris.split(","))
                .anyMatch(u -> u.trim().equalsIgnoreCase(requestURI.trim()));
        log.info("shouldNotApplyMaintenancePageFilterForURI.shouldNotApplyMaintenancePageFilterForURI: {}", shouldNotApplyMaintenancePageFilterForURI);
        return shouldNotApplyMaintenancePageFilterForURI;
    }

    private void debug(HttpServletRequest request) {
        log.info("skipMaintenancePageForUser.debug.start");

        String requestURI = request.getRequestURI();
        log.info("skipMaintenancePageForUser.requestURI: {}", requestURI);

        String method = request.getMethod();
        log.info("skipMaintenancePageForUser.method: {}", method);

        String authType = request.getAuthType();
        log.info("skipMaintenancePageForUser.authType: {}", authType);

        Principal userPrincipal = request.getUserPrincipal();
        log.info("skipMaintenancePageForUser.userPrincipal: {}", userPrincipal);

        String authorization = request.getHeader("authorization");
        log.info("skipMaintenancePageForUser.request header authorization: {}", authorization);

        String grant_type = request.getParameter("grant_type");
        log.info("skipMaintenancePageForUser.request parameter grant_type: {}", grant_type);

        String redirect_uri = request.getParameter("redirect_uri");
        log.info("skipMaintenancePageForUser.request parameter redirect_uri: {}", redirect_uri);

        String client_id = request.getParameter("client_id");
        log.info("skipMaintenancePageForUser.request parameter client_id: {}", client_id);

        String code = request.getParameter("code");
        log.info("skipMaintenancePageForUser.request parameter code: {}", code);

        String username = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
        log.info("skipMaintenancePageForUser.request parameter username: {}", username);

        log.info("skipMaintenancePageForUser.debug.end");
    }
}
