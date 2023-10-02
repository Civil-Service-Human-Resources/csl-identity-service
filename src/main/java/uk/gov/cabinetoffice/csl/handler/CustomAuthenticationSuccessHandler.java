package uk.gov.cabinetoffice.csl.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

@NoArgsConstructor
@Configuration
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

//    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
    private final ForwardAuthenticationSuccessHandler forward = new ForwardAuthenticationSuccessHandler("http:////localhost:3004");

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication != null) {
            if (authentication.getPrincipal() != null) {
                    System.out.println("#####CustomAuthenticationSuccessHandler.onAuthenticationSuccess.authentication.getPrincipal(): " + authentication.getPrincipal());
                    //IdentityService.setLastLoggedIn()
            }
        }
        System.out.println("#####CustomAuthenticationSuccessHandler.onAuthenticationSuccess.Before forwarding");
        this.forward.onAuthenticationSuccess(request, response, authentication);
        System.out.println("#####CustomAuthenticationSuccessHandler.onAuthenticationSuccess.After forwarding");
//        this.delegate.onAuthenticationSuccess(request, response, authentication);
//        System.out.println("#####CustomAuthenticationSuccessHandler.onAuthenticationSuccess.After delegate");
    }
}
