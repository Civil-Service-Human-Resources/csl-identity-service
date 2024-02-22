package uk.gov.cabinetoffice.csl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.repository.IdentityRepository;

@Service
public class PasswordService {

    private final String updatePasswordEmailTemplateId;
    private final NotifyService notifyService;
    private final IdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(@Value("${govNotify.template.passwordUpdate}") String updatePasswordEmailTemplateId,
                           @Qualifier("notifyServiceImpl") NotifyService notifyService,
                           IdentityRepository identityRepository,
                           PasswordEncoder passwordEncoder) {
        this.updatePasswordEmailTemplateId = updatePasswordEmailTemplateId;
        this.notifyService = notifyService;
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
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

    public boolean isPasswordMatches(String username, String password) {
        Identity identity = identityRepository.findFirstByEmailEqualsIgnoreCase(username);
        return passwordEncoder.matches(password, identity.getPassword());
    }
}
