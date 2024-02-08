package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.ReadOnlyProperty;
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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final String updatePasswordEmailTemplateId;
    private final InviteService inviteService;
    private final AgencyTokenCapacityService agencyTokenCapacityService;
    private final NotifyService notifyService;
    private final IdentityRepository identityRepository;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(@Value("${govNotify.template.passwordUpdate}") String updatePasswordEmailTemplateId,
                       InviteService inviteService,
                       AgencyTokenCapacityService agencyTokenCapacityService,
                       @Qualifier("notifyServiceImpl") NotifyService notifyService,
                       IdentityRepository identityRepository,
                       ICivilServantRegistryClient civilServantRegistryClient,
                       PasswordEncoder passwordEncoder) {
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
        Identity identity = identityRepository.findFirstByEmailEquals(username);
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

    @ReadOnlyProperty
    public boolean existsByEmail(String email) {
        return identityRepository.existsByEmail(email);
    }

    @Transactional(noRollbackFor = {UnableToAllocateAgencyTokenException.class, ResourceNotFoundException.class})
    public void createIdentityFromInviteCode(String code, String password, TokenRequest tokenRequest) {
        Invite invite = inviteService.findByCode(code);
        final String domain = getDomainFromEmailAddress(invite.getForEmail());

        Set<Role> newRoles = new HashSet<>(invite.getForRoles());

        String agencyTokenUid = null;
        if (requestHasTokenData(tokenRequest)) {
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
        } else if (!isAllowListedDomain(domain) && !isEmailInvitedViaIDM(invite.getForEmail())) {
            log.info("Invited request neither agency, nor allowListed, nor invited via IDM: {}", invite);
            throw new ResourceNotFoundException();
        }

        Identity identity = new Identity(
                UUID.randomUUID().toString(),
                invite.getForEmail(),
                passwordEncoder.encode(password),
                true,
                false,
                newRoles,
                Instant.now(),
                false,
                agencyTokenUid,
                0);

        identityRepository.save(identity);

        log.debug("New identity email = {} successfully created", identity.getEmail());
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

    public void lockIdentity(String email) {
        Identity identity = identityRepository.findFirstByActiveTrueAndEmailEquals(email);
        identity.setLocked(true);
        identityRepository.save(identity);
    }

    public boolean checkPassword(String username, String password) {
        Identity identity = identityRepository.findFirstByEmailEquals(username);
        return passwordEncoder.matches(password, identity.getPassword());
    }

    public boolean checkEmailExists(String email) {
        return identityRepository.existsByEmail(email);
    }

    public Identity loginSucceeded(Identity identity) {
        identity.setLastLoggedIn(Instant.now());
        identity.setFailedLoginAttempts(0);
        return identityRepository.save(identity);
    }

    public void loginFailed(String email) {
        log.info("UserService:loginFailed: {}", email);
        //TODO: implement below code
        // 1. fetch the identity from DB for the given email id
        // 2. check if identity is not null
        // 3. increase the failedLoginAttempts in identity
        // 4. check if failedLoginAttempts is equal or more than the account.lockout.maxLoginAttempts
        // 5. if yes for 3 then set locked to true in identity
        // 6. save the identity
        // 7. if identity is locked then throw AuthenticationException("User account is locked")
//        if (existsByEmail(email)) {
//            incrementAttempts(email);
//            if (areAttemptsMoreThanAllowedLimit(email)) {
//                lockIdentity(email);
//                throw new AuthenticationException("User account is locked") {};
//            }
//        }
    }

    public String getDomainFromEmailAddress(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1);
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

    public boolean isAllowListedDomain(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase());
    }

    private boolean requestHasTokenData(TokenRequest tokenRequest) {
        return isNotBlank(tokenRequest.getDomain())
                && isNotBlank(tokenRequest.getToken())
                && isNotBlank(tokenRequest.getOrg());
    }

    private boolean isEmailInvitedViaIDM(String email) {
        return inviteService.isEmailInvited(email);
    }
}
