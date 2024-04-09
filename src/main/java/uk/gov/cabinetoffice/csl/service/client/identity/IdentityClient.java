package uk.gov.cabinetoffice.csl.service.client.identity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;
import uk.gov.cabinetoffice.csl.service.auth2.OAuthToken;
import uk.gov.cabinetoffice.csl.exception.InternalAuthErrorException;

import java.time.LocalDateTime;

@Component
@Slf4j
public class IdentityClient implements IIdentityClient {

    private final IHttpClient client;

    @Value("${oauth2.tokenUrl}")
    private String tokenUrl;

    public IdentityClient(@Qualifier("identityHttpClient") IHttpClient client) {
        this.client = client;
    }

    @Override
    @Cacheable("serviceToken")
    public OAuthToken getServiceToken() {
        log.debug("Getting service token from identity service");
        String url = String.format("%s?grant_type=client_credentials", tokenUrl);
        RequestEntity<Void> request = RequestEntity.post(url).build();
        OAuthToken oAuthToken = client.executeRequest(request, OAuthToken.class);
        if (oAuthToken == null) {
            log.error("Service token response was null");
            throw new InternalAuthErrorException("System error");
        }
        oAuthToken.setExpiryDateTime(LocalDateTime.now().plusSeconds(oAuthToken.getExpiresIn()));
        return oAuthToken;
    }

    @Override
    @CacheEvict(value = "serviceToken", allEntries = true)
    public void evictServiceTokenFromCache() {
        log.info("Service token is removed from the cache.");
    }
}
