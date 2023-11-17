package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.client.csrs.ICivilServantRegistryClient;

@Slf4j
@Service
public class AgencyTokenService {

    private final UserService userService;

    private final ICivilServantRegistryClient civilServantRegistryClient;

    public AgencyTokenService(UserService userService, ICivilServantRegistryClient civilServantRegistryClient) {
        this.userService = userService;
        this.civilServantRegistryClient = civilServantRegistryClient;
    }

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
