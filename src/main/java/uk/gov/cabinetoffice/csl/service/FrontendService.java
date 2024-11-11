package uk.gov.cabinetoffice.csl.service;

import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
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
        IdentityDetails identity = (IdentityDetails) userAuthService.getAuthentication().getPrincipal();
        frontendClient.signOutUser(identity.getIdentity().getUid());
    }

}
