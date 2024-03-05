package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Identity;
import uk.gov.cabinetoffice.csl.domain.Reactivation;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.exception.IdentityNotFoundException;
import uk.gov.cabinetoffice.csl.exception.ResourceNotFoundException;
import uk.gov.cabinetoffice.csl.repository.ReactivationRepository;

import java.time.*;
import java.util.ArrayList;
import java.util.Optional;

import static java.time.Month.FEBRUARY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.ReactivationStatus.*;

@SpringBootTest
@ActiveProfiles("no-redis")
public class ReactivationServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "code";
    private static final String UID = "UID";

    private final IdentityService identityService = mock (IdentityService.class);
    private final ReactivationRepository reactivationRepository = mock(ReactivationRepository.class);
    private final int validityInSeconds = 86400;
    private final Clock clock = Clock.fixed(Instant.parse("2024-03-01T00:00:00Z"), ZoneId.of("UTC"));

    private final ReactivationService reactivationService =
            new ReactivationService(identityService, reactivationRepository, clock, validityInSeconds);

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
        assertEquals(REACTIVATED, reactivationArgumentCaptorValue.getReactivationStatus());
    }

    @Test
    public void shouldThrowIdentityNotFoundExceptionIfIdentityNotFound() {
        Reactivation reactivation = new Reactivation();
        reactivation.setEmail(EMAIL);
        reactivation.setCode(CODE);

        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);

        doThrow(new IdentityNotFoundException("Identity not found"))
                .when(identityService).getIdentityForEmailAndActiveFalse(EMAIL);

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
        reactivation.setReactivationStatus(PENDING);

        when(reactivationRepository
                .findFirstByCodeAndReactivationStatus(CODE, PENDING))
                .thenReturn(Optional.of(reactivation));

        assertEquals(reactivation, reactivationService.getReactivationForCodeAndStatus(CODE, PENDING));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionIfReactivationDoesNotExist() {
        when(reactivationRepository
                .findFirstByCodeAndReactivationStatus(CODE, PENDING))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reactivationService.getReactivationForCodeAndStatus(CODE, PENDING));
    }

    @Test
    public void isPendingReactivationExistsForEmailReturnsFalseIfPendingReactivationExpired() {
        String email = "my.name@myorg.gov.uk";
        LocalDateTime dateOfReactivationRequest = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        Reactivation reactivation = new Reactivation();
        reactivation.setCode(CODE);
        reactivation.setReactivationStatus(PENDING);
        reactivation.setEmail(email);
        reactivation.setRequestedAt(dateOfReactivationRequest);

        ArrayList<Reactivation> reactivations = new ArrayList<>();
        reactivations.add(reactivation);

        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(eq(email), eq(PENDING)))
                .thenReturn(reactivations);

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(email));
    }

    @Test
    public void isPendingExistsByEmailReturnsFalseIfNoPendingReactivationExistForEmail(){
        String email = "my.name@myorg.gov.uk";

        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(eq(email), eq(PENDING)))
                .thenReturn(new ArrayList<>());

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(email));
    }
}
