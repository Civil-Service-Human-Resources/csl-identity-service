package uk.gov.cabinetoffice.csl.service.client.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.cabinetoffice.csl.service.client.HttpClient;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;

@Configuration
public class IdentityClientConfig {

    @Value("${oauth2.serviceUrl}")
    private String identityBaseUrl;

    @Value("${oauth2.clientId}")
    private String identityClientId;

    @Value("${oauth2.clientSecret}")
    private String identityClientSecret;

    @Bean(name = "identityHttpClient")
    IHttpClient identityClient(RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder
                .rootUri(identityBaseUrl)
                .basicAuthentication(identityClientId, identityClientSecret)
                .build();
        return new HttpClient(restTemplate);
    }
}
