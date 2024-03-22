package uk.gov.cabinetoffice.csl.factory;

import org.springframework.stereotype.Component;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Identity;

import java.time.Instant;

@Component
public class EmailUpdateFactory {
    public EmailUpdate create(Identity identity, String email) {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setIdentity(identity);
        emailUpdate.setEmail(email);
        emailUpdate.setTimestamp(Instant.now());
        return emailUpdate;
    }
}