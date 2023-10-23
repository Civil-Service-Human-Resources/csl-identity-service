package uk.gov.cabinetoffice.csl.client.csrs;

import uk.gov.cabinetoffice.csl.domain.AgencyToken;
import uk.gov.cabinetoffice.csl.domain.OrganisationalUnitDto;

import java.util.Optional;

public interface ICivilServantRegistryClient {

    Boolean isDomainInAgency(String domain);

    Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation);

    OrganisationalUnitDto[] getOrganisationalUnitsFormatted();
}
