package uk.gov.cabinetoffice.csl.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {

        SecurityContext context = SecurityContextHolder.createEmptyContext();

        IdentityDetails principal = TestUtil.createIdentityDetails(
                Long.valueOf(customUser.id()), customUser.uid(), customUser.email(), customUser.password());

        Authentication auth = UsernamePasswordAuthenticationToken
                        .authenticated(principal, "password", principal.getAuthorities());
        context.setAuthentication(auth);

        return context;
    }
}
