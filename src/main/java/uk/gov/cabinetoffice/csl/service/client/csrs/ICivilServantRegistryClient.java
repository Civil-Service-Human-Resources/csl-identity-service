package uk.gov.cabinetoffice.csl.service.client.csrs;

import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.List;
import java.util.Optional;

public interface ICivilServantRegistryClient {

    void removeOrganisationalUnitFromCivilServant(String uid);

    Boolean isDomainInAgency(String domain);

    Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation);

    OrganisationalUnit[] getOrganisationalUnitsFormatted();

    List<String> getAllowListDomains();

    void evictAllowListDomainCache();
}
