package uk.gov.cabinetoffice.csl.service.client.csrs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.cabinetoffice.csl.service.client.HttpClient;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;
import uk.gov.cabinetoffice.csl.service.auth2.RestTemplateOAuthInterceptor;

@Configuration
public class CivilServantRegistryClientConfig {

    @Value("${civilServantRegistry.serviceUrl}")
    private String civilServantRegistryBaseUrl;

    private final RestTemplateOAuthInterceptor restTemplateOAuthInterceptor;

    public CivilServantRegistryClientConfig(RestTemplateOAuthInterceptor restTemplateOAuthInterceptor) {
        this.restTemplateOAuthInterceptor = restTemplateOAuthInterceptor;
    }

    @Bean(name = "civilServantRegistryHttpClient")
    IHttpClient civilServantRegistryClient(RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder
                .rootUri(civilServantRegistryBaseUrl)
                .additionalInterceptors(restTemplateOAuthInterceptor)
                .build();
        return new HttpClient(restTemplate);
    }
}
