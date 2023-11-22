package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;
import uk.gov.cabinetoffice.csl.domain.AgencyToken;
import uk.gov.cabinetoffice.csl.domain.DomainsResponse;
import uk.gov.cabinetoffice.csl.domain.OrganisationalUnitDTO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CivilServantRegistryClient implements ICivilServantRegistryClient {

    @Value("${civilServantRegistry.agencyTokensFormat}")
    private String agencyTokensFormat;

    @Value("${civilServantRegistry.agencyTokensByDomainFormat}")
    private String agencyTokensByDomainFormat;

    @Value("${civilServantRegistry.organisationalUnitsFlatUrl}")
    private String organisationalUnitsFlatUrl;

    @Value("${civilServantRegistry.domainsUrl}")
    private String domainsUrl;

    private final IHttpClient httpClient;

    public CivilServantRegistryClient(@Qualifier("civilServantRegistryHttpClient") IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Boolean isDomainInAgency(String domain) {
        try {
            String url = String.format(agencyTokensByDomainFormat, domain);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return httpClient.executeRequest(request, Boolean.class);
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while checking if domain in agency", e);
            return false;
        }
    }

    @Override
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

    @Override
    public OrganisationalUnitDTO[] getOrganisationalUnitsFormatted() {
        try {
            RequestEntity<Void> request = RequestEntity.get(organisationalUnitsFlatUrl).build();
            return httpClient.executeRequest(request, OrganisationalUnitDTO[].class);
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while getting Organisational Units Formatted", e);
            return new OrganisationalUnitDTO[0];
        }
    }

    @Override
    public AgencyToken[] getAgencyTokensForDomain(String domain) {
        try {
            RequestEntity<Void> request = RequestEntity.get(agencyTokensByDomainFormat).build();
            return httpClient.executeRequest(request, AgencyToken[].class);
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while getting Agency Tokens", e);
            return new AgencyToken[]{};
        }
    }

    @Override
    @Cacheable("allowlistdomains")
    public List<String> getAllowListDomains() {
        log.info("Fetching allowlist domains from CSRS API");
        try {
            RequestEntity<Void> request = RequestEntity.get(domainsUrl).build();
            DomainsResponse domainsResponse = httpClient.executeRequest(request, DomainsResponse.class);
            if (domainsResponse == null) {
                throw new RuntimeException("Allowlist Domains returned null");
            }
            return domainsResponse.getDomains().stream().map(d -> d.getDomain().toLowerCase()).collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            log.error("An error occurred while getting Agency Token For Domain Token Organisation", e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "allowlistdomains", allEntries = true)
    @Scheduled(fixedRateString = "${civilServantRegistry.cache.allowListDomainsTTL}")
    public void evictAllowListDomainCache() {
        log.info("Evicting Allowlist Domains cache");
    }
}
