package uk.gov.cabinetoffice.csl.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.*;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.repository.ReactivationRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.Clock.systemDefaultZone;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;

@Slf4j
@Service
@Transactional
public class ReactivationService {

    private final IdentityService identityService;
    private final ReactivationRepository reactivationRepository;
    private final Clock clock;
    private final int validityInSeconds;

    public ReactivationService(IdentityService identityService,
                               ReactivationRepository reactivationRepository,
                               Clock clock,
                               @Value("${reactivation.validityInSeconds}") int validityInSeconds) {
        this.identityService = identityService;
        this.reactivationRepository = reactivationRepository;
        this.clock = clock;
        this.validityInSeconds = validityInSeconds;
    }

    public Reactivation createPendingReactivation(String email){
        String reactivationCode = RandomStringUtils.random(40, true, true);
        Reactivation reactivation = new Reactivation(reactivationCode, PENDING, now(systemDefaultZone()), email);
        return saveReactivation(reactivation);
    }

    public void reactivateIdentity(Reactivation reactivation) {
        reactivateIdentity(reactivation, null);
    }

    public void reactivateIdentity(Reactivation reactivation, AgencyToken agencyToken)
            throws IdentityNotFoundException {
        String email = reactivation.getEmail();
        Identity identity = identityService.getIdentityForEmailAndActiveFalse(email);
        identityService.reactivateIdentity(identity, agencyToken);
        log.info("Identity reactivated for email: {}", email);
        reactivation.setReactivationStatus(REACTIVATED);
        reactivation.setReactivatedAt(now(systemDefaultZone()));
        saveReactivation(reactivation);
        log.info("Reactivation status updated to {} for email: {}", REACTIVATED, email);
    }

    public Reactivation getPendingReactivationForEmail(String email) {
        if(isPendingReactivationExistsForEmail(email)) {
            reactivationRepository.findFirstByEmailIgnoreCaseAndReactivationStatus(email, PENDING)
                    .orElseThrow(ResourceNotFoundException::new);
        }
        throw new ResourceNotFoundException();
    }

    public boolean isPendingReactivationExistsForEmail(String email) {
        List<Reactivation> pendingReactivations =
                reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(email, PENDING);

        if(pendingReactivations != null && pendingReactivations.size() > 1) {
            pendingReactivations.forEach(r -> r.setReactivationStatus(EXPIRED));
            reactivationRepository.saveAll(pendingReactivations);
            return false;
        }

        if(pendingReactivations != null && pendingReactivations.size() == 1) {
            return !isReactivationExpired(pendingReactivations.get(0));
        }

        return false;
    }

    public boolean isReactivationExpired(Reactivation reactivation) {
        if(reactivation.getReactivationStatus().equals(EXPIRED)) {
            return true;
        }

        if(reactivation.getReactivationStatus().equals(PENDING)) {
            long diffInMs = MILLIS.between(LocalDateTime.now(clock), reactivation.getRequestedAt());
            if(diffInMs > validityInSeconds * 1000L) {
                reactivation.setReactivationStatus(EXPIRED);
                saveReactivation(reactivation);
                return true;
            }
        }
        return false;
    }

    public Reactivation getReactivationForCodeAndStatus(String code, ReactivationStatus reactivationStatus) {
        return reactivationRepository
                .findFirstByCodeAndReactivationStatus(code, reactivationStatus)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public Reactivation saveReactivation(Reactivation reactivation) {
        return reactivationRepository.save(reactivation);
    }
}
