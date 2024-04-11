package uk.gov.cabinetoffice.csl.controller.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

  private static final String GENERIC_ERROR_PAGE_TEMPLATE = "/error";

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (status != null) {
      Integer statusCode = Integer.valueOf(status.toString());
        return ErrorPageMap.ERROR_PAGES.getOrDefault(statusCode, GENERIC_ERROR_PAGE_TEMPLATE);
    }
    return GENERIC_ERROR_PAGE_TEMPLATE;
  }
}
