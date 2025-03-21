package uk.gov.cabinetoffice.csl.service;

import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.client.frontend.IFrontendClient;

@Component
public class FrontendService {

    private final IFrontendClient frontendClient;
    public FrontendService(IFrontendClient frontendClient) {
        this.frontendClient = frontendClient;
    }

    public void signoutUser(String uid) {
        frontendClient.signOutUser(uid);
    }
}
