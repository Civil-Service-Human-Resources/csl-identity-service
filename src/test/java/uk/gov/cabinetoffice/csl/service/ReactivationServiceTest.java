package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.domain.ReactivationStatus;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.repository.ReactivationRepository;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class ReactivationServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "code";
    private static final String UID = "UID";

    @Mock
    private ReactivationRepository reactivationRepository;

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private ReactivationService reactivationService;

    @Test
    public void shouldReactivateIdentity() {
        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(EMAIL);
        reactivation.setCode(CODE);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);

        Identity identity = new Identity();

        ArgumentCaptor<Reactivation> reactivationArgumentCaptor = ArgumentCaptor.forClass(Reactivation.class);

        when(identityService.getIdentityForEmailAndActiveFalse(EMAIL)).thenReturn(identity);
        doNothing().when(identityService).reactivateIdentity(identity, agencyToken);

        reactivationService.reactivateIdentity(reactivation, agencyToken);

        verify(reactivationRepository).save(reactivationArgumentCaptor.capture());

        Reactivation reactivationArgumentCaptorValue = reactivationArgumentCaptor.getValue();
        assertEquals(ReactivationStatus.REACTIVATED, reactivationArgumentCaptorValue.getReactivationStatus());
    }

    @Test
    public void shouldThrowExceptionIfIdentityNotFound() {
        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(EMAIL);
        reactivation.setCode(CODE);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);

        doThrow(new IdentityNotFoundException("Identity not found")).when(identityService).getIdentityForEmailAndActiveFalse(EMAIL);

        Exception exception = assertThrows(IdentityNotFoundException.class,
                () -> reactivationService.reactivateIdentity(reactivation, agencyToken));

        String expectedExceptionMessage = "Identity not found";
        String actualExceptionMessage = exception.getMessage();
        assertTrue(actualExceptionMessage.contains(expectedExceptionMessage));
    }

    @Test
    public void shouldGetReactivationByCodeAndStatus() {
        Reactivation reactivation = new Reactivation();
        reactivation.setCode(CODE);

        when(reactivationRepository
                .findFirstByCodeAndReactivationStatusEquals(CODE, ReactivationStatus.PENDING))
                .thenReturn(Optional.of(reactivation));

        assertEquals(reactivation, reactivationService.getReactivationForCodeAndStatus(CODE, ReactivationStatus.PENDING));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionIfReactivationDoesNotExist() {
        when(reactivationRepository
                .findFirstByCodeAndReactivationStatusEquals(CODE, ReactivationStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reactivationService.getReactivationForCodeAndStatus(CODE, ReactivationStatus.PENDING));
    }

    @Test
    public void pendingExistsByEmailReturnsTrueIfPendingReactivationExistForEmail(){
        String email = "my.name@myorg.gov.uk";

        when(reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(eq(email), eq(ReactivationStatus.PENDING), any(Date.class)))
                .thenReturn(true);

        assertTrue(reactivationService.isPendingReactivationExistsForEmail(email));
    }

    @Test
    public void pendingExistsByEmailReturnsFalseIfNoPendingReactivationExistForEmail(){
        String email = "my.name@myorg.gov.uk";

        when(reactivationRepository.existsByEmailIgnoreCaseAndReactivationStatusEqualsAndRequestedAtAfter(eq(email), eq(ReactivationStatus.PENDING), any(Date.class)))
                .thenReturn(false);

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(email));
    }
}
