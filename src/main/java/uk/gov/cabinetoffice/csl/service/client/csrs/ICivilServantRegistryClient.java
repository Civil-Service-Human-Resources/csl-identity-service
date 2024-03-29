package uk.gov.cabinetoffice.csl.service.client.csrs;

import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnitDTO;

import java.util.List;
import java.util.Optional;

public interface ICivilServantRegistryClient {

    Boolean isDomainInAgency(String domain);

    Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation);

    OrganisationalUnitDTO[] getOrganisationalUnitsFormatted();

    AgencyToken[] getAgencyTokensForDomain(String domain);

    List<String> getAllowListDomains();

    void evictAllowListDomainCache();
}
