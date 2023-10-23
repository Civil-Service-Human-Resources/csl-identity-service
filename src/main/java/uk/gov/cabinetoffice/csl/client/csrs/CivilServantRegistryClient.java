package uk.gov.cabinetoffice.csl.client.csrs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.cabinetoffice.csl.client.IHttpClient;
import uk.gov.cabinetoffice.csl.domain.AgencyToken;
import uk.gov.cabinetoffice.csl.domain.OrganisationalUnitDTO;

import java.util.Optional;

@Slf4j
@Component
public class CivilServantRegistryClient implements ICivilServantRegistryClient {

    @Value("${civilServantRegistry.agencyTokensFormat}")
    private String agencyTokensFormat;

    @Value("${civilServantRegistry.agencyTokensByDomainFormat}")
    private String agencyTokensByDomainFormat;

    @Value("${civilServantRegistry.organisationalUnitsFlatUrl}")
    private String organisationalUnitsFlatUrl;

    private final IHttpClient httpClient;

    public CivilServantRegistryClient(@Qualifier("civilServantRegistryHttpClient") IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Boolean isDomainInAgency(String domain) {
        try {
            String url = String.format(agencyTokensByDomainFormat, domain);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return httpClient.executeRequest(request, Boolean.class);
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while checking if domain in agency", e);
            return null;
        }
    }

    public Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation) {
        try {
            String url = String.format(agencyTokensFormat, domain, token, organisation);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return Optional.of(httpClient.executeRequest(request, AgencyToken.class));
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while getting Agency Token For Domain Token Organisation", e);
            return Optional.empty();
        }
    }

    public OrganisationalUnitDTO[] getOrganisationalUnitsFormatted() {
        OrganisationalUnitDTO[] organisationalUnitDTOs;
        try {
            RequestEntity<Void> request = RequestEntity.get(organisationalUnitsFlatUrl).build();
            return httpClient.executeRequest(request, OrganisationalUnitDTO[].class);
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while getting Organisational Units Formatted", e);
            organisationalUnitDTOs = new OrganisationalUnitDTO[0];
        }
        return organisationalUnitDTOs;
    }
}
