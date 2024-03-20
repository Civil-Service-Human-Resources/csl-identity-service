package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.BatchProcessResponse;
import uk.gov.cabinetoffice.csl.dto.IdentityDTO;
import uk.gov.cabinetoffice.csl.dto.TokenRequest;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.util.*;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class IdentityService {

    private final InviteService inviteService;
    private final AgencyTokenCapacityService agencyTokenCapacityService;
    private final IdentityRepository identityRepository;
    private final CompoundRoleRepository compoundRoleRepository;
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
                    .orElseThrow(() -> new ResourceNotFoundException("Agency token not found"));

            log.info("Identity request has agency uid = {}", agencyTokenUid);
        } else if (!isAllowListedDomain(domain) && !inviteService.isEmailInvited(email)) {
            log.info("Invited request neither agency, nor allowListed, nor invited via IDM: {}", invite);
            throw new ResourceNotFoundException("Invited request neither agency, nor allowListed, nor invited via IDM for email: "
                    + email);
        }
        Identity identity = new Identity(randomUUID().toString(), email, passwordEncoder.encode(password),
                true, false, newRoles, now(), false, agencyTokenUid, 0);
        identityRepository.save(identity);
        log.debug("New identity email = {} successfully created", email);
    }

    public BatchProcessResponse removeReportingRoles(List<String> uids) {
        log.info(format("Removing reporting access from the following users: %s", uids));
        BatchProcessResponse response = new BatchProcessResponse();
        List<Identity> identities = identityRepository.findIdentitiesByUids(uids);
        Collection<String> reportingRoles = compoundRoleRepository.getReportingRoles();
        List<Identity> identitiesToSave = new ArrayList<>();
        identities.forEach(i -> {
            if (i.hasAnyRole(reportingRoles)) {
                i.removeRoles(reportingRoles);
                identitiesToSave.add(i);
            }
        });
        if (!identitiesToSave.isEmpty()) {
            log.info(format("Reporting access removed from the following users: %s", uids));
            identityRepository.saveAll(identitiesToSave);
            response.setSuccessfulIds(identitiesToSave.stream().map(Identity::getUid).collect(toList()));
        }
        return response;
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

    public Identity getActiveIdentityForEmail(String email) {
        return identityRepository.findFirstByActiveTrueAndEmailEqualsIgnoreCase(email);
    }

    public Identity getInactiveIdentityForEmail(String email) {
        return identityRepository
                .findFirstByActiveFalseAndEmailEqualsIgnoreCase(email)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found for email: " + email));
    }

    public Identity getIdentityForUid(String uid) {
        return identityRepository
                .findFirstByUid(uid)
                .orElseThrow(() -> new IdentityNotFoundException("Identity not found for uid: " + uid));
    }

    public boolean isAllowListedDomain(String domain) {
        return civilServantRegistryClient.getAllowListDomains().contains(domain.toLowerCase());
    }

    public boolean isDomainInAgency(String domain) {
        return civilServantRegistryClient.isDomainInAgency(domain);
    }

    public boolean isEmailInvited(String email) {
        return inviteService.isEmailInvited(email);
    }

    public List<Identity> getAllIdentities() {
        return identityRepository.findAll();
    }

    public List<IdentityDTO> getAllNormalisedIdentities() {
        return identityRepository.findAllNormalised();
    }

    public List<IdentityDTO> getIdentitiesByUidsNormalised(List<String> uids) {
        return identityRepository.findIdentitiesByUidsNormalised(uids);
    }
}
