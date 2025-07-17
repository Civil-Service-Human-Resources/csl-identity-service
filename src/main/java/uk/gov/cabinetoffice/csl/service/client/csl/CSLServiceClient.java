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

    @Value("${cslService.updateEmail}")
    private String updateEmail;

    private final IHttpClient httpClient;

    public CSLServiceClient(@Qualifier("cslServiceHttpClient") IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void activateUser(String uid) {
        try {
            log.info("Activating Identity in reporting database for user {}", uid);
            String url = String.format(activateUserUrl, uid);
            RequestEntity<Void> request = RequestEntity.post(url).build();
            httpClient.executeRequest(request, Void.class);
            log.info("Identity activated in reporting database for user {}", uid);
        } catch (Exception e) {
            log.error("An error has occurred while activating identity for user {} in reporting database", uid, e);
            throw new GenericServerException("System error");
        }
    }

    @Override
    public void updateEmail(String uid, String email) {
        try {
            log.info("Updating Email {} in reporting database for user {}", email, uid);
            String url = String.format(updateEmail, uid, email);
            RequestEntity<Void> request = RequestEntity.post(url).build();
            httpClient.executeRequest(request, Void.class);
            log.info("Email {} updated in reporting database for user {}", email, uid);
        } catch (Exception e) {
            log.error("An error has occurred while updating email {} for user {} in reporting database", email, uid, e);
            throw new GenericServerException("System error");
        }
    }
}
