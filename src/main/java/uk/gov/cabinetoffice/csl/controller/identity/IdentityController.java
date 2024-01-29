package uk.gov.cabinetoffice.csl.controller.identity;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.service.auth2.IUserAuthService;

@AllArgsConstructor
@RestController
public class IdentityController {

    private final IUserAuthService userAuthService;

    @GetMapping("/identity/resolve")
    public IdentityDTO resolveIdentity() {
        return userAuthService.resolveIdentity();
    }
}
