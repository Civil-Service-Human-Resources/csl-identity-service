package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;

import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CsrsService {
    private ICivilServantRegistryClient civilServantRegistryClient;

    public CsrsService(ICivilServantRegistryClient civilServantRegistryClient){
        this.civilServantRegistryClient = civilServantRegistryClient;
    }

    public boolean isDomainAllowlisted(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase(Locale.ROOT));
    }

    public List<OrganisationalUnit> getAllOrganisationalUnits() {
        return civilServantRegistryClient.getAllOrganisations();
    }

    public List<OrganisationalUnit> getOrganisationalUnitsByDomain(String domain) {
        return this.getAllOrganisationalUnits()
                .stream()
                .filter(o -> o.doesDomainExist(domain))
                .collect(toList());
    }

    public boolean isDomainValid(String domain) {
        return !this.getOrganisationalUnitsByDomain(domain).isEmpty();
    }
}
