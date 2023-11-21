package uk.gov.cabinetoffice.csl.controller.cachereset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.service.client.identity.IIdentityClient;

@Slf4j
@RestController
@RequestMapping("/reset-cache")
public class CacheResetController {

    private final IIdentityClient identityClient;

    public CacheResetController(IIdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @GetMapping(path = "/service-token", produces = "application/json")
    public ResponseEntity<?> removeServiceTokenFromCache() {
        identityClient.removeServiceTokenFromCache();
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
