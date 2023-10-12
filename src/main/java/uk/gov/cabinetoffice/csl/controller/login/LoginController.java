package uk.gov.cabinetoffice.csl.controller.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
public class LoginController {

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

  @RequestMapping("/login")
  public String login(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {

    if(maintenancePageEnabled) {
      model.addAttribute("maintenancePageContentLine1", maintenancePageContentLine1);
      model.addAttribute("maintenancePageContentLine2", maintenancePageContentLine2);
      model.addAttribute("maintenancePageContentLine3", maintenancePageContentLine3);
      model.addAttribute("maintenancePageContentLine4", maintenancePageContentLine4);
      return "maintenance";
    }

    DefaultSavedRequest dsr =
        (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    if (dsr != null && dsr.getQueryString() == null) {
      response.sendRedirect(authenticationSuccessTargetUrl);
    }
    return "login";
  }
}
