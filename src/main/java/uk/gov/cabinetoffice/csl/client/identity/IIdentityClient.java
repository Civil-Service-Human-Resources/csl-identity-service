package uk.gov.cabinetoffice.csl.client.identity;

import uk.gov.cabinetoffice.csl.service.auth2.OAuthToken;

public interface IIdentityClient {

    OAuthToken getServiceToken();
}
