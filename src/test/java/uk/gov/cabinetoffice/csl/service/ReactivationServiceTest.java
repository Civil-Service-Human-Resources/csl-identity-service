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

    private final IdentityService identityService = mock(IdentityService.class);
    private final CSLService cslService = mock(CSLService.class);
    private final ReactivationRepository reactivationRepository = mock(ReactivationRepository.class);
    private final int validityInSeconds = 86400;
    private final Clock clock = Clock.fixed(Instant.parse("2024-03-01T00:00:00Z"), ZoneId.of("Europe/London"));

    private final ReactivationService reactivationService =
            new ReactivationService(identityService, cslService, reactivationRepository, clock, validityInSeconds);

    @Test
    public void shouldReactivateIdentity() {
        Reactivation reactivation = createPendingReactivation();

        AgencyToken agencyToken = createAgencyToken();

        Identity identity = new Identity();
        identity.setUid(UID);

        ArgumentCaptor<Reactivation> reactivationArgumentCaptor = ArgumentCaptor.forClass(Reactivation.class);

        when(identityService.getInactiveIdentityForEmail(EMAIL)).thenReturn(identity);
        doNothing().when(identityService).reactivateIdentity(identity, agencyToken);
        doNothing().when(cslService).identityActivated(identity.getUid());

        reactivationService.reactivateIdentity(reactivation, agencyToken);

        verify(reactivationRepository).save(reactivationArgumentCaptor.capture());

        Reactivation reactivationArgumentCaptorValue = reactivationArgumentCaptor.getValue();
        assertEquals(REACTIVATED, reactivationArgumentCaptorValue.getReactivationStatus());
    }

    @Test
    public void shouldThrowIdentityNotFoundExceptionIfIdentityNotFound() {
        Reactivation reactivation = createPendingReactivation();

        AgencyToken agencyToken = createAgencyToken();

        doThrow(new IdentityNotFoundException("Identity not found"))
                .when(identityService).getInactiveIdentityForEmail(EMAIL);

        Exception exception = assertThrows(IdentityNotFoundException.class,
                () -> reactivationService.reactivateIdentity(reactivation, agencyToken));

        String expectedExceptionMessage = "Identity not found";
        String actualExceptionMessage = exception.getMessage();
        assertTrue(actualExceptionMessage.contains(expectedExceptionMessage));
    }

    @Test
    public void shouldGetReactivationByCodeAndStatus() {
        Reactivation reactivation = createPendingReactivation();

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
        Reactivation reactivation = createPendingReactivation();

        ArrayList<Reactivation> reactivations = new ArrayList<>();
        reactivations.add(reactivation);

        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(eq(EMAIL), eq(PENDING)))
                .thenReturn(reactivations);

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(EMAIL));
    }

    @Test
    public void isPendingExistsByEmailReturnsFalseIfNoPendingReactivationExistForEmail(){
        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatus(eq(EMAIL), eq(PENDING)))
                .thenReturn(new ArrayList<>());

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(EMAIL));
    }

    private Reactivation createPendingReactivation() {
        LocalDateTime requestedAt = LocalDateTime.of(2024, FEBRUARY, 1, 11, 30);
        Reactivation reactivation = new Reactivation();
        reactivation.setCode(CODE);
        reactivation.setReactivationStatus(PENDING);
        reactivation.setEmail(EMAIL);
        reactivation.setRequestedAt(requestedAt);
        return reactivation;
    }

    private AgencyToken createAgencyToken() {
        AgencyToken agencyToken = new AgencyToken();
        agencyToken.setUid(UID);
        return agencyToken;
    }
}
