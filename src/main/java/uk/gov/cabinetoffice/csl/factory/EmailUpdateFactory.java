package uk.gov.cabinetoffice.csl.factory;

import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;

import static java.time.Clock.systemDefaultZone;
import static java.time.LocalDateTime.now;
import static uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus.PENDING;

@Component
public class EmailUpdateFactory {
    public EmailUpdate create(Identity identity, String email) {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setIdentity(identity);
        emailUpdate.setEmail(email);
        emailUpdate.setRequestedAt(now(systemDefaultZone()));
        emailUpdate.setEmailUpdateStatus(PENDING);
        return emailUpdate;
    }
}
