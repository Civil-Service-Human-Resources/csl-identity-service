package uk.gov.cabinetoffice.csl.controller.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.io.IOException;

@Slf4j
@Controller
public class LoginController {

  @Value("${authenticationSuccess.targetUrl}")
  private String authenticationSuccessTargetUrl;

  private final Utils utils;

  public LoginController(Utils utils) {
    this.utils = utils;
  }

  @RequestMapping("/login")
  public String login(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {

    if(utils.displayMaintenancePage(request, model)) {
      return "maintenance/maintenance";
    }

    DefaultSavedRequest dsr =
        (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    if (dsr != null && dsr.getQueryString() == null) {
      response.sendRedirect(authenticationSuccessTargetUrl);
    }
    return "login";
  }
}
