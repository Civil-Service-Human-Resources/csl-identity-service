package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.DomainsResponse;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Component
public class CivilServantRegistryClient implements ICivilServantRegistryClient {

    @Value("${civilServantRegistry.civilServantUrl}")
    private String civilServantUrl;

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
    public void removeOrganisationalUnitFromCivilServant(String uid) {
        log.info(format("Removing organisation from user %s", uid));
        String url = format("%s/resource/%s/remove_organisation", civilServantUrl, uid);
        RequestEntity<Void> request = RequestEntity.post(url).build();
        httpClient.executeRequest(request, Void.class);
    }

    @Override
    public Boolean isDomainInAgency(String domain) {
        try {
            String url = format(agencyTokensByDomainFormat, domain);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return httpClient.executeRequest(request, Boolean.class);
        } catch (HttpClientErrorException e) {
            log.error("An error has occurred while checking if domain in agency using Civil Servant registry", e);
            return false;
        }
    }

    @Override
    public Optional<AgencyToken> getAgencyTokenForDomainTokenOrganisation(String domain, String token, String organisation) {
        try {
            String url = format(agencyTokensFormat, domain, token, organisation);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return Optional.of(httpClient.executeRequest(request, AgencyToken.class));
        } catch (HttpClientErrorException e) {
            log.error("An error has occurred while getting Agency Token For Domain Token Organisation from Civil Servant registry", e);
            return Optional.empty();
        }
    }

    @Override
    public OrganisationalUnit[] getOrganisationalUnitsFormatted() {
        try {
            RequestEntity<Void> request = RequestEntity.get(organisationalUnitsFlatUrl).build();
            return httpClient.executeRequest(request, OrganisationalUnit[].class);
        } catch (HttpClientErrorException e) {
            log.error("An error has occurred while getting Organisational Units Formatted from Civil Servant registry", e);
            return new OrganisationalUnit[0];
        }
    }

    @Override
    @Cacheable("allowListDomains")
    public List<String> getAllowListDomains() {
        log.info("Fetching allowlist domains from Civil Servant Registry");
        try {
            RequestEntity<Void> request = RequestEntity.get(domainsUrl).build();
            DomainsResponse domainsResponse = httpClient.executeRequest(request, DomainsResponse.class);
            if (domainsResponse == null) {
                log.error("Allowlist Domains returned null");
                throw new GenericServerException("System error");
            }
            return domainsResponse.getDomains().stream().map(d -> d.getDomain().toLowerCase()).collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            log.error("An error has occurred while getting allow listed domains from Civil Servant Registry", e);
            throw new GenericServerException("System error");
        }
    }

    @Override
    @CacheEvict(value = "allowListDomains", allEntries = true)
    public void evictAllowListDomainCache() {
        log.info("Evicting Allowlist Domains cache");
    }
}
