package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.AccountBlockedException;
import uk.gov.cabinetoffice.csl.exception.AccountDeactivatedException;
import uk.gov.cabinetoffice.csl.exception.PendingReactivationExistsException;
import uk.gov.cabinetoffice.csl.util.Utils;

@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private IdentityService identityService;
    private ReactivationService reactivationService;
    private Utils utils;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Identity identity = identityService.getIdentityForEmail(username);

        if (identity == null) {
            throw new UsernameNotFoundException("No user found with email address " + username);
        } else {
            String email = identity.getEmail();
            String domain = utils.getDomainFromEmailAddress(email);
            if (!isAllowListedDomain(domain)
                    && !isAgencyDomain(domain, identity)
                    && !isEmailInvited(email)) {
                throw new AccountBlockedException("User account is blocked");
            }
            if (!identity.isActive()) {
                if (reactivationService.isPendingReactivationExistsForEmail(identity.getEmail())) {
                    throw new PendingReactivationExistsException("Pending reactivation already exists for user");
                }
                throw new AccountDeactivatedException("User account is deactivated");
            }
        }
        return new IdentityDetails(identity);
    }

    private boolean isAllowListedDomain(String domain) {
        return identityService.isAllowListedDomain(domain);
    }

    private boolean isAgencyDomain(String domain, Identity identity) {
        return identityService.isDomainInAgency(domain) && identity.getAgencyTokenUid() != null;
    }

    private boolean isEmailInvited(String email) {
        return identityService.isEmailInvited(email);
    }
}
