package uk.gov.cabinetoffice.csl.service.auth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

@Slf4j
@Component
public class RestTemplateOAuthInterceptor implements ClientHttpRequestInterceptor {

    private final IBearerTokenService bearerTokenService;

    public RestTemplateOAuthInterceptor(IBearerTokenService bearerTokenService) {
        this.bearerTokenService = bearerTokenService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        try {
            String token = bearerTokenService.getBearerToken();
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        } catch (Exception e) {
            log.error("RestTemplateOAuthInterceptor.intercept: Error has occurred {}", e.toString());
            throw new GenericServerException("System error");
        }
    }
}
