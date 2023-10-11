package uk.gov.cabinetoffice.csl.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@RestController
public class AuthenticationController {

    @GetMapping("/identity/resolve")
    public IdentityDTO resolveIdentity(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getClaim("email");
        String uid = jwt.getClaim("user_name");
        Set<String> roles = new HashSet<>();
        if (jwt.getClaim("authorities") instanceof List) {
            roles.addAll(jwt.getClaim("authorities"));
        }
        return new IdentityDTO(email, uid, roles);
    }
}
