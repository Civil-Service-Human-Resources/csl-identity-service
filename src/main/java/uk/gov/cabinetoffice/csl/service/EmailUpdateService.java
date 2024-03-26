package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.factory.EmailUpdateFactory;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.repository.EmailUpdateRepository;
import uk.gov.cabinetoffice.csl.service.client.csrs.ICivilServantRegistryClient;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import static java.time.Clock.systemDefaultZone;
import static java.time.LocalDateTime.now;
import static uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus.UPDATED;

@Slf4j
@Service
@Transactional
public class EmailUpdateService {

    private final EmailUpdateRepository emailUpdateRepository;
    private final EmailUpdateFactory emailUpdateFactory;
    private final NotifyService notifyService;
    private final ICivilServantRegistryClient civilServantRegistryClient;
    private final IdentityService identityService;
    private final Clock clock;
    private final int validityInSeconds;

    @Value("${govNotify.template.emailUpdate}")
    private String updateEmailTemplateId;

    @Value("${emailUpdate.urlFormat}")
    private String inviteUrlFormat;

    public EmailUpdateService(EmailUpdateRepository emailUpdateRepository,
                              EmailUpdateFactory emailUpdateFactory,
                              @Qualifier("notifyServiceImpl") NotifyService notifyService,
                              ICivilServantRegistryClient civilServantRegistryClient,
                              IdentityService identityService,
                              Clock clock,
                              @Value("${emailUpdate.validityInSeconds}") int validityInSeconds) {
        this.emailUpdateRepository = emailUpdateRepository;
        this.emailUpdateFactory = emailUpdateFactory;
        this.notifyService = notifyService;
        this.civilServantRegistryClient = civilServantRegistryClient;
        this.identityService = identityService;
        this.clock = clock;
        this.validityInSeconds = validityInSeconds;
    }

    public void saveEmailUpdateAndNotify(Identity identity, String email) {
        EmailUpdate emailUpdate = emailUpdateFactory.create(identity, email);
        emailUpdateRepository.save(emailUpdate);
        String activationUrl = String.format(inviteUrlFormat, emailUpdate.getCode());
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("activationUrl", activationUrl);
        notifyService.notifyWithPersonalisation(email, updateEmailTemplateId, personalisation);
    }

    public boolean isEmailUpdateRequestExistsForCode(String code) {
        return emailUpdateRepository.existsByCode(code);
    }

    public EmailUpdate getEmailUpdateRequestForCode(String code) {
        return emailUpdateRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Email update entry not found in database"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEmailAddress(EmailUpdate emailUpdate) {
        updateEmailAddress(emailUpdate, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEmailAddress(EmailUpdate emailUpdate, AgencyToken agencyToken) {
        Identity emailUpdateIdentity = emailUpdate.getIdentity();
        Identity existingIdentity = identityService.getIdentityForEmail(emailUpdateIdentity.getEmail());
        String existingEmail = existingIdentity.getEmail();

        String newEmail = emailUpdate.getNewEmail();

        log.debug("Updating email address for: oldEmail = {}, newEmail = {}", existingEmail, newEmail);
        identityService.updateEmailAddress(existingIdentity, newEmail, agencyToken);
        civilServantRegistryClient.removeOrganisationalUnitFromCivilServant(emailUpdate.getIdentity().getUid());

        emailUpdate.setUpdatedAt(now(systemDefaultZone()));
        emailUpdate.setEmailUpdateStatus(UPDATED);
        log.info("Saving the emailUpdate in DB: {}", emailUpdate);
        emailUpdateRepository.save(emailUpdate);

        log.info("Email address {} has been updated to {} successfully", existingEmail, newEmail);
    }
}
