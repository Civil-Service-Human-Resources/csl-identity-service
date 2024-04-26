package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;
import uk.gov.cabinetoffice.csl.service.client.IHttpClient;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.DomainsResponse;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class CivilServantRegistryClient implements ICivilServantRegistryClient {

    @Value("${civilServantRegistry.civilServantUrl}")
    private String civilServantUrl;

    @Value("${civilServantRegistry.getOrganisationsMaxPageSize}")
    private Integer getOrganisationsMaxPageSize;

    @Value("${civilServantRegistry.organisationalUnitsUrl}")
    private String organisationalUnitsUrl;

    @Value("${civilServantRegistry.domainsUrl}")
    private String domainsUrl;

    @Value("${civilServantRegistry.agencyTokensUrl}")
    private String agencyTokensUrl;

    private final IHttpClient httpClient;

    private final CsrsServiceDataTransformer csrsServiceDataTransformer;

    public CivilServantRegistryClient(@Qualifier("civilServantRegistryHttpClient") IHttpClient httpClient,
                                      CsrsServiceDataTransformer csrsServiceDataTransformer) {
        this.httpClient = httpClient;
        this.csrsServiceDataTransformer = csrsServiceDataTransformer;
    }

    @Override
    public boolean isDomainAllowListed(String domain) {
        return this.getAllowListDomainsFromCache().contains(domain.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isDomainValid(String domain) {
        return !this.getFilteredOrganisations(domain).isEmpty();
    }

    @Override
    public boolean isDomainInAnAgencyToken(String domain) {
        try {
            String url = agencyTokensUrl + String.format("?domain=%s", domain);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return httpClient.executeRequest(request, Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDomainInAnAgencyTokenWithOrg(String domain, String orgCode) {
        try {
            String url = agencyTokensUrl + String.format("?domain=%s&code=%s", domain, orgCode);
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return httpClient.executeRequest(request, Boolean.class);
        } catch (Exception e) {
            return false;
        }
    }

    private DomainsResponse getAllowListDomains() {
        log.info("Fetching allowlist domains from Civil Servant Registry");
            RequestEntity<Void> request = RequestEntity.get(domainsUrl).build();
            return httpClient.executeRequest(request, DomainsResponse.class);
    }

    @Override
    @Cacheable("allowListDomains")
    public List<String> getAllowListDomainsFromCache() {
        log.info("Fetching allowlist domains from Civil Servant Registry");
        try {
            DomainsResponse domainsResponse = getAllowListDomains();
            if (domainsResponse == null) {
                log.error("Allowlist Domains returned null");
                throw new GenericServerException("System error");
            }
            return domainsResponse.getDomains()
                    .stream()
                    .map(d -> d.getDomain().toLowerCase())
                    .collect(toList());
        } catch (Exception e) {
            log.error("An error has occurred while getting allow listed domains from Civil Servant Registry", e);
            throw new GenericServerException("System error");
        }
    }

    @Override
    @CacheEvict(value = "allowListDomains", allEntries = true)
    public void evictAllowListDomainCache() {
        log.info("Evicting Allowlist Domains cache");
    }

    @Override
    public List<OrganisationalUnit> getFilteredOrganisations(String domain) {
        return this.getAllOrganisationsFromCache()
                .stream()
                .filter(o -> o.doesDomainExist(domain))
                .collect(toList());
    }

    @Override
    public List<OrganisationalUnit> getAllOrganisations() {
        log.info("Fetching all organisations from Civil Servant Registry API");
        List<OrganisationalUnit> organisationalUnits = new ArrayList<>();
        GetOrganisationsResponse initialResponse = getOrganisations(1, 0);
        if (initialResponse.getTotalElements() >= 1) {
            List<CompletableFuture<List<OrganisationalUnit>>> futures =
                    IntStream.range(0, (int) Math.ceil((double) initialResponse.getTotalElements() / getOrganisationsMaxPageSize))
                            .boxed()
                            .map(i -> CompletableFuture.supplyAsync(() -> getOrganisations(getOrganisationsMaxPageSize, i).getContent())).toList();

            organisationalUnits = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(i -> futures.stream().flatMap(listCompletableFuture -> listCompletableFuture.join().stream()).collect(toList())).join();

        }
        log.info(String.format("%s", organisationalUnits.size()));
        return organisationalUnits;
    }

    private GetOrganisationsResponse getOrganisations(Integer size, Integer page) {
        String url = organisationalUnitsUrl + String.format("?size=%s&page=%s&formatName=true", size, page);
        RequestEntity<Void> request = RequestEntity.get(url).build();
        return httpClient.executeRequest(request, GetOrganisationsResponse.class);
    }

    @Override
    @Cacheable("organisations")
    public List<OrganisationalUnit> getAllOrganisationsFromCache() {
        List<OrganisationalUnit> organisationalUnits = getAllOrganisations();
        return csrsServiceDataTransformer.transformOrganisations(organisationalUnits);
    }

    @Override
    @CacheEvict(value = "organisations", allEntries = true)
    public void evictOrganisationsCache() {
        log.info("Evicting organisations cache");
    }

    @Override
    public void removeOrganisationalUnitFromCivilServant(String uid) {
        try {
            log.info(format("Removing organisation from user %s", uid));
            String url = format("%s/resource/%s/remove_organisation", civilServantUrl, uid);
            RequestEntity<Void> request = RequestEntity.post(url).build();
            httpClient.executeRequest(request, Void.class);
        } catch (Exception e) {
            log.error("An error has occurred while removing organisation from user using Civil Servant registry", e);
            throw new GenericServerException("System error");
        }
    }

    private Optional<AgencyToken> getAgencyToken(String url) {
        try {
            RequestEntity<Void> request = RequestEntity.get(url).build();
            return Optional.of(httpClient.executeRequest(request, AgencyToken.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AgencyToken> getAgencyToken(String domain, String token, String organisation) {
        return getAgencyToken(agencyTokensUrl + String.format("?domain=%s&token=%s&code=%s", domain, token, organisation));
    }

    @Override
    public Optional<AgencyToken> getAgencyTokenWithUid(String uid) {
        return getAgencyToken(agencyTokensUrl + String.format("?uid=%s", uid));
    }

    @Override
    public boolean isAgencyTokenUidValidForDomain(String agencyTokenUid, String domain) {
        return getAgencyTokenWithUid(agencyTokenUid)
                .map(token -> token.getUid().equals(agencyTokenUid) && token.isDomainAssignedToAgencyToken(domain))
                .orElse(false);
    }
}
