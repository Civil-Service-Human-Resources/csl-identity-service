package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.AccountDeactivatedException;
import uk.gov.cabinetoffice.csl.exception.PendingReactivationExistsException;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private IdentityRepository identityRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Identity identity = identityRepository.findFirstByEmailEqualsIgnoreCase(username);
        if (identity == null) {
            throw new UsernameNotFoundException("No user found with email address: " + username);
        } else if (!identity.isActive()) {
            //TODO: To be implemented as part of the future tickets
            //boolean pendingReactivationExistsForAccount = reactivationService.pendingExistsByEmail(identity.getEmail());
            boolean pendingReactivationExistsForAccount = false;
            if(pendingReactivationExistsForAccount){
                throw new PendingReactivationExistsException("Pending reactivation already exists for user: " + username);
            }
            throw new AccountDeactivatedException("User account is deactivated for user: " + username);
        }
        return new IdentityDetails(identity);
    }
}
