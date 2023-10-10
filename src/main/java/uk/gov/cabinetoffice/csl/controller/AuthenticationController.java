package uk.gov.cabinetoffice.csl.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

@AllArgsConstructor
@RestController
public class AuthenticationController {

    @GetMapping("/identity/resolve")
    public IdentityDTO resolveIdentity(Authentication authentication) {
        IdentityDetails identityDetails = (IdentityDetails) authentication.getPrincipal();
        return new IdentityDTO(identityDetails.getIdentity());
    }
}
