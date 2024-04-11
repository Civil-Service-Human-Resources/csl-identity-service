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
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.exception.UnableToAllocateAgencyTokenException;
import uk.gov.cabinetoffice.csl.repository.CompoundRoles;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;
import uk.gov.cabinetoffice.csl.util.Utils;

import java.time.Clock;
import java.util.*;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
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
    private final CompoundRoles compoundRoles;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final PasswordEncoder passwordEncoder;
    private final Utils utils;
    private final Clock clock;

    @Transactional(noRollbackFor = {UnableToAllocateAgencyTokenException.class, ResourceNotFoundException.class})
    public void createIdentityFromInviteCode(String code, String password, AgencyToken agencyToken) {
        Invite invite = inviteService.getInviteForCode(code);
        String email = invite.getForEmail();
        final String domain = utils.getDomainFromEmailAddress(email);
        Set<Role> newRoles = new HashSet<>(invite.getForRoles());
        String agencyTokenUid = null;
        if (agencyToken != null && agencyToken.hasData()) {
            Optional<AgencyToken> agencyTokenForDomainTokenOrganisation =
                    civilServantRegistryClient.getAgencyTokenForDomainTokenOrganisation(agencyToken.getDomain(),
                            agencyToken.getToken(), agencyToken.getOrg());

            agencyTokenUid = agencyTokenForDomainTokenOrganisation
                    .map(at -> {
                        if (agencyTokenCapacityService.hasSpaceAvailable(at)) {
                            return at.getUid();
                        } else {
                            throw new UnableToAllocateAgencyTokenException("Agency token uid " +
                                    at.getUid() + " has no spaces available. Identity not created");
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
                true, false, newRoles, now(clock), false, agencyTokenUid, 0);
        identityRepository.save(identity);
        log.debug("New identity email = {} successfully created", email);
    }

    public BatchProcessResponse removeReportingRoles(List<String> uids) {
        return removeRoles(uids, CompoundRole.REPORTER);
    }

    public BatchProcessResponse removeRoles(List<String> uids, CompoundRole compoundRole) {
        return removeRoles(uids, singletonList(compoundRole));
    }

    public BatchProcessResponse removeRoles(List<String> uids, List<CompoundRole> compoundRoleList) {
        log.info(format("Removing %s access from the following users: %s", compoundRoleList, uids));
        BatchProcessResponse response = new BatchProcessResponse();
        List<Identity> identities = identityRepository.findIdentitiesByUids(uids);
        Collection<String> reportingRoles = compoundRoleList
                .stream()
                .flatMap(cr ->
                        compoundRoles.getRoles(cr)
                                .stream())
                .collect(toList());
        List<Identity> identitiesToSave = new ArrayList<>();
        identities.forEach(i -> {
            if (i.hasAnyRole(reportingRoles)) {
                i.removeRoles(reportingRoles);
                identitiesToSave.add(i);
            }
        });
        if (!identitiesToSave.isEmpty()) {
            log.info(format("%s access removed from the following users: %s", compoundRoleList, uids));
            identityRepository.saveAll(identitiesToSave);
            response.setSuccessfulIds(identitiesToSave.stream().map(Identity::getUid).collect(toList()));
        }
        return response;
    }

    public void updateEmailAddress(Identity identity, String email, AgencyToken newAgencyToken) {
        if (newAgencyToken != null && newAgencyToken.getUid() != null) {
            log.debug("Updating agency token for user: oldAgencyToken = {}, newAgencyToken = {}", identity.getAgencyTokenUid(), newAgencyToken.getUid());
            identity.setAgencyTokenUid(newAgencyToken.getUid());
        } else {
            log.debug("Setting existing agency token UID to null");
            identity.setAgencyTokenUid(null);
        }
        identity.setEmail(email);
        identity.removeRoles(compoundRoles.getRoles(Arrays.asList(
                CompoundRole.REPORTER,
                CompoundRole.UNRESTRICTED_ORGANISATION
        )));
        identityRepository.save(identity);
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

    public boolean isValidEmailDomain(String email) {
        final String domain = utils.getDomainFromEmailAddress(email);
        return (isAllowListedDomain(domain) || civilServantRegistryClient.isDomainInAgency(domain));
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
