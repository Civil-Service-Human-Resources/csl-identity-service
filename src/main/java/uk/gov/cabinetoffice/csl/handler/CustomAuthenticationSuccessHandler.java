package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
@Configuration
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Value("${authenticationSuccess.targetUrl}")
    private String authenticationSuccessTargetUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("CustomAuthenticationSuccessHandler:onAuthenticationSuccess:authentication: {}", authentication);
        this.setDefaultTargetUrl(authenticationSuccessTargetUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
