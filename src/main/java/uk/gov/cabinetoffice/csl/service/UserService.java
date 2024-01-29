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

@Slf4j
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final String updatePasswordEmailTemplateId;
    private final InviteService inviteService;
    private final AgencyTokenCapacityService agencyTokenCapacityService;
    private final TokenServices tokenServices;
    private final NotifyService notifyService;
    private final IdentityRepository identityRepository;
    private final TokenRepository tokenRepository;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(@Value("${govNotify.template.passwordUpdate}") String updatePasswordEmailTemplateId,
                       InviteService inviteService,
                       AgencyTokenCapacityService agencyTokenCapacityService,
                       TokenServices tokenServices,
                       @Qualifier("notifyServiceImpl") NotifyService notifyService,
                       IdentityRepository identityRepository,
                       @Qualifier("tokenRepository") TokenRepository tokenRepository,
                       ICivilServantRegistryClient civilServantRegistryClient,
                       PasswordEncoder passwordEncoder) {
        this.updatePasswordEmailTemplateId = updatePasswordEmailTemplateId;
        this.inviteService = inviteService;
        this.agencyTokenCapacityService = agencyTokenCapacityService;
        this.tokenServices = tokenServices;
        this.notifyService = notifyService;
        this.identityRepository = identityRepository;
        this.tokenRepository = tokenRepository;
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
                agencyTokenUid);

        identityRepository.save(identity);

        log.debug("New identity email = {} successfully created", identity.getEmail());
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
        Identity identity = identityRepository.findFirstByEmailEquals(username);
        return passwordEncoder.matches(password, identity.getPassword());
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

    public boolean isAllowListedDomain(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase());
    }

    public void updatePasswordAndRevokeTokens(Identity identity, String password) {
        identity.setPassword(passwordEncoder.encode(password));
        identityRepository.save(identity);
        revokeAccessTokens(identity);
        notifyService.notify(identity.getEmail(), updatePasswordEmailTemplateId);
    }

    public void revokeAccessTokens(Identity identity) {
        tokenRepository.findAllByUserName(identity.getUid())
                .forEach(token -> tokenServices.revokeToken(token.getToken().getValue()));
    }

    private boolean requestHasTokenData(TokenRequest tokenRequest) {
        return hasData(tokenRequest.getDomain())
                && hasData(tokenRequest.getToken())
                && hasData(tokenRequest.getOrg());
    }

    private boolean isEmailInvitedViaIDM(String email) {
        return inviteService.isEmailInvited(email);
    }
}
