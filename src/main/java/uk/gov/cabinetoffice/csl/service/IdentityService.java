package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.TokenRequest;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class IdentityService {

    private final InviteService inviteService;
    private final AgencyTokenCapacityService agencyTokenCapacityService;
    private final IdentityRepository identityRepository;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final PasswordEncoder passwordEncoder;
    private final Utils utils;

    @Transactional(noRollbackFor = {UnableToAllocateAgencyTokenException.class, ResourceNotFoundException.class})
    public void createIdentityFromInviteCode(String code, String password, TokenRequest tokenRequest) {
        Invite invite = inviteService.getInviteForCode(code);
        String email = invite.getForEmail();
        final String domain = utils.getDomainFromEmailAddress(email);
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
        Identity identity = new Identity(randomUUID().toString(), email, passwordEncoder.encode(password),
                true, false, newRoles, now(), false, agencyTokenUid, 0);
        identityRepository.save(identity);
        log.debug("New identity email = {} successfully created", email);
    }

    public void reactivateIdentity(Identity identity, AgencyToken agencyToken) {
        identity.setActive(true);
        if (agencyToken != null && agencyToken.getUid() != null) {
            identity.setAgencyTokenUid(agencyToken.getUid());
        }
        identityRepository.save(identity);
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

    public boolean isAllowListedDomain(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase());
    }
}
