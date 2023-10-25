package uk.gov.cabinetoffice.csl.controller.cachereset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.service.IdentityService;

@Slf4j
@RestController
@RequestMapping("/reset-cache")
public class CacheResetController {

    private final IdentityService identityService;

    public CacheResetController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping(path = "/service-token", produces = "application/json")
    public ResponseEntity<?> removeServiceTokenFromCache() {
        identityService.removeServiceTokenFromCache();
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
