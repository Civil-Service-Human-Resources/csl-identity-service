package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;

@AllArgsConstructor
@Service
public class AgencyTokenService {

    private final UserService userService;

    private final ICivilServantRegistryClient civilServantRegistryClient;

    public boolean isDomainAllowListed(String domain) {
        return userService.isAllowListedDomain(domain);
    }

    public boolean isDomainAnAgencyTokenDomain(String domain) {
        return numAgencyTokens(domain) > 0;
    }

    private int numAgencyTokens(String domain) {
        return civilServantRegistryClient.getAgencyTokensForDomain(domain).length;
    }

    public boolean isDomainInAgencyToken(String domain) {
        return civilServantRegistryClient.isDomainInAgency(domain);
    }
}
