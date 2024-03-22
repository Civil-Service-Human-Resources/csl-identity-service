package uk.gov.cabinetoffice.csl.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.dto.VerificationCodeDetermination;
import uk.gov.cabinetoffice.csl.exception.VerificationCodeTypeNotFound;

import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;
import static uk.gov.cabinetoffice.csl.dto.VerificationCodeType.*;

@AllArgsConstructor
@Service
public class VerificationCodeDeterminationService {

    private final EmailUpdateService emailUpdateService;

    private final ReactivationService reactivationService;

    public VerificationCodeDetermination getCodeType(String code) {
        if(reactivationService.isPendingReactivationExistsForCode(code)) {
            Reactivation reactivation = reactivationService.getReactivationForCodeAndStatus(code, PENDING);
            return new VerificationCodeDetermination(reactivation.getEmail(), REACTIVATION);
        } else if(emailUpdateService.existsByCode(code)) {
            EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateByCode(code);
            return new VerificationCodeDetermination(emailUpdate.getEmail(), EMAIL_UPDATE);
        } else {
            throw new VerificationCodeTypeNotFound(String.format("Verification code type not found for code: %s", code));
        }
    }
}
