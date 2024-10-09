package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    public boolean isDomainInAnAgencyToken(String domain){
        return civilServantRegistryClient.isDomainInAnAgencyToken(domain);
    }

    public boolean isDomainInAnAgencyTokenWithOrg(String domain, String orgCode) {
        return civilServantRegistryClient.isDomainInAnAgencyTokenWithOrg(domain, orgCode);
    }

    public void evictAllowListDomainCache(){
        civilServantRegistryClient.evictAllowListDomainCache();
    }

    public void evictOrganisationsCache(){
        civilServantRegistryClient.evictOrganisationsCache();
    }

    public void removeOrganisationalUnitFromCivilServant(String uid){
        civilServantRegistryClient.removeOrganisationalUnitFromCivilServant(uid);
    }

    public Optional<AgencyToken> getAgencyToken(String domain, String token, String organisation) {
        return civilServantRegistryClient.getAgencyToken(domain, token, organisation);
    }

    public boolean isAgencyTokenUidValidForDomain(String agencyTokenUid, String domain) {
        return civilServantRegistryClient.isAgencyTokenUidValidForDomain(agencyTokenUid, domain);
    }
}
