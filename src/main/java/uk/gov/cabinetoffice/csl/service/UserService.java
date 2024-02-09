package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.IdentityDetails;
import uk.gov.cabinetoffice.csl.exception.*;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final int maxLoginAttempts;
    private final String updatePasswordEmailTemplateId;
    private final InviteService inviteService;
    private final AgencyTokenCapacityService agencyTokenCapacityService;
    private final NotifyService notifyService;
    private final IdentityRepository identityRepository;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(@Value("${account.lockout.maxLoginAttempts}") int maxLoginAttempts,
                       @Value("${govNotify.template.passwordUpdate}") String updatePasswordEmailTemplateId,
                       InviteService inviteService,
                       AgencyTokenCapacityService agencyTokenCapacityService,
                       @Qualifier("notifyServiceImpl") NotifyService notifyService,
                       IdentityRepository identityRepository,
                       ICivilServantRegistryClient civilServantRegistryClient,
                       PasswordEncoder passwordEncoder) {
        this.maxLoginAttempts = maxLoginAttempts;
        this.updatePasswordEmailTemplateId = updatePasswordEmailTemplateId;
        this.inviteService = inviteService;
        this.agencyTokenCapacityService = agencyTokenCapacityService;
        this.notifyService = notifyService;
        this.identityRepository = identityRepository;
        this.civilServantRegistryClient = civilServantRegistryClient;
        this.passwordEncoder = passwordEncoder;
    }

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

    @Transactional(noRollbackFor = {UnableToAllocateAgencyTokenException.class, ResourceNotFoundException.class})
    public void createIdentityFromInviteCode(String code, String password, TokenRequest tokenRequest) {
        Invite invite = inviteService.getInviteForCode(code);
        String email = invite.getForEmail();
        final String domain = getDomainFromEmailAddress(email);
        Set<Role> newRoles = new HashSet<>(invite.getForRoles());
        String agencyTokenUid = null;
        if (tokenRequest != null && tokenRequest.hasData()) {
            Optional<AgencyToken> agencyTokenForDomainTokenOrganisation =
                    civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(tokenRequest.getDomain(),
                            tokenRequest.getToken(), tokenRequest.getOrg());

            agencyTokenUid = agencyTokenForDomainTokenOrganisation
                    .map(agencyToken -> {
                        if (agencyTokenCapacityService.hasSpaceAvailable(agencyToken)) {
                            return agencyToken.getUid();
                        } else {
                            throw new UnableToAllocateAgencyTokenException("Agency token uid " +
                                    agencyToken.getUid() + " has no spaces available. Identity not created");
                        }
                    })
                    .orElseThrow(ResourceNotFoundException::new);

            log.info("Identity request has agency uid = {}", agencyTokenUid);
        } else if (!isAllowListedDomain(domain) && !inviteService.isEmailInvited(email)) {
            log.info("Invited request neither agency, nor allowListed, nor invited via IDM: {}", invite);
            throw new ResourceNotFoundException();
        }

        Identity identity = new Identity(
                UUID.randomUUID().toString(),
                email,
                passwordEncoder.encode(password),
                true,
                false,
                newRoles,
                Instant.now(),
                false,
                agencyTokenUid,
                0);

        identityRepository.save(identity);

        log.debug("New identity email = {} successfully created", email);
    }

    public void updatePasswordAndActivateAndUnlock(Identity identity, String password) {
        identity.setPassword(passwordEncoder.encode(password));
        identity.setActive(true);
        identity.setLocked(false);
        identity.setDeletionNotificationSent(false);
        identity.setFailedLoginAttempts(0);
        identityRepository.save(identity);
    }

    public void updatePasswordAndNotify(Identity identity, String password) {
        identity.setPassword(passwordEncoder.encode(password));
        identityRepository.save(identity);
        notifyService.notify(identity.getEmail(), updatePasswordEmailTemplateId);
    }

    public boolean checkPassword(String username, String password) {
        Identity identity = identityRepository.findFirstByEmailEqualsIgnoreCase(username);
        return passwordEncoder.matches(password, identity.getPassword());
    }

    @ReadOnlyProperty
    public boolean isIdentityExistsForEmail(String email) {
        return identityRepository.existsByEmailIgnoreCase(email);
    }

    public Identity getIdentityForEmail(String email) {
        return identityRepository.findFirstByEmailEqualsIgnoreCase(email);
    }

    public Identity getIdentityForEmailAndActiveFalse(String email) {
        return identityRepository
                .findFirstByActiveFalseAndEmailEqualsIgnoreCase(email)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found for email: " + email));
    }

    public Identity loginSucceeded(Identity identity) {
        log.debug("UserService.loginSucceeded: {}", identity);
        identity.setLastLoggedIn(Instant.now());
        identity.setFailedLoginAttempts(0);
        return identityRepository.save(identity);
    }

    public Identity loginFailed(String email) {
        log.debug("UserService:loginFailed: {}", email);
        Identity identity = identityRepository.findFirstByEmailEqualsIgnoreCase(email);
        if(identity != null && email.equalsIgnoreCase(identity.getEmail())) {
            Integer currentFailedLoginAttempts = identity.getFailedLoginAttempts();
            identity.setFailedLoginAttempts(currentFailedLoginAttempts + 1);
            if(identity.getFailedLoginAttempts() >= maxLoginAttempts) {
                identity.setLocked(true);
                identityRepository.save(identity);
                log.info("UserService:User account is locked for {}", email);
                throw new AuthenticationException("User account is locked") {};
            }
            identity = identityRepository.save(identity);
        }
        return identity;
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
    }

    public boolean isAllowListedDomain(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase());
    }
}
