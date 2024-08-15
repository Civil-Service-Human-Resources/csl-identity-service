package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
            if (!isUserValid(identity)) {
                throw new AccountBlockedException("User account is blocked");
            }
            if (!identity.isActive()) {
                if (reactivationService.isPendingReactivationExistsForEmail(identity.getEmail())) {
                    throw new PendingReactivationExistsException("Pending reactivation exists for user");
                }
                throw new AccountDeactivatedException("User account is deactivated");
            }
        }
        return new IdentityDetails(identity);
    }

    private boolean isUserValid(Identity identity) {
        String email = identity.getEmail();
        String domain = utils.getDomainFromEmailAddress(email);
        String agencyTokenUid = identity.getAgencyTokenUid();
        if (isEmailInvited(email)) {
            log.debug(String.format("User %s has a valid invite from another user", email));
            return true;
        }
        if (agencyTokenUid != null) {
            log.debug(String.format("Checking domain %s against agency token %s for user %s", domain, agencyTokenUid, email));
            return identityService.isAgencyTokenUidValidForDomain(agencyTokenUid, domain);
        }
        log.debug(String.format("Checking domain %s against allowlist for user %s", domain, email));
        return isAllowListedDomain(domain);
    }

    private boolean isAllowListedDomain(String domain) {
        return identityService.isDomainAllowListed(domain);
    }

    private boolean isEmailInvited(String email) {
        return identityService.isEmailInvited(email);
    }
}
