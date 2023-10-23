package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.cabinetoffice.csl.domain.AgencyToken;
import uk.gov.cabinetoffice.csl.domain.OrganisationalUnitDto;

import java.util.Optional;

@Slf4j
@Service
public class CsrsService {

    private final String agencyTokensFormat;
    private final String agencyTokensByDomainFormat;
    private final String agencyTokensByDomainAndOrganisationFormat;
    private final String organisationalUnitsFlatUrl;

    //TODO: To be implemented as part of the future ticket
    //private RestTemplate restTemplate;

    public CsrsService(//@Autowired RestTemplate restTemplate,
                       @Value("${registry.agencyTokensFormat}") String agencyTokensFormat,
                       @Value("${registry.agencyTokensByDomainFormat}") String agencyTokensByDomainFormat,
                       @Value("${registry.agencyTokensByDomainAndOrganisationFormat}") String agencyTokensByDomainAndOrganisationFormat,
                       @Value("${registry.organisationalUnitsFlatUrl}") String organisationalUnitsFlatUrl) {
        //this.restTemplate = restTemplate;
        this.agencyTokensFormat = agencyTokensFormat;
        this.agencyTokensByDomainFormat = agencyTokensByDomainFormat;
        this.agencyTokensByDomainAndOrganisationFormat = agencyTokensByDomainAndOrganisationFormat;
        this.organisationalUnitsFlatUrl = organisationalUnitsFlatUrl;
    }

    public Boolean isDomainInAgency(String domain) {
        try {
            //TODO: To be implemented as part of the future ticket
            //return restTemplate.getForObject(String.format(agencyTokensByDomainFormat, domain), Boolean.class);
            return false;
        } catch (HttpClientErrorException e) {
            log.error("An error occurred checking if domain in agency", e);
            return false;
        }
    }

    public AgencyToken[] getAgencyTokensForDomain(String domain) {
        try {
            //TODO: To be implemented as part of the future ticket
            //return restTemplate.getForObject(String.format(agencyTokensByDomainFormat, domain), AgencyToken[].class);
            return null;
        } catch (HttpClientErrorException e) {
            return new AgencyToken[]{};
        }
    }

    public Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation) {
        try {
            //TODO: To be implemented as part of the future ticket
            //return Optional.of(restTemplate.getForObject(String.format(agencyTokensFormat, domain, token, organisation), AgencyToken.class));
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }

    public Optional<AgencyToken> getAgencyTokenForDomainAndOrganisation(String domain, String organisation) {
        try {
            //TODO: To be implemented as part of the future ticket
            //return Optional.of(restTemplate.getForObject(String.format(agencyTokensByDomainAndOrganisationFormat, domain, organisation), AgencyToken.class));
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            return Optional.empty();
        }
    }

    public OrganisationalUnitDto[] getOrganisationalUnitsFormatted() {
        OrganisationalUnitDto[] organisationalUnitDtos = null;
        try {
            //TODO: To be implemented as part of the future ticket
            //organisationalUnitDtos = restTemplate.getForObject(organisationalUnitsFlatUrl, OrganisationalUnitDto[].class);
        } catch (HttpClientErrorException e) {
            organisationalUnitDtos = new OrganisationalUnitDto[0];
        }
        return organisationalUnitDtos;
    }
}
