package uk.gov.cabinetoffice.csl.factory;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;

import java.time.Clock;

import static java.time.LocalDateTime.now;
import static uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus.PENDING;

@Component
@AllArgsConstructor
public class EmailUpdateFactory {

    private final Clock clock;

    public EmailUpdate create(Identity identity, String newEmail) {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setPreviousEmail(identity.getEmail());
        emailUpdate.setNewEmail(newEmail);
        emailUpdate.setRequestedAt(now(clock));
        emailUpdate.setEmailUpdateStatus(PENDING);
        emailUpdate.setIdentity(identity);
        return emailUpdate;
    }
}
