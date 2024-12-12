package uk.gov.cabinetoffice.csl.service.client.frontend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;

import static java.lang.String.format;

@Component
@Slf4j
public class FrontendClient implements IFrontendClient {

    private final IHttpClient client;

    private final String logoutUserEndpoint;

    public FrontendClient(@Qualifier("lpgUiClient") IHttpClient client, @Value("${lpg.SignoutEndpoint}") String logoutUserEndpoint) {
        this.client = client;
        this.logoutUserEndpoint = logoutUserEndpoint;
    }

    @Override
    public void signOutUser(String uid) {
        log.info("Signing current user out");
        String url = format(logoutUserEndpoint, uid);
        RequestEntity<Void> request = RequestEntity.post(url).build();
        client.executeRequest(request, Void.class);
    }
}
