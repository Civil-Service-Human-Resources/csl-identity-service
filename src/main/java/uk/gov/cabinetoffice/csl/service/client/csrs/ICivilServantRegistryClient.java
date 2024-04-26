package uk.gov.cabinetoffice.csl.service.client.csrs;

import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.List;
import java.util.Optional;

public interface ICivilServantRegistryClient {

    boolean isDomainAllowListed(String domain);

    boolean isDomainValid(String domain);

    boolean isDomainInAnAgencyToken(String domain);

    boolean isDomainInAnAgencyTokenWithOrg(String domain, String orgCode);

    List<String> getAllowListDomainsFromCache();

    void evictAllowListDomainCache();

    List<OrganisationalUnit> getFilteredOrganisations(String domain);

    List<OrganisationalUnit> getAllOrganisations();

    List<OrganisationalUnit> getAllOrganisationsFromCache();

    void evictOrganisationsCache();

    void removeOrganisationalUnitFromCivilServant(String uid);

    Optional<AgencyToken> getAgencyToken(String domain, String token, String organisation);

    Optional<AgencyToken> getAgencyTokenWithUid(String uid);

    boolean isAgencyTokenUidValidForDomain(String agencyTokenUid, String domain);
}
