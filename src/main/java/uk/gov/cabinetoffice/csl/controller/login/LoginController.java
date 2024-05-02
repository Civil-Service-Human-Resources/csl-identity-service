package uk.gov.cabinetoffice.csl.controller.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

import java.io.IOException;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Controller
public class LoginController {

  private static final String SKIP_MAINTENANCE_PAGE_PARAM_NAME = "username";

  @Value("${authenticationSuccess.targetUrl}")
  private String authenticationSuccessTargetUrl;

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

  @RequestMapping("/login")
  public String login(HttpServletRequest request, HttpServletResponse response,
                      Model model, Authentication authentication) throws IOException {

    if(maintenancePageEnabled) {
      log.info("LoginController.login.authentication: {}", authentication);
      if (authentication != null) {
        Object principal = authentication.getPrincipal();
        log.info("LoginController.login.principal: {}", principal);
        if(principal instanceof IdentityDetails identityDetails) {
          log.info("LoginController.login.identityDetails: {}", identityDetails);
        }
        if(principal instanceof Jwt jwt) {
          log.info("LoginController.login.jwt: {}", jwt);
          String email = jwt.getClaim("email");
          log.info("LoginController.login.email: {}", email);
        }
      }

      String skipMaintenancePageForUser = request.getParameter(SKIP_MAINTENANCE_PAGE_PARAM_NAME);
      log.info("LoginController.login.skipMaintenancePageForUser.username: {}", skipMaintenancePageForUser);

      boolean skipMaintenancePage = isNotBlank(skipMaintenancePageForUser) &&
              Arrays.stream(skipMaintenancePageForUsers.split(","))
              .anyMatch(u -> u.trim().equalsIgnoreCase(skipMaintenancePageForUser.trim()));
      log.info("LoginController.login.skipMaintenancePage: {}", skipMaintenancePage);

      if (!skipMaintenancePage) {
        model.addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
        model.addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
        model.addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
        model.addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
        return "maintenance/maintenance";
      }
    }

    DefaultSavedRequest dsr =
        (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    if (dsr != null && dsr.getQueryString() == null) {
      response.sendRedirect(authenticationSuccessTargetUrl);
    }
    return "login";
  }
}
