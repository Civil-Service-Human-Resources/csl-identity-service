package uk.gov.cabinetoffice.csl.controller.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Slf4j
@Controller
public class LoginController {

  @Value("${lpg.uiUrl}")
  private String lpgUiBaseUrl;

  @Value("${lpg.Signout}")
  private String logoutSignout;

  @RequestMapping("/login")
  public String login(HttpServletRequest request, HttpServletResponse response) throws IOException {

    DefaultSavedRequest dsr =
        (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    log.debug("LoginController.login: dsr: {} ", dsr);
    if (dsr != null && dsr.getQueryString() == null) {
      log.debug("LoginController.login: dsr: {} ", dsr);
      log.debug("LoginController.login: setting response.sendRedirect: authenticationSuccessTargetUrl: {} ",
              lpgUiBaseUrl + logoutSignout);
      response.sendRedirect(lpgUiBaseUrl + logoutSignout);
    }
    log.debug("LoginController.login: returning login");
    return "login";
  }
}
