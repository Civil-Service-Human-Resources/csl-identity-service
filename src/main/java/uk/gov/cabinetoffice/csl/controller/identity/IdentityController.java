package uk.gov.cabinetoffice.csl.controller.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.dto.IdentityDto;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class IdentityController {

    @GetMapping("/identity/resolve")
    public IdentityDto resolveIdentity(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getClaim("email");
        String uid = jwt.getClaim("user_name");
        Set<String> roles = new HashSet<>();
        if (jwt.getClaim("authorities") instanceof List) {
            roles.addAll(jwt.getClaim("authorities"));
        }
        return new IdentityDto(email, uid, roles);
    }
}
