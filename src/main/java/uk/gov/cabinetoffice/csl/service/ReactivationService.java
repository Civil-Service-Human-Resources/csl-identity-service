package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.domain.ReactivationStatus;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.repository.ReactivationRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class ReactivationService {

    private final ReactivationRepository reactivationRepository;
    private final IdentityService identityService;

    public Reactivation getReactivationForCodeAndStatus(String code, ReactivationStatus reactivationStatus) {
        return reactivationRepository
                .findFirstByCodeAndReactivationStatusEquals(code, reactivationStatus)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public boolean isPendingReactivationExistsForEmail(String email){
        return reactivationRepository
                .existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(email,
                        PENDING, getDateOneDayAgo());
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
        reactivation.setReactivatedAt(new Date());
        reactivationRepository.save(reactivation);
        log.info("Reactivation status updated to {} for email: {}", REACTIVATED, email);
    }

    public Reactivation createPendingReactivation(String email){
        String reactivationCode = RandomStringUtils.random(40, true, true);
        Reactivation reactivation = new Reactivation(reactivationCode, PENDING, new Date(), email);
        return reactivationRepository.save(reactivation);
    }

    //TODO: this method is not required
    private Date getDateOneDayAgo(){
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return Date.from(oneDayAgo.toInstant(ZoneOffset.UTC));
    }
}
