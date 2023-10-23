package uk.gov.cabinetoffice.csl.client.identity;

import uk.gov.cabinetoffice.csl.domain.identity.OAuth2Token;

public interface IIdentityClient {

    OAuth2Token getServiceToken();
}
