package uk.gov.cabinetoffice.csl.service.client.csl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.cabinetoffice.csl.service.auth2.RestTemplateOAuthInterceptor;
import uk.gov.cabinetoffice.csl.service.client.HttpClient;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;

@Configuration
public class CSLServiceClientConfig {

    @Value("${cslService.serviceUrl}")
    private String cslServiceBaseUrl;

    private final RestTemplateOAuthInterceptor restTemplateOAuthInterceptor;

    public CSLServiceClientConfig(RestTemplateOAuthInterceptor restTemplateOAuthInterceptor) {
        this.restTemplateOAuthInterceptor = restTemplateOAuthInterceptor;
    }

    @Bean(name = "cslServiceHttpClient")
    IHttpClient cslServiceClient(RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder
                .rootUri(cslServiceBaseUrl)
                .additionalInterceptors(restTemplateOAuthInterceptor)
                .build();
        return new HttpClient(restTemplate);
    }
}
