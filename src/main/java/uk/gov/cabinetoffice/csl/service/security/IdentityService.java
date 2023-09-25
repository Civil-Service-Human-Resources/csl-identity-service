package uk.gov.cabinetoffice.csl.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.exception.AccountDeactivatedException;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.PendingReactivationExistsException;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

import java.time.Instant;

@Slf4j
@Service
@Transactional
public class IdentityService implements UserDetailsService {

    private final IdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(IdentityRepository identityRepository, PasswordEncoder passwordEncoder) {
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Identity identity = identityRepository.findFirstByEmailEquals(username);
        if (identity == null) {
            throw new UsernameNotFoundException("No user found with email address " + username);
        } else if (!identity.isActive()) {
            //TODO: Implement as part of the future tickets
            //boolean pendingReactivationExistsForAccount = reactivationService.pendingExistsByEmail(identity.getEmail());
            boolean pendingReactivationExistsForAccount = false;
            if(pendingReactivationExistsForAccount){
                throw new PendingReactivationExistsException("Pending reactivation already exists for user");
            }
            throw new AccountDeactivatedException("User account is deactivated");
        }
        return new IdentityDetails(identity);
    }

    @ReadOnlyProperty
    public boolean existsByEmail(String email) {
        return identityRepository.existsByEmail(email);
    }

    public void updatePassword(Identity identity, String password) {
        identity.setActive(true);
        identity.setDeletionNotificationSent(false);
        identity.setPassword(passwordEncoder.encode(password));
        identity.setLocked(false);
        identityRepository.save(identity);
    }

    public void lockIdentity(String email) {
        Identity identity = identityRepository.findFirstByActiveTrueAndEmailEquals(email);
        identity.setLocked(true);
        identityRepository.save(identity);
    }

    public boolean checkPassword(String username, String password) {
        UserDetails userDetails = loadUserByUsername(username);
        return passwordEncoder.matches(password, userDetails.getPassword());
    }

    public boolean checkEmailExists(String email) {
        return identityRepository.existsByEmail(email);
    }

    public Identity setLastLoggedIn(Instant datetime, Identity identity) {
        identity.setLastLoggedIn(datetime);
        return identityRepository.save(identity);
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }

    private boolean hasData(String s) {
        return s != null && s.length() > 0;
    }

    public Identity getIdentityByEmail(String email) {
        return identityRepository.findFirstByEmailEquals(email);
    }

    public Identity getIdentityByEmailAndActiveFalse(String email) {
        return identityRepository
                .findFirstByActiveFalseAndEmailEquals(email)
                .orElseThrow(
                        () -> new IdentityNotFoundException("Identity not found for email: " + email));
    }
}
