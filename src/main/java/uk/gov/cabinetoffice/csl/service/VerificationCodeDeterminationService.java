package uk.gov.cabinetoffice.csl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.dto.VerificationCodeDetermination;
import uk.gov.cabinetoffice.csl.exception.VerificationCodeTypeNotFound;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.PENDING;
import static uk.gov.cabinetoffice.csl.dto.VerificationCodeType.*;
import static uk.gov.cabinetoffice.csl.util.TextEncryptionUtils.getDecryptedText;

@Service
public class VerificationCodeDeterminationService {

    @Value("${textEncryption.encryptionKey}")
    private String encryptionKey;

    private final EmailUpdateService emailUpdateService;

    private final ReactivationService reactivationService;

    private final IdentityService identityService;

    public VerificationCodeDeterminationService(EmailUpdateService emailUpdateService,
                                                ReactivationService reactivationService,
                                                IdentityService identityService) {
        this.emailUpdateService = emailUpdateService;
        this.reactivationService = reactivationService;
        this.identityService = identityService;
    }

    public VerificationCodeDetermination getCodeType(String code) {
        if(reactivationService.isPendingReactivationExistsForCode(code)) {
            Reactivation reactivation = reactivationService.getReactivationForCodeAndStatus(code, PENDING);
            return new VerificationCodeDetermination(reactivation.getEmail(), REACTIVATION);
        } else if(emailUpdateService.isEmailUpdateRequestExistsForCode(code)) {
            EmailUpdate emailUpdate = emailUpdateService.getEmailUpdateRequestForCode(code);
            return new VerificationCodeDetermination(emailUpdate.getNewEmail(), EMAIL_UPDATE);
        } else {
            String encryptedEmail = decode(code, UTF_8);
            String email = getDecryptedText(encryptedEmail, encryptionKey);
            if(identityService.isIdentityExistsForEmail(email)) {
                return new VerificationCodeDetermination(email, ASSIGN_AGENCY_TOKEN);
            }
            throw new VerificationCodeTypeNotFound(String.format("Verification code type not found for code: %s", code));
        }
    }
}
