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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import static java.util.Locale.ENGLISH;
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

    private final ReactivationService reactivationService =
            new ReactivationService(identityService, reactivationRepository, validityInSeconds);

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
                .findFirstByCodeAndReactivationStatusEquals(CODE, PENDING))
                .thenReturn(Optional.of(reactivation));

        assertEquals(reactivation, reactivationService.getReactivationForCodeAndStatus(CODE, PENDING));
    }

    @Test
    public void shouldThrowResourceNotFoundExceptionIfReactivationDoesNotExist() {
        when(reactivationRepository
                .findFirstByCodeAndReactivationStatusEquals(CODE, PENDING))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reactivationService.getReactivationForCodeAndStatus(CODE, PENDING));
    }

    @Test
    public void isPendingExistsByEmailReturnsFalseIfPendingReactivationExpired() throws ParseException {
        String email = "my.name@myorg.gov.uk";
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", ENGLISH);
        Date dateOfReactivationRequest = formatter.parse("01-Feb-2024");

        Reactivation reactivation = new Reactivation();
        reactivation.setCode(CODE);
        reactivation.setReactivationStatus(PENDING);
        reactivation.setEmail(email);
        reactivation.setRequestedAt(dateOfReactivationRequest);

        ArrayList<Reactivation> reactivations = new ArrayList<>();
        reactivations.add(reactivation);

        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatusEquals(eq(email), eq(PENDING)))
                .thenReturn(reactivations);

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(email));
    }

    @Test
    public void isPendingExistsByEmailReturnsFalseIfNoPendingReactivationExistForEmail(){
        String email = "my.name@myorg.gov.uk";

        when(reactivationRepository.findByEmailIgnoreCaseAndReactivationStatusEquals(eq(email), eq(PENDING)))
                .thenReturn(new ArrayList<>());

        assertFalse(reactivationService.isPendingReactivationExistsForEmail(email));
    }
}
