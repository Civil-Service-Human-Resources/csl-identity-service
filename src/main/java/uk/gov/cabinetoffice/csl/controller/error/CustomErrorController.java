package uk.gov.cabinetoffice.csl.controller.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.cabinetoffice.csl.util.ErrorPageMap;

@Controller
public class CustomErrorController implements ErrorController {

  private static final String GENERIC_ERROR = "/error";

  @RequestMapping(GENERIC_ERROR)
  public String handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (status != null) {
      Integer statusCode = Integer.valueOf(status.toString());
        return ErrorPageMap.ERROR_PAGES.getOrDefault(statusCode, GENERIC_ERROR);
    }
    return GENERIC_ERROR;
  }
}
