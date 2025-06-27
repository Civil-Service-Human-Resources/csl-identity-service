package uk.gov.cabinetoffice.csl.service.client.csl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;

@Slf4j
@Component
public class CSLServiceClient implements ICSLServiceClient {

    @Value("${cslService.activateUser}")
    private String activateUserUrl;

    private final IHttpClient httpClient;

    public CSLServiceClient(@Qualifier("cslServiceHttpClient") IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void activateUser(String uid) {
        try {
            log.info("Identity activated for user {}", uid);
            String url = String.format(activateUserUrl, uid);
            RequestEntity<Void> request = RequestEntity.post(url).build();
            httpClient.executeRequest(request, Void.class);
        } catch (Exception e) {
            log.error("An error has occurred while identity activated for user {}", uid, e);
            throw new GenericServerException("System error");
        }
    }
}
