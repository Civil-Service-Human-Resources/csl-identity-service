package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.EmailUpdate;
import uk.gov.cabinetoffice.csl.domain.EmailUpdateStatus;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.domain.ReactivationStatus;
import uk.gov.cabinetoffice.csl.dto.VerificationCodeDetermination;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.cabinetoffice.csl.dto.VerificationCodeType.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class VerificationCodeDeterminationServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "jFwK/MPj+mHqdD4q7KhcBoqjYkH96N8FTcMlxsaVuJ4=";
    private static final String encryptionKey = "0123456789abcdef0123456789abcdef";

    private VerificationCodeDeterminationService verificationCodeDeterminationService;

    @Mock
    private EmailUpdateService emailUpdateService;

    @Mock
    private ReactivationService reactivationService;

    @Mock
    private IdentityService identityService;

    @BeforeEach
    public void setUp() {
        verificationCodeDeterminationService = new VerificationCodeDeterminationService(
                encryptionKey, emailUpdateService, reactivationService, identityService);
    }

    @Test
    public void shouldReturnReactivationVerificationCodeType() {
        Reactivation pendingReactivation = new Reactivation();
        pendingReactivation.setEmail(EMAIL);
        pendingReactivation.setCode(CODE);
        pendingReactivation.setReactivationStatus(ReactivationStatus.PENDING);

        when(reactivationService.isPendingReactivationExistsForCode(CODE)).thenReturn(true);
        when(reactivationService.getReactivationForCodeAndStatus(CODE, ReactivationStatus.PENDING)).thenReturn(pendingReactivation);

        VerificationCodeDetermination codeType = verificationCodeDeterminationService.getCodeType(CODE);
        assertEquals(codeType.getVerificationCodeType(), REACTIVATION);
        assertThat(codeType.getEmail(), equalTo(EMAIL));
    }

    @Test
    public void shouldReturnEmailUpdateVerificationCodeType() {
        EmailUpdate emailUpdate = new EmailUpdate();
        emailUpdate.setCode(CODE);
        emailUpdate.setNewEmail(EMAIL);
        emailUpdate.setEmailUpdateStatus(EmailUpdateStatus.PENDING);

        when(reactivationService.isPendingReactivationExistsForCode(CODE)).thenReturn(false);
        when(emailUpdateService.isEmailUpdateRequestExistsForCode(CODE)).thenReturn(true);
        when(emailUpdateService.getEmailUpdateRequestForCode(CODE)).thenReturn(emailUpdate);

        VerificationCodeDetermination codeType = verificationCodeDeterminationService.getCodeType(CODE);
        assertEquals(codeType.getVerificationCodeType(), EMAIL_UPDATE);
        assertThat(codeType.getEmail(), equalTo(EMAIL));
    }

    @Test
    public void shouldReturnAssignAgencyTokenVerificationCodeType() {
        when(reactivationService.isPendingReactivationExistsForCode(CODE)).thenReturn(false);
        when(emailUpdateService.isEmailUpdateRequestExistsForCode(CODE)).thenReturn(false);
        when(identityService.isIdentityExistsForEmail(EMAIL)).thenReturn(true);

        VerificationCodeDetermination codeType = verificationCodeDeterminationService.getCodeType(CODE);
        assertEquals(codeType.getVerificationCodeType(), ASSIGN_AGENCY_TOKEN);
        assertThat(codeType.getEmail(), equalTo(EMAIL));
    }
}
