package uk.gov.cabinetoffice.csl.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;
import uk.gov.cabinetoffice.csl.service.client.frontend.IFrontendClient;

@Component
public class FrontendService {

    private final IFrontendClient frontendClient;
    private final IUserAuthService userAuthService;
    public FrontendService(IFrontendClient frontendClient, IUserAuthService userAuthService) {
        this.frontendClient = frontendClient;
        this.userAuthService = userAuthService;
    }

    public void signoutUser() {
        String uid = userAuthService.getUid();
        if(StringUtils.isNotBlank(uid)) {
            frontendClient.signOutUser(uid);
        }
    }
}
