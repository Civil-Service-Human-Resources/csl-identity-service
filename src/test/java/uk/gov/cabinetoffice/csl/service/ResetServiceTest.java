package uk.gov.cabinetoffice.csl.service;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.domain.Reset;
import uk.gov.cabinetoffice.csl.repository.ResetRepository;
import uk.gov.service.notify.NotificationClientException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.*;
import static uk.gov.cabinetoffice.csl.domain.ResetStatus.PENDING;

@SpringBootTest
@ActiveProfiles("no-redis")
public class ResetServiceTest {

    public static final String EMAIL = "test@example.com";
    public static final String CODE = "abc123";
    public static final String TEMPLATE_ID = "template123";
    public static final String URL = "localhost:8080";
    private final int validityInSeconds = 86400;

    private final ResetRepository resetRepository = mock(ResetRepository.class);

    private final NotifyService notifyService = mock(NotifyService.class);

    private final ResetService resetService = new ResetService(resetRepository, notifyService, validityInSeconds);

    @Test
    public void shouldSaveNewResetWhenNoExistingPendingResetExists() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        List<Reset> existingPendingResets = new ArrayList<>();
        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        resetService.notifyForResetRequest(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(1)).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(reset.getCode(), not(equalTo(CODE)));
        MatcherAssert.assertThat(reset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(reset.getResetStatus(), equalTo(PENDING));
    }

    @Test
    public void shouldUseExistingResetWhenItIsPending() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset1 = createReset();
        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset1);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        resetService.notifyForResetRequest(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(1)).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(reset.getCode(), equalTo(CODE));
        MatcherAssert.assertThat(reset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(reset.getResetStatus(), equalTo(PENDING));
    }

    @Test
    public void shouldUpdateStatusOfExistingResetToExpiredWhenItIsExpiredAndCreateNewReset() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset1 = createReset();
        reset1.setRequestedAt(new Date(2323223232L));
        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset1);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        resetService.notifyForResetRequest(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(2)).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(reset.getCode(), not(equalTo(CODE)));
        MatcherAssert.assertThat(reset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(reset.getResetStatus(), equalTo(PENDING));
    }

    @Test
    public void shouldUpdateStatusOfAllExistingResetToExpiredAndCreateNewReset() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset reset1 = createReset();
        Reset reset2 = createReset();
        List<Reset> existingPendingResets = new ArrayList<>();
        existingPendingResets.add(reset1);
        existingPendingResets.add(reset2);

        when(resetRepository.findByEmailIgnoreCaseAndResetStatus(EMAIL, PENDING)).thenReturn(existingPendingResets);

        resetService.notifyForResetRequest(EMAIL);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository, times(1)).save(resetArgumentCaptor.capture());

        Reset reset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(reset.getCode(), not(equalTo(CODE)));
        MatcherAssert.assertThat(reset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(reset.getResetStatus(), equalTo(PENDING));
    }

    @Test
    public void shouldModifyExistingResetWhenResetSuccessFor() throws NotificationClientException {
        doNothing().when(notifyService).notify(EMAIL, CODE, TEMPLATE_ID, URL);

        Reset expectedReset = createReset();

        resetService.notifyOfSuccessfulReset(expectedReset);

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);

        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(actualReset.getCode(), equalTo(CODE));
        MatcherAssert.assertThat(actualReset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(actualReset.getResetStatus(), equalTo(RESET));
    }

    @Test
    public void isResetExpiredShouldReturnExpiredIfRequestedAtMoreThan24H() {
        Reset reset = createReset();
        reset.setRequestedAt(new Date(2323223232L));

        MatcherAssert.assertThat(resetService.isResetExpired(reset), equalTo(true));

        ArgumentCaptor<Reset> resetArgumentCaptor = ArgumentCaptor.forClass(Reset.class);
        verify(resetRepository).save(resetArgumentCaptor.capture());

        Reset actualReset = resetArgumentCaptor.getValue();
        MatcherAssert.assertThat(actualReset.getCode(), equalTo(CODE));
        MatcherAssert.assertThat(actualReset.getEmail(), equalTo(EMAIL));
        MatcherAssert.assertThat(actualReset.getResetStatus(), equalTo(EXPIRED));
    }

    private Reset createReset() {
        Reset reset = new Reset();
        reset.setCode(CODE);
        reset.setEmail(EMAIL);
        reset.setResetStatus(PENDING);
        reset.setRequestedAt(new Date());
        return reset;
    }
}
