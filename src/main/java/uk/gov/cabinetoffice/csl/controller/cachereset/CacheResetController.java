package uk.gov.cabinetoffice.csl.controller.cachereset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.service.client.identity.IIdentityClient;

@Slf4j
@RestController
@RequestMapping("/reset-cache")
public class CacheResetController {

    private final IIdentityClient identityClient;
    private final ICivilServantRegistryClient civilServantRegistryClient;

    public CacheResetController(IIdentityClient identityClient, ICivilServantRegistryClient civilServantRegistryClient) {
        this.identityClient = identityClient;
        this.civilServantRegistryClient = civilServantRegistryClient;
    }

    @GetMapping(path = "/service-token", produces = "application/json")
    public ResponseEntity<?> evictServiceTokenFromCache() {
        identityClient.evictServiceTokenFromCache();
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping(path = "/allowlist-domains", produces = "application/json")
    public ResponseEntity<?> evictAllowListDomainCache() {
        civilServantRegistryClient.evictAllowListDomainCache();
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
